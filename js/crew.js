// crew.js — '홈크루' 카테고리: 생성/가입/멤버관리/채팅/크루대전.

// 팀장일 때만 '크루원관리' 탭이 추가로 붙는다 (가입요청 승인·강퇴는 팀장 전용 화면으로 분리).
function getCrewPageTabs() {
  const tabs = ['크루 메인', '크루채팅', '크루원 정보'];
  if (getMyCrewRole() === '팀장') tabs.push('크루원관리');
  else tabs.push('크루탈퇴');
  return tabs;
}
// 크루를 만들 때 반드시 하나 고르는 컨셉 태그. 가입 목록 카드와 크루 내부(view-head)에
// 계속 노출해서, 이 크루가 어떤 성향인지 한눈에 알 수 있게 한다.
const CREW_CONCEPTS = ['다이어트', '크루랭킹', '친목', '근육강화', '건강유지'];
// 크루대전 카드의 "5vs5" 느낌을 내는 작은 사람 실루엣 아이콘 — currentColor라 부모의 color만
// 바꾸면 색이 따라온다(renderCrewOverview의 크루대전 카드에서 5개씩 두 줄로 씀).
const BATTLE_PERSON_ICON = `<svg viewBox="0 0 24 24" width="15" height="15" style="display:block;"><circle cx="12" cy="7" r="4" fill="currentColor"/><path d="M4 22c0-4.4 3.58-8 8-8s8 3.6 8 8" fill="currentColor"/></svg>`;
const CREW_EXP_PER_LEVEL = 2000; // 크루 레벨업에 필요한 경험치량 (레벨마다 동일하게 고정)
const CREW_CONCEPT_MAX = 3;
function normalizeCrewConcepts(value, fallback) {
  const source = Array.isArray(value) ? value : value == null ? fallback : value;
  if (Array.isArray(source)) return [...new Set(source.map(v => String(v).trim()).filter(Boolean))].slice(0, CREW_CONCEPT_MAX);
  if (typeof source !== 'string') return [];
  const text = source.trim();
  if (!text) return [];
  try {
    const parsed = JSON.parse(text);
    if (Array.isArray(parsed)) return normalizeCrewConcepts(parsed);
  } catch (err) { /* 콤마/해시태그 문자열 형식으로 계속 처리 */ }
  return [...new Set(text.split(/[,|#]/).map(v => v.trim()).filter(Boolean))].slice(0, CREW_CONCEPT_MAX);
}
function setCrewConcept(c) {
  const list = state.crew.concepts;
  const idx = list.indexOf(c);
  if (idx >= 0) list.splice(idx, 1);
  else if (list.length >= CREW_CONCEPT_MAX) { toast(`컨셉은 최대 ${CREW_CONCEPT_MAX}개까지 선택할 수 있어요`); return; }
  else list.push(c);
  render();
}
// 우리동네 크루 가입하기 목록. 검색·지역 필터·페이지네이션 데모를 위해 여러 지역에 걸쳐 구성했다.
let JOINABLE_CREWS = []; // 서버에서 실제 크루 목록을 받아와 채우는 배열 (loadJoinableCrews 참고)

async function loadJoinableCrews() {
  try {
    const res = await fetch(`${API_BASE}/api/crews/browse`, {
      headers: state.token ? { 'Authorization': 'Bearer ' + state.token } : {}
    });
    const body = await res.json();
    if (!body.success) return;
    JOINABLE_CREWS = body.data.map(c => {
      const parts = (c.region || '').split(' ').filter(Boolean);
      return {
        id: c.id, name: c.name, desc: c.description, concept: c.concept,
        concepts: normalizeCrewConcepts(c.concepts, c.concept),
        regionCity: parts[0] || '', regionGu: parts[1] || '', regionDong: parts.slice(2).join(' ') || '',
        level: c.level, score: 0, leader: null,
        currentMembers: c.currentMembers ?? c.memberCount ?? 0,
        maxMembers: c.maxMembers ?? 5,
        joinEnabled: c.joinEnabled !== false && c.recruiting !== false,
      };
    });
    // 서버가 "내가 이미 이 크루에 가입 신청을 보냈는지"를 알려주므로, 새로고침/재입장해도
    // 승인 전까지는 계속 "승인대기중"으로 보이게 여기서 진짜 값으로 덮어쓴다.
    state.crew.joinPendingIds = body.data.filter(c => c.alreadyRequested).map(c => c.id);
    refreshCrewJoinResults();
  } catch (err) {
    console.error('크루 목록 불러오기 실패', err);
  }
}

function renderCrew() {
  const i = state.subtabs.crew;
  if (!state.crew.created) {
    // 크루가 아직 없을 때는 "우리동네 크루 가입하기"를 기본 화면으로 보여주고(가입이 더 흔한
    // 시작점이라), 크루를 새로 만들고 싶은 사람만 오른쪽 위 버튼으로 생성 화면을 연다.
    const creating = i === 1;
    return `
    <div class="view-head flex-between">
      <div>
        <h1>홈크루</h1>
      </div>
      <button class="btn btn-secondary btn-sm" onclick="${(!creating && state.guestMode) ? "goto('login')" : `setSub('crew',${creating ? 0 : 1})`}">${creating ? '← 가입하기로 돌아가기' : '크루 생성'}</button>
    </div>
    ${creating ? renderCrewCreate() : renderCrewJoin()}`;
  }
  const tabs = getCrewPageTabs();
  const activeTab = tabs[i] || tabs[0];
  return `
  <div class="view-head"><h1>${state.crew.name}</h1></div>
  <div class="subtabs subtabs-compact">
    ${tabs.map((t, idx) => `<div class="tab ${i === idx ? 'active' : ''}" onclick="setSub('crew',${idx})">${t}</div>`).join('')}
  </div>
  ${activeTab === '크루 메인' ? renderCrewOverview()
      : activeTab === '크루채팅' ? renderCrewChat()
        : activeTab === '크루원 정보' ? renderCrewMembers()
          : activeTab === '크루원관리' ? renderCrewManage() : renderCrewLeave()}`;
}
function renderCrewCreate() {
  return `
  <div class="card" style="max-width:480px;margin:0 auto;">
    <p class="section-label">새 크루 만들기 (1,000P · 최대 5명)</p>
    <div class="field">
      <label>활동 지역</label>
      <div class="hint" style="padding:10px 12px;border:1px solid var(--line);border-radius:8px;background:var(--surface-2);">${state.user.region} <span style="color:var(--ink-faint);">(캘리브레이션 시 등록된 활동 지역)</span></div>
    </div>
    <div class="field"><label for="cr-name">크루 이름</label><input id="cr-name" placeholder="예: 역삼동 스쿼트단" value="${state.crew.draftName || ''}" oninput="state.crew.draftName=this.value"></div>
    <div class="field"><label for="cr-desc">크루 소개</label><textarea id="cr-desc" rows="3" placeholder="어떤 크루인지 소개해주세요" oninput="state.crew.draftDesc=this.value">${state.crew.draftDesc || ''}</textarea></div>
    <div class="field">
      <label>크루 컨셉 (최대 ${CREW_CONCEPT_MAX}개 선택, 1개 이상 필수)</label>
      <div style="display:flex;flex-wrap:wrap;gap:8px;">
        ${CREW_CONCEPTS.map(c => `<button type="button" class="btn btn-sm ${state.crew.concepts.includes(c) ? 'btn-primary' : 'btn-secondary'}" onclick="setCrewConcept('${c}')">#${c}</button>`).join('')}
      </div>
    </div>
    <button class="btn btn-primary btn-block" ${state.user.points < 1000 ? 'disabled style="opacity:.5;"' : ''} onclick="createCrew()">1,000P로 크루 생성</button>
    ${state.user.points < 1000 ? '<p class="hint" style="color:var(--danger);">포인트가 부족하여 크루를 생성할 수 없습니다.</p>' : ''}
  </div>`;
}
async function createCrew() {
  if (state.user.points < 1000) { toast('크루 생성에는 1,000포인트가 필요합니다'); return; }
  if (!state.crew.concepts.length) { toast('크루 컨셉을 하나 이상 선택해주세요'); return; }
  const name = document.getElementById('cr-name').value.trim();
  const desc = document.getElementById('cr-desc').value.trim() || '함께 성장하는 홈트 크루입니다.';
  if (!name) { toast('크루 이름을 입력해주세요'); return; }

  try {
    const res = await fetch(`${API_BASE}/api/crews`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + state.token },
      body: JSON.stringify({ name, description: desc, concepts: [...state.crew.concepts], concept: state.crew.concepts[0] })
    });
    const body = await res.json();
    if (!body.success) { toast(body.message || '크루 생성에 실패했습니다'); return; }

    await loadMyProfile(); // 포인트 100 차감된 실제 값 반영
    await loadMyCrew();
    state.subtabs.crew = 0;
    connectCrewChat(); // 방금 크루가 생겼으니 실시간 소켓을 연결한다
    toast('크루가 생성되었습니다');
    render();
  } catch (err) {
    toast('서버에 연결할 수 없습니다 (백엔드가 켜져 있는지 확인해주세요)');
  }
}

const CREW_JOIN_PAGE_SIZE = 8;
function getCrewJoinList() {
  const s = state.crew;
  const fCity = s.joinCity && REGION_DATA[s.joinCity] ? s.joinCity : null;
  const fGu = fCity && s.joinGu && REGION_DATA[fCity][s.joinGu] ? s.joinGu : null;
  const dongs = fGu ? REGION_DATA[fCity][fGu] : [];
  const fDong = fGu && s.joinDong && dongs.includes(s.joinDong) ? s.joinDong : null;
  const concepts = s.joinConcepts || [];
  return JOINABLE_CREWS.filter(c => {
    if (s.joinSearch && !c.name.includes(s.joinSearch)) return false;
    if (fCity && c.regionCity !== fCity) return false;
    if (fGu && c.regionGu !== fGu) return false;
    if (fDong && c.regionDong !== fDong) return false;
    if (concepts.length && !c.concepts.some(cc => concepts.includes(cc))) return false;
    return true;
  });
}
// 검색창 자체는 renderCrewJoin()에서 한 번만 그리고, 이후 검색어 입력은 #crew-join-results
// 안쪽만 innerHTML로 갈아끼운다 — 검색할 때마다 전체를 다시 그리면(=input을 새 DOM으로
// 교체하면) 한글 입력 중(조합 중)인 input이 통째로 교체되면서 IME 조합이 끊겨 자음/모음이
// 따로 입력되는 문제가 있었다. input 노드를 건드리지 않으면 이 문제가 근본적으로 사라진다.
function renderCrewJoin() {
  const s = state.crew;
  return `
  <div class="field" style="max-width:360px;"><label for="crew-search-input">크루명 검색</label><input id="crew-search-input" placeholder="크루 이름으로 검색" value="${s.joinSearch || ''}"
    oninput="if(!this.dataset.composing) setCrewJoinSearch(this.value)"
    oncompositionstart="this.dataset.composing='1'"
    oncompositionend="this.dataset.composing=''; setCrewJoinSearch(this.value)"></div>
  <div id="crew-join-results">${renderCrewJoinResults()}</div>`;
}
function renderCrewJoinResults() {
  const s = state.crew;
  const cities = Object.keys(REGION_DATA);
  const fCity = s.joinCity && REGION_DATA[s.joinCity] ? s.joinCity : null;
  const gus = fCity ? Object.keys(REGION_DATA[fCity]) : [];
  const fGu = fCity && s.joinGu && REGION_DATA[fCity][s.joinGu] ? s.joinGu : null;
  const dongs = fGu ? REGION_DATA[fCity][fGu] : [];
  const fDong = fGu && s.joinDong && dongs.includes(s.joinDong) ? s.joinDong : null;
  const concepts = s.joinConcepts || [];

  const list = getCrewJoinList();
  const totalPages = Math.max(1, Math.ceil(list.length / CREW_JOIN_PAGE_SIZE));
  const page = Math.min(s.joinPage || 1, totalPages);
  const pageItems = list.slice((page - 1) * CREW_JOIN_PAGE_SIZE, page * CREW_JOIN_PAGE_SIZE);

  return `
  <div class="filter-bar">
    <select onchange="setCrewJoinCity(this.value)">
      <option value="">시 전체</option>
      ${cities.map(c => `<option ${c === fCity ? 'selected' : ''}>${c}</option>`).join('')}
    </select>
    <select onchange="setCrewJoinGu(this.value)" ${fCity ? '' : 'disabled'}>
      <option value="">구 전체</option>
      ${gus.map(g => `<option ${g === fGu ? 'selected' : ''}>${g}</option>`).join('')}
    </select>
    <select onchange="setCrewJoinDong(this.value)" ${fGu ? '' : 'disabled'}>
      <option value="">동 전체</option>
      ${dongs.map(d => `<option ${d === fDong ? 'selected' : ''}>${d}</option>`).join('')}
    </select>
  </div>
  <div class="field" style="margin-top:8px;">
    <label>크루 컨셉 <span class="hint" style="margin:0;">(최대 ${CREW_CONCEPT_MAX}개 선택, 선택 안 하면 전체)</span></label>
    <div style="display:flex;flex-wrap:wrap;gap:8px;">
      <button type="button" class="btn btn-sm ${!concepts.length ? 'btn-primary' : 'btn-secondary'}" onclick="setCrewJoinConcept('')">전체</button>
      ${CREW_CONCEPTS.map(c => `<button type="button" class="btn btn-sm ${concepts.includes(c) ? 'btn-primary' : 'btn-secondary'}" onclick="setCrewJoinConcept('${c}')">#${c}</button>`).join('')}
    </div>
  </div>
  <div class="grid grid-3">
    ${pageItems.length ? pageItems.map(c => `
      <div class="card">
        <div class="flex-between"><h3 style="margin:0;">${c.name}</h3><span class="pill pill-gold">Lv.${c.level}</span></div>
        <div style="margin-top:6px;">${c.concepts.map(cc => `<span class="pill pill-accent" style="margin-right:4px;">#${cc}</span>`).join('')}</div>
        <p class="desc" style="margin-top:8px;">${c.desc}</p>
        <p class="hint" style="margin:0 0 4px;">${c.regionCity} ${c.regionGu} ${c.regionDong}</p>
        ${renderJoinButton(c)}
      </div>`).join('') : '<div class="empty-note" style="grid-column:1/-1;">조건에 맞는 크루가 없어요.</div>'}
  </div>
  ${totalPages > 1 ? `
  <div class="flex-between" style="margin-top:16px;justify-content:center;gap:14px;">
    <button class="btn btn-sm btn-ghost" ${page <= 1 ? 'disabled style="opacity:.4;cursor:not-allowed;"' : ''} onclick="setCrewJoinPage(${page - 1})">이전</button>
    <span class="hint" style="margin:0;">${page} / ${totalPages} 페이지</span>
    <button class="btn btn-sm btn-ghost" ${page >= totalPages ? 'disabled style="opacity:.4;cursor:not-allowed;"' : ''} onclick="setCrewJoinPage(${page + 1})">다음</button>
  </div>`: ''}`;
}
function refreshCrewJoinResults() {
  const el = document.getElementById('crew-join-results');
  if (el) el.innerHTML = renderCrewJoinResults();
}
function setCrewJoinSearch(v) {
  state.crew.joinSearch = v; state.crew.joinPage = 1;
  refreshCrewJoinResults();
}
function setCrewJoinCity(v) { state.crew.joinCity = v || null; state.crew.joinGu = null; state.crew.joinDong = null; state.crew.joinPage = 1; refreshCrewJoinResults(); }
function setCrewJoinGu(v) { state.crew.joinGu = v || null; state.crew.joinDong = null; state.crew.joinPage = 1; refreshCrewJoinResults(); }
function setCrewJoinDong(v) { state.crew.joinDong = v || null; state.crew.joinPage = 1; refreshCrewJoinResults(); }
function setCrewJoinConcept(c) {
  if (!c) { state.crew.joinConcepts = []; }
  else {
    const list = state.crew.joinConcepts || (state.crew.joinConcepts = []);
    const idx = list.indexOf(c);
    if (idx >= 0) list.splice(idx, 1);
    else if (list.length >= CREW_CONCEPT_MAX) { toast(`컨셉은 최대 ${CREW_CONCEPT_MAX}개까지 선택할 수 있어요`); return; }
    else list.push(c);
  }
  state.crew.joinPage = 1;
  refreshCrewJoinResults();
}
function setCrewJoinPage(p) { state.crew.joinPage = p; refreshCrewJoinResults(); }
// 가입요청 버튼 자체(renderCrewJoinResults) — 대기중인 크루는 disabled + "승인대기중"으로 표시한다.
function renderJoinButton(c) {
  if (state.guestMode) return `<button class="btn btn-primary btn-block" style="margin-top:0;" onclick="goto('login')">가입요청하기</button>`;
  const pending = (state.crew.joinPendingIds || []).includes(c.id);
  if (pending) return `<button class="btn btn-primary btn-block" style="margin-top:0;opacity:.5;cursor:not-allowed;" disabled>승인대기중</button>`;
  return `<button class="btn btn-primary btn-block" style="margin-top:0;" onclick="joinCrew(${c.id})">가입요청하기</button>`;
}
async function joinCrew(crewId) {
  const crew = JOINABLE_CREWS.find(c => Number(c.id) === Number(crewId));
  const current = Number(crew?.currentMembers ?? crew?.memberCount ?? 0);
  const max = Number(crew?.maxMembers ?? 5);
  if (!crew || crew.joinEnabled === false || crew.recruiting === false || current >= max) {
    toast('현재 가입할 수 없는 크루입니다');
    refreshCrewJoinResults();
    return;
  }
  // 서버 응답을 기다리지 않고 클릭 즉시 버튼을 잠가서 중복 신청을 막고 "승인대기중"을 보여준다.
  if (!state.crew.joinPendingIds) state.crew.joinPendingIds = [];
  state.crew.joinPendingIds.push(crewId);
  refreshCrewJoinResults();
  try {
    const res = await fetch(`${API_BASE}/api/crews/${crewId}/join-requests`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + state.token },
      body: JSON.stringify({ message: '', status: 'PENDING' })
    });
    const body = await res.json();
    if (!body.success) {
      state.crew.joinPendingIds = state.crew.joinPendingIds.filter(id => id !== crewId);
      refreshCrewJoinResults();
      toast(body.message || '가입 신청에 실패했습니다');
      return;
    }
    toast('가입 신청을 보냈습니다. 크루장의 승인을 기다려주세요.');
  } catch (err) {
    state.crew.joinPendingIds = state.crew.joinPendingIds.filter(id => id !== crewId);
    refreshCrewJoinResults();
    toast('서버에 연결할 수 없습니다 (백엔드가 켜져 있는지 확인해주세요)');
  }
}

async function loadCrewJoinRequests() {
  if (!state.token) return;
  try {
    const res = await fetch(`${API_BASE}/api/crews/me/join-requests`, {
      headers: { 'Authorization': 'Bearer ' + state.token }
    });
    const body = await res.json();
    if (!body.success) return;
    const requests = Array.isArray(body.data) ? body.data : (body.data?.content || []);
    state.crew.joinRequests = requests
      .filter(r => !r.status || r.status === 'PENDING')
      .map(r => ({
        id: r.id, userId: r.requesterId, n: r.requesterNickname, level: r.requesterLevel,
        msg: r.message || '', status: r.status || 'PENDING', requestedAt: r.requestedAt || r.createdAt || null
      }));
    render();
  } catch (err) {
    console.error('가입 요청 목록 불러오기 실패', err);
  }
}

async function approveJoinRequest(requestId) {
  try{
    const res = await fetch(`${API_BASE}/api/crews/me/join-requests/${requestId}/approve`, {
      method:'POST', headers:{ 'Authorization': 'Bearer ' + state.token }
    });
    const body = await res.json();
    if(!body.success){ toast(body.message || '승인에 실패했습니다'); return; }
    toast('가입을 승인했습니다');
    await loadMyCrew();
    await loadCrewJoinRequests();
  }catch(err){
    toast('서버에 연결할 수 없습니다');
  }
}
async function rejectJoinRequest(requestId) {
  try{
    const res = await fetch(`${API_BASE}/api/crews/me/join-requests/${requestId}/reject`, {
      method:'POST', headers:{ 'Authorization': 'Bearer ' + state.token }
    });
    const body = await res.json();
    if(!body.success){ toast(body.message || '거절에 실패했습니다'); return; }
    toast('가입 요청을 거절했습니다');
    await loadCrewJoinRequests();
  }catch(err){
    toast('서버에 연결할 수 없습니다');
  }
}



function getMyCrewRole() {
  const me = state.crew.members.find(m => Number(m.userId) === Number(state.user.id) || m.n === '나');
  return me ? me.role : '팀원';
}
// 내부 로직·비교는 계속 '팀장'/'팀원' 값을 그대로 쓰고(getMyCrewRole() === '팀장' 같은 코드가
// 여러 군데라 값 자체를 바꾸면 범위가 커진다), 화면에 보여줄 때만 이 함수로 라벨을 바꿔친다.
function crewRoleLabel(role) { return role === '팀장' ? '크루장' : '크루원'; }


function renderCrewOverview() {
  const members = state.crew.members;
  const contribTotal = members.reduce((s, m) => s + m.score, 0) || 1;
  const ranked = [...members].sort((a, b) => b.score - a.score).map((m, i) => ({ ...m, rank: i + 1, pct: Math.round(m.score / contribTotal * 100) }));
  const dongRank = getMyDongCrewRank();
  const gm = state.crew.groupMission;
  const gaugePct = Math.min(100, Math.round(gm.progress / gm.target * 100));
  const expInLevel = (state.crew.exp || 0) % CREW_EXP_PER_LEVEL;
  return `
  ${renderCrewNoticeCard()}
  <div class="grid grid-fixed-2" style="align-items:stretch;">
    <div class="card crew-level-exp-card" style="display:flex;flex-direction:column;">
      <div class="crew-level-exp-head">
        <p class="section-label">크루 레벨 · 누적 경험치</p>
        ${(() => {
          const region = state.crew.leaderRegion || state.crew.region || '';
          return region ? `<span class="crew-leader-region">${region}</span>` : '';
        })()}
      </div>
      <div class="stat-row">
        <div class="stat-box"><div class="num mono">Lv.${state.crew.level}</div><div class="lbl">크루 레벨</div></div>
        <div class="stat-box"><div class="num mono">${(state.crew.exp || 0).toLocaleString()}</div><div class="lbl">누적 경험치</div></div>
        <div class="stat-box"><div class="num mono">#${dongRank.rank}</div><div class="lbl">${dongRank.dong} 순위</div></div>
      </div>
      ${(state.crew.concepts || []).length ? `
      <div style="margin-top:14px;display:flex;flex-wrap:wrap;gap:6px;">
        ${state.crew.concepts.map(c => `<span class="pill pill-accent">#${c}</span>`).join('')}
      </div>` : ''}
      <div style="margin-top:auto;">
        <p class="hint" style="margin:10px 0 4px;">레벨업까지 <b class="mono" style="color:var(--ink);">${expInLevel.toLocaleString()} / ${CREW_EXP_PER_LEVEL.toLocaleString()}</b></p>
        <div class="progress" style="height:8px;margin:0;"><span style="width:${Math.round(expInLevel / CREW_EXP_PER_LEVEL * 100)}%"></span></div>
      </div>
    </div>
    <div class="card">
      <p class="section-label">크루 미션 누적점수</p>
      ${ranked.map(m => `
        <div class="rep-row">
          <span class="rank-num ${m.rank === 1 ? 'top' : ''}" style="min-width:24px;height:22px;">${m.rank}</span>
          <span class="user-avatar" style="width:22px;height:22px;font-size:10px;flex:none;background:${avatarColor(m.rank - 1)}">${avatarInitial(m.n)}</span>
          <span style="width:64px;">${m.n}${m.n === '나' ? ' <span class="pill pill-accent">나</span>' : ''}</span>
          <div class="bar-track"><span style="width:${m.pct}%;background:var(--accent)"></span></div>
          <span class="angle mono">${m.score.toLocaleString()}점</span>
        </div>`).join('')}
    </div>
  </div>
  <div class="card" style="margin-top:14px;">
    <p class="section-label">오늘의 크루미션</p>
    <div class="flex-between"><h3 style="margin:0;">개인운동에서 ${gm.ex} 하기</h3><span class="pill pill-accent">일일</span></div>
    <p class="desc" style="margin:8px 0 10px;">크루원이 각자 '운동' 탭에서 ${gm.ex}를 완료하면 그 기록이 크루 종합 점수에 자동으로 더해져요.</p>
    <div class="gauge">
      <span class="fill" style="width:${gaugePct}%"></span>
      <span class="gauge-label">${gm.progress.toLocaleString()} / ${gm.target.toLocaleString()}</span>
    </div>
  </div>`;
}

/* ---------- 크루채팅 ----------
   실제 서비스라면 WebSocket으로 다른 크루원의 진짜 메시지를 실시간으로 받아야 하지만,
   이 프로토타입은 혼자 쓰는 목업이라 "내가 보내면 잠시 뒤 크루원 중 한 명이 랜덤 문구로
   답장하는" 형태로 흉내낸다. 매 메시지마다 render()를 다시 부르면 스크롤 위치·입력창이
   날아가므로, updateBattleUI()와 같은 방식으로 채팅 로그 DOM에만 말풍선을 append한다. */
const CREW_CHAT_AUTO_REPLIES = ['오늘도 화이팅!', '저도 방금 시작했어요', '다들 페이스 좋으시네요 👍', '조금 이따 같이 인증해요', '오늘 미션 거의 다 채웠어요!'];
function renderCrewChat() {
  const msgs = state.crew.chat.messages;
  return `
  <div class="chat-wrap" style="max-width:860px;margin:0 auto;">
    <div class="chat-log" id="crew-chat-log">
      ${msgs.map(m => `
        <div class="bubble ${m.mine ? 'me' : 'them'}">
          ${m.mine ? '' : `<div class="who">${m.who}</div>`}${m.text}
        </div>`).join('')}
    </div>
    <div class="chat-input">
      <input id="crew-chat-input" placeholder="크루원에게 메시지 보내기" onkeydown="if(event.key==='Enter'){ event.preventDefault(); sendCrewChat(); }">
      <button class="btn btn-primary btn-sm" onclick="sendCrewChat()">전송</button>
    </div>
  </div>`;
}
function scrollCrewChatToBottom() {
  const log = document.getElementById('crew-chat-log');
  if (log) log.scrollTop = log.scrollHeight;
}
function appendChatBubble(m) {
  const log = document.getElementById('crew-chat-log');
  if (!log) return;
  const div = document.createElement('div');
  div.className = 'bubble ' + (m.mine ? 'me' : 'them');
  div.innerHTML = (m.mine ? '' : `<div class="who">${m.who}</div>`) + m.text;
  log.appendChild(div);
  scrollCrewChatToBottom();
}

let crewStompClient = null;
let crewChatSubscription = null;
let crewMemberSubscription = null;
let crewJoinRequestSubscription = null;
let crewEventsSubscription = null;

// 홈크루 메뉴에 들어와 있는 동안(어느 서브탭이든) 계속 연결되는 소켓 — 크루채팅뿐 아니라
// 강퇴 등 멤버십 변경도 /topic/crews/{id}/members로 실시간 브로드캐스트 받는다(setMenu 참고).
function connectCrewChat(){
  if(!state.token || !state.crew.id) return;
  if(crewStompClient && crewStompClient.connected) return; // 이미 연결됨
  const socket = new SockJS(`${API_BASE}/ws`);
  crewStompClient = Stomp.over(socket);
  crewStompClient.debug = null;
  crewStompClient.connect(
    { Authorization: 'Bearer ' + state.token },
    () => {
      crewChatSubscription = crewStompClient.subscribe(`/topic/crews/${state.crew.id}/chat`, (frame) => {
        const m = JSON.parse(frame.body);
        if (m.senderId === state.user.id) return; // 내가 보낸 메시지는 sendCrewChat()에서 이미 낙관적으로 표시했음
        const msg = { who: m.senderNickname, mine: false, text: m.text };
        state.crew.chat.messages.push(msg);
        appendChatBubble(msg);
      });
      crewMemberSubscription = crewStompClient.subscribe(`/topic/crews/${state.crew.id}/members`, (frame) => {
        handleCrewMemberEvent(JSON.parse(frame.body));
      });
      if(getMyCrewRole() === '팀장'){
        crewJoinRequestSubscription = crewStompClient.subscribe(`/topic/crews/${state.crew.id}/join-requests`, async () => {
          await loadCrewJoinRequests();
        });
      }
      // 크루대전 파티 초대/응답처럼 나 한 사람에게만 오는 알림은 개인 큐로 온다.
      crewEventsSubscription = crewStompClient.subscribe('/user/queue/crew-events', (frame) => {
        const ev = JSON.parse(frame.body);
        if (ev.type === 'BATTLE_PARTY_INVITE' || ev.type === 'BATTLE_PARTY_RESPONSE') {
          handleCrewPartyEvent(ev);
        } else {
          handleCrewMemberEvent(ev);
        }
      });
    },
    (err) => { console.error('채팅 연결 실패', err); }
  );
}

function disconnectCrewChat(){
  if(crewChatSubscription){ crewChatSubscription.unsubscribe(); crewChatSubscription=null; }
  if(crewMemberSubscription){ crewMemberSubscription.unsubscribe(); crewMemberSubscription=null; }
  if(crewJoinRequestSubscription){ crewJoinRequestSubscription.unsubscribe(); crewJoinRequestSubscription=null; }
  if(crewEventsSubscription){ crewEventsSubscription.unsubscribe(); crewEventsSubscription=null; }
  if(crewStompClient && crewStompClient.connected){ crewStompClient.disconnect(); }
  crewStompClient = null;
}

let joinWaitStompClient = null;
let joinWaitSubscription = null;
// 아직 크루가 없는 상태(가입 신청을 넣어두고 기다리는 중일 수 있음)로 홈크루 탭에 들어와
// 있는 동안 연결하는 개인 알림 큐. 크루장이 내 가입 신청을 승인하면 서버가
// /user/queue/crew-events로 따로 알려줘서(CrewService.approveJoinRequest 참고), 아직
// 크루의 /topic/crews/{id}/members를 구독할 수 없는 신청자 본인도 새로고침 없이 바로
// 우리 크루 화면으로 넘어갈 수 있다.
function connectJoinWait(){
  if(!state.token || state.crew.created) return;
  if(joinWaitStompClient && joinWaitStompClient.connected) return;
  const socket = new SockJS(`${API_BASE}/ws`);
  joinWaitStompClient = Stomp.over(socket);
  joinWaitStompClient.debug = null;
  joinWaitStompClient.connect(
    { Authorization: 'Bearer ' + state.token },
    () => {
      joinWaitSubscription = joinWaitStompClient.subscribe('/user/queue/crew-events', (frame) => {
        disconnectJoinWait(); // 승인되는 순간 정식 크루원이 되니, 대기용 소켓은 끊고 handleCrewMemberEvent가 crewChat 소켓을 새로 잡는다
        handleCrewMemberEvent(JSON.parse(frame.body));
      });
    },
    (err) => { console.error('가입 대기 알림 연결 실패', err); }
  );
}
function disconnectJoinWait(){
  if(joinWaitSubscription){ joinWaitSubscription.unsubscribe(); joinWaitSubscription=null; }
  if(joinWaitStompClient && joinWaitStompClient.connected){ joinWaitStompClient.disconnect(); }
  joinWaitStompClient = null;
}

// 크루장이 크루원을 강퇴하면 서버가 /topic/crews/{id}/members로 이벤트를 쏘는데, 이걸
// 강퇴한 본인·강퇴당한 사람·나머지 크루원 모두가 구독하고 있으므로 각자 알맞게 반응한다.
// 크루장이 크루원을 강퇴하거나 승인하면 서버가 /topic/crews/{id}/members로 이벤트를 쏘는데,
// 이미 그 크루 화면에 들어와 소켓이 연결된 사람들만 새로고침 없이 반응한다.
async function handleCrewMemberEvent(ev){
  if(ev.type === 'KICKED'){
    if(ev.targetUserId === state.user.id){
      toast('크루에서 강퇴되었습니다');
      disconnectCrewChat(); // 더 이상 이 크루 소속이 아니므로 소켓을 끊는다
      await loadMyCrew();
      state.subtabs.crew = 0;
      render();
    } else {
      toast(`${ev.targetNickname}님이 강퇴되었습니다`);
      await loadMyCrew();
      render();
    }
  } else if(ev.type === 'JOINED'){
    if(ev.targetUserId === state.user.id){
      // 내 가입 신청이 방금 승인된 경우 — 개인 알림 큐(connectJoinWait)로만 들어오는 경로.
      toast('가입이 승인되었습니다! 크루에 오신 것을 환영해요 🎉');
      await loadMyCrew();
      connectCrewChat(); // 방금 막 크루원이 됐으니 채팅/멤버십 소켓을 새로 연결한다
    } else {
      toast(`${ev.targetNickname}님이 크루에 합류했습니다`);
      await loadMyCrew();
    }
    render();
  }
}

// 크루대전 파티 초대/응답 개인 알림 처리. BATTLE_PARTY_INVITE는 초대받은 사람 쪽에서,
// BATTLE_PARTY_RESPONSE는 초대를 보낸 사람 쪽에서 받는다.
function handleCrewPartyEvent(ev){
  if (ev.type === 'BATTLE_PARTY_INVITE') {
    if (Number(ev.inviterUserId) === Number(state.user.id)) return; // 내가 보낸 초대의 에코는 무시
    state.crewParty.incoming = (state.crewParty.incoming || [])
      .filter(x => Number(x.inviterUserId) !== Number(ev.inviterUserId));
    state.crewParty.incoming.push({
      inviterUserId: ev.inviterUserId,
      inviterNickname: ev.inviterNickname,
      battleSize: ev.battleSize,
      timeLeft: 10
    });
    startIncomingPartyTicker();
    toast(`🔔 ${ev.inviterNickname}님이 크루대전 파티에 초대했어요`);
    render();
  } else if (ev.type === 'BATTLE_PARTY_RESPONSE') {
    const inv = (state.crewParty.invites || []).find(x => Number(x.userId) === Number(ev.responderUserId));
    if (!inv) return;
    inv.status = ev.accepted ? 'accepted' : 'rejected';
    toast(ev.accepted ? `🔔 ${ev.responderNickname}님이 파티 신청을 수락했어요` : `${ev.responderNickname}님이 파티 신청을 거절했어요`);
    updatePartyStatusModal();
    checkPartyReady();
    render();
  }
}


async function loadCrewChatHistory(){
  if(!state.token) return;
  try{
    const res = await fetch(`${API_BASE}/api/crews/me/chat`, {
      headers: { 'Authorization': 'Bearer ' + state.token }
    });
    const body = await res.json();
    if(!body.success) return;
    state.crew.chat.messages = body.data.map(m => ({
      who: m.senderNickname, mine: m.senderId===state.user.id, text: m.text
    }));
    render();
  }catch(err){
    console.error('채팅 내역 불러오기 실패', err);
  }
}


function sendCrewChat() {
  const el = document.getElementById('crew-chat-input');
  if (!el) return;
  const text = el.value.trim();
  if (!text) return;
  if(!crewStompClient || !crewStompClient.connected){ toast('채팅 연결 중입니다. 잠시 후 다시 시도해주세요.'); return; }
  // 서버 브로드캐스트가 돌아올 때까지 기다리지 않고 내 말풍선은 바로 그려서, 전송 즉시
  // 화면에 쌓이는 것처럼 보이게 한다(에코가 오면 subscribe 쪽에서 senderId로 걸러서 중복 방지).
  const msg = { who: state.user.nickname, mine: true, text };
  state.crew.chat.messages.push(msg);
  appendChatBubble(msg);
  crewStompClient.send(`/app/crews/${state.crew.id}/chat`, {}, JSON.stringify({ text }));
  el.value = '';
}


/* ---------- 5vs5 크루대전 파티맺기 ----------
   크루장·크루원 모두 대전을 시작하려면 먼저 "파티"를 맺어야 한다. 데려갈 크루원을
   체크박스로 골라 신청하면(선택은 필수 아님 — 아무도 안 고르면 바로 파티 완료 처리)
   각 크루원에게 초대가 가고, 10초 안에 수락해야 파티에 합류한다. 실제 서비스라면
   상대방이 진짜 알림을 받고 직접 수락 버튼을 눌러야 하지만, 이 프로토타입은 혼자 쓰는
   목업이라 2~8초 사이 랜덤 시점에 자동으로 수락한 것처럼 흉내낸다. 초대 전원이
   수락/시간초과로 결론나면 "대전 시작" 버튼이 눌리게 활성화된다. */
function openPartyInvite() {
  if (state.crewBattle) return;
  state.crewParty.open = true;
  state.crewParty.selected = [];
  state.crewParty.battleSize = state.crewParty.battleSize || 5;
  render();
}
function closePartyInvite() { state.crewParty.open = false; render(); }
function setBattleSize(size) {
  const p = state.crewParty;
  p.battleSize = Number(size);
  const maxOthers = p.battleSize - 1;
  if (p.selected.length > maxOthers) p.selected = p.selected.slice(0, maxOthers);
  render();
}
function togglePartyPick(name) {
  const p = state.crewParty;
  const sel = p.selected;
  const idx = sel.indexOf(name);
  if (idx >= 0) sel.splice(idx, 1);
  else {
    if (sel.length >= (p.battleSize || 5) - 1) {
      toast(`파티원은 본인 포함 ${p.battleSize || 5}명까지만 선택할 수 있어요`);
      return;
    }
    sel.push(name);
  }
  render();
}
function sendPartyInvites() {
  const p = state.crewParty;
  const picked = [...p.selected];
  const requiredOthers = (p.battleSize || 5) - 1;
  if (picked.length !== requiredOthers) {
    toast(`${p.battleSize || 5}vs${p.battleSize || 5}는 본인 포함 ${p.battleSize || 5}명을 선택해야 해요`);
    return;
  }
  p.open = false;
  if (!picked.length) {
    state.crewParty.invites = [];
    state.crewParty.ready = true;
    // 메인 화면의 "대전 시작" 버튼을 없앴으므로, 파티원 없이 바로 시작하는 경우에도 시작
    // 버튼이 있는 현황 팝업을 띄워야 실제로 대전을 시작할 방법이 생긴다.
    state.crewParty.statusOpen = true;
    toast('파티원 없이 바로 대전을 시작할 수 있어요');
    render();
    return;
  }
  const pickedMembers = picked
    .map(n => state.crew.members.find(m => m.n === n))
    .filter(Boolean);
  const doSendPartyInvites = () => {
    state.crewParty.invites = pickedMembers.map(m => ({ userId: m.userId, n: m.n, status: 'pending', timeLeft: 10 }));
    state.crewParty.ready = false;
    state.crewParty.statusOpen = true;
    crewStompClient.send(`/app/crews/${state.crew.id}/party-invite`, {}, JSON.stringify({
      battleSize: p.battleSize || 5,
      inviteeUserIds: pickedMembers.map(m => m.userId)
    }));
    toast('파티 신청을 보냈어요 · 상대방이 10초 안에 수락하지 않으면 자동으로 만료돼요');
    startPartyTicker();
    render();
  };
  if (crewStompClient && crewStompClient.connected) {
    doSendPartyInvites();
    return;
  }
  // 홈크루 탭에 막 들어온 직후라 소켓 연결이 아직 안 끝났을 수 있다 — 실패로 끝내지 않고
  // 연결될 때까지 잠깐 기다렸다가 되는 대로 자동으로 보낸다(최대 8초).
  toast('크루 연결 중입니다 · 연결되는 대로 자동으로 신청을 보낼게요');
  connectCrewChat();
  let waited = 0;
  const waitId = setInterval(() => {
    waited += 300;
    if (crewStompClient && crewStompClient.connected) {
      clearInterval(waitId);
      doSendPartyInvites();
    } else if (waited >= 8000) {
      clearInterval(waitId);
      toast('크루 연결에 실패했어요. 다시 시도해주세요.');
    }
  }, 300);
}
function pendingPartyInviteCount() {
  return (state.crewParty.incoming || []).length;
}
function toggleNotifPanel() { state.notifPanelOpen = !state.notifPanelOpen; render(); }
function closeNotifPanel() { state.notifPanelOpen = false; render(); }
function renderNotifPanel() {
  const pending = state.crewParty.incoming || [];
  return `
  <div class="notif-panel">
    <h4>크루대전 파티 신청</h4>
    ${pending.length ? pending.map(inv => `
      <div class="notif-row">
        <span>${inv.inviterNickname}님이 ${inv.battleSize}vs${inv.battleSize} 파티에 초대했어요 (${inv.timeLeft}초)</span>
      </div>
      <div class="notif-row" style="border-top:none;padding-top:0;gap:8px;">
        <button class="btn btn-sm btn-secondary" style="flex:1;" onclick="rejectPartyInvite(${inv.inviterUserId})">거절</button>
        <button class="btn btn-sm btn-primary" style="flex:1;" onclick="acceptPartyInvite(${inv.inviterUserId})">수락</button>
      </div>`).join('') : '<p class="hint" style="margin:0;">새 알림이 없어요.</p>'}
  </div>`;
}
function respondToPartyInvite(inviterUserId, accepted) {
  const list = state.crewParty.incoming || [];
  const idx = list.findIndex(x => Number(x.inviterUserId) === Number(inviterUserId));
  if (idx < 0) return;
  const inv = list[idx];
  list.splice(idx, 1);
  if (crewStompClient && crewStompClient.connected) {
    crewStompClient.send(`/app/crews/${state.crew.id}/party-invite/respond`, {}, JSON.stringify({
      inviterUserId: inv.inviterUserId,
      accepted
    }));
  }
  toast(accepted ? `${inv.inviterNickname}님의 파티 신청을 수락했어요` : `${inv.inviterNickname}님의 파티 신청을 거절했어요`);
  render();
}
function acceptPartyInvite(inviterUserId) { respondToPartyInvite(inviterUserId, true); }
function rejectPartyInvite(inviterUserId) { respondToPartyInvite(inviterUserId, false); }
function startIncomingPartyTicker() {
  clearInterval(state.crewParty.incomingTickId);
  state.crewParty.incomingTickId = setInterval(() => {
    const list = state.crewParty.incoming || [];
    if (!list.length) { clearInterval(state.crewParty.incomingTickId); return; }
    let changed = false;
    for (let i = list.length - 1; i >= 0; i--) {
      list[i].timeLeft = Math.max(0, list[i].timeLeft - 1);
      if (list[i].timeLeft === 0) { list.splice(i, 1); changed = true; }
    }
    if (changed || state.notifPanelOpen) render();
  }, 1000);
}
// 실시간 카운트다운·수락 상태는 render()를 다시 타지 않고 상태창 DOM만 직접 패치한다
// (크루채팅과 마찬가지로, 매초 전체를 다시 그리면 다른 화면에서 입력 중이던 값이 날아간다).
function updatePartyStatusModal() {
  const p = state.crewParty;
  if (!p.invites) return;
  p.invites.forEach((inv, i) => {
    const el = document.querySelector(`#party-inv-${i} .party-inv-status`);
    if (el) el.textContent = inv.status === 'accepted' ? '✅ 수락' : inv.status === 'rejected' ? '❌ 거절' : inv.status === 'expired' ? '⏱ 시간초과' : `대기중 (${inv.timeLeft}초)`;
  });
  const summary = document.getElementById('party-status-summary');
  if (summary) summary.textContent = `${p.invites.filter(x => x.status === 'accepted').length}/${p.invites.length}명 수락`;
  const startBtn = document.getElementById('party-status-start-btn');
  if (startBtn) startBtn.style.display = p.ready ? 'inline-flex' : 'none';
}
function startPartyTicker() {
  clearInterval(state.crewParty.tickId);
  state.crewParty.tickId = setInterval(() => {
    const p = state.crewParty;
    if (!p.invites) { clearInterval(p.tickId); return; }
    let expired = false;
    p.invites.forEach(inv => {
      if (inv.status === 'pending') {
        inv.timeLeft = Math.max(0, inv.timeLeft - 1);
        if (inv.timeLeft === 0) { inv.status = 'expired'; expired = true; }
      }
    });
    updatePartyStatusModal();
    if (expired) checkPartyReady();
  }, 1000);
}
function checkPartyReady() {
  const p = state.crewParty;
  if (!p.invites || !p.invites.every(x => x.status !== 'pending')) return;
  if (p.ready) return;
  clearInterval(p.tickId);
  // 아무도 수락하지 않았으면(전원 거절/시간초과) 대전을 시작할 수 없다 — "대전 시작하기"
  // 버튼을 계속 숨겨두고, 다시 초대하도록 안내만 한다.
  const anyAccepted = p.invites.some(x => x.status === 'accepted');
  if (!anyAccepted) {
    toast('아무도 파티 신청을 수락하지 않았어요. 다시 초대해주세요.');
    updatePartyStatusModal();
    return;
  }
  p.ready = true;
  toast('파티가 완성됐어요! 이제 대전을 시작할 수 있어요');
  if (state.screen === 'app' && state.menu === 'crew') render();
  else updatePartyStatusModal();
}
function openPartyStatus() { state.crewParty.statusOpen = true; render(); }
function closePartyStatus() { state.crewParty.statusOpen = false; render(); }
function renderPartyInviteModal() {
  const p = state.crewParty;
  const others = state.crew.members.filter(m => Number(m.userId) !== Number(state.user.id));
  const sel = p.selected;
  const size = Number(p.battleSize || 5);
  const requiredOthers = size - 1;
  const readyToSubmit = sel.length === requiredOthers;
  return `
  <div class="confirm-backdrop" onclick="if(event.target===this) closePartyInvite()">
    <div class="confirm-box" style="max-width:420px;">
      <h3 style="margin:0 0 4px;">크루대전 파티원 선택</h3>
      <p class="hint" style="margin:0 0 12px;">대전 인원수를 먼저 선택한 뒤, 본인을 제외한 파티원을 골라주세요.</p>
      <div class="field" style="margin-bottom:14px;">
        <label>대전 방식</label>
        <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:6px;">
          ${[2,3,4,5].map(n => `<button type="button" class="btn btn-sm ${size === n ? 'btn-primary' : 'btn-secondary'}" onclick="setBattleSize(${n})">${n}vs${n}</button>`).join('')}
        </div>
      </div>
      <p class="hint" style="margin:0 0 8px;">현재 선택: <b class="mono">${sel.length} / ${requiredOthers}</b>명 (본인 포함 ${size}명)</p>
      <div style="display:flex;flex-direction:column;gap:8px;max-height:260px;overflow-y:auto;">
        ${others.length ? others.map(m => {
          const checked = sel.includes(m.n);
          const disabled = !checked && sel.length >= requiredOthers;
          return `
          <label style="display:flex;align-items:center;gap:8px;padding:8px 10px;border:1px solid var(--line);border-radius:8px;cursor:${disabled ? 'not-allowed' : 'pointer'};opacity:${disabled ? '.45' : '1'};">
            <input type="checkbox" ${checked ? 'checked' : ''} ${disabled ? 'disabled' : ''} onchange="togglePartyPick('${m.n}')">
            <span style="flex:1;">${m.n}</span><span class="pill ${m.role === '팀장' ? 'pill-gold' : 'pill-muted'}">${crewRoleLabel(m.role)}</span>
          </label>`;
        }).join('') : '<p class="hint" style="margin:0;">선택할 다른 크루원이 없어요.</p>'}
      </div>
      <p class="hint" style="margin:10px 0 0;color:${readyToSubmit ? 'var(--accent)' : 'var(--danger)'};">
        ${readyToSubmit ? '인원 구성이 맞습니다. 매칭 신청을 보낼 수 있어요.' : `본인 포함 ${size}명이 되도록 ${requiredOthers}명의 파티원을 선택해주세요.`}
      </p>
      <div class="confirm-actions" style="margin-top:14px;">
        <button class="btn btn-ghost btn-sm" onclick="closePartyInvite()">취소</button>
        <button class="btn btn-primary btn-sm" ${readyToSubmit ? '' : 'disabled style="opacity:.45;cursor:not-allowed;"'} onclick="sendPartyInvites()">매칭 신청</button>
      </div>
    </div>
  </div>`;
}
function renderPartyStatusModal() {
  const p = state.crewParty;
  return `
  <div class="confirm-backdrop" onclick="if(event.target===this) closePartyStatus()">
    <div class="confirm-box" style="max-width:340px;">
      <h3 style="margin:0 0 4px;">대전 파티 현황</h3>
      <p class="hint" id="party-status-summary" style="margin:0 0 12px;">${p.invites ? `${p.invites.filter(x => x.status === 'accepted').length}/${p.invites.length}명 수락` : '파티원 없이 진행'}</p>
      <div style="display:flex;flex-direction:column;gap:8px;">
        ${(p.invites || []).map((inv, i) => `
          <div id="party-inv-${i}" class="flex-between" style="padding:8px 10px;border:1px solid var(--line);border-radius:8px;">
            <span>${inv.n}</span>
            <span class="party-inv-status hint" style="margin:0;">${inv.status === 'accepted' ? '✅ 수락' : inv.status === 'expired' ? '⏱ 시간초과' : `대기중 (${inv.timeLeft}초)`}</span>
          </div>`).join('')}
      </div>
      <div class="confirm-actions" style="margin-top:14px;">
        <button class="btn btn-ghost btn-sm" onclick="closePartyStatus()">닫기</button>
        <button id="party-status-start-btn" class="btn btn-primary btn-sm" style="display:${p.ready ? 'inline-flex' : 'none'};" onclick="closePartyStatus(); startCrewBattle();">대전 시작하기</button>
      </div>
    </div>
  </div>`;
}

/* ========================================================================
   5vs5 크루대전
   ------------------------------------------------------------------------
   비슷한 레벨의 상대 크루와 "먼저 스쿼트 N개 채우기" 실시간 대결. 내 개수는 실제
   스마트폰 카메라·MediaPipe 판정(exRegisterRep)에서 그대로 받아오고, 나머지 4명의 크루원과
   상대팀은 프로토타입이라 일정 주기로 자동 증가시켜 흉내낸다(실제로는 각자의 서버
   집계가 필요한 지점). 실시간 갱신은 render()를 다시 타지 않고 updateBattleUI()가
   DOM을 직접 패치한다 — 이유는 render() 훅 주석과 exRegisterRep() 참고.
   ======================================================================== */
const BATTLE_FILLER_NAMES = ['헬린이', '스쿼트왕', '런닝러버', '플랭크신', '다이어터'];
// 정확도 등급별 점수 — 크루대전은 반복 횟수가 아니라 이 점수 합산으로 승패를 가린다.
// GOOD은 따로 언급되지 않아 GREAT과 동일하게 취급한다(둘 다 "유효한 반복"이라는 의미로).
const BATTLE_GRADE_POINTS = { PERFECT: 2, GREAT: 1, GOOD: 1, MISS: 0 };
// 팀원·상대팀은 실제 판정이 없으니, 매 틱마다 이 분포에서 등급을 하나 뽑아 점수를 흉내낸다.
const BATTLE_TICK_GRADES = ['PERFECT', 'PERFECT', 'GREAT', 'GREAT', 'GREAT', 'GOOD', 'GOOD', 'MISS'];
function randomBattleGrade() { return BATTLE_TICK_GRADES[Math.floor(Math.random() * BATTLE_TICK_GRADES.length)]; }
function startCrewBattle() {
  if (!state.crewParty.ready) {
    toast('먼저 크루대전파티를 맺어야 대전을 시작할 수 있어요');
    openPartyInvite();
    return;
  }
  const myLevel = state.crew.level || 1;
  const battleSize = Number(state.crewParty.battleSize || 5);
  const candidates = JOINABLE_CREWS.filter(c => c.name !== state.crew.name && Math.abs(c.level - myLevel) <= 2);
  const pool = candidates.length ? candidates : JOINABLE_CREWS.filter(c => c.name !== state.crew.name);
  // 크루 찾기 화면을 거치지 않고 바로 크루대전을 시작하면 JOINABLE_CREWS가 아직 비어있을 수
  // 있다 — 그 경우 대비용 가짜 상대팀으로 대체해서 죽지 않게 한다.
  const opponent = pool[Math.floor(Math.random() * pool.length)] || JOINABLE_CREWS[0] || { name: '랜덤크루', level: myLevel, leader: null };

  // 파티에 합류(수락)한 크루원을 우선으로 데려가고, 남는 자리는 나머지 크루원 → 필러로 채운다.
  const partyMates = (state.crewParty.invites || []).filter(x => x.status === 'accepted').map(x => x.n);
  const restMates = state.crew.members.filter(m => m.n !== '나').map(m => m.n).filter(n => !partyMates.includes(n));
  const realMates = [...partyMates, ...restMates];
  clearInterval(state.crewParty.tickId);
  state.crewParty = { open: false, statusOpen: false, selected: [], invites: null, ready: false, tickId: null, battleSize };
  const teammates = [];
  for (let i = 0; i < battleSize - 1; i++) {
    teammates.push({ n: realMates[i] || BATTLE_FILLER_NAMES[i % BATTLE_FILLER_NAMES.length], score: 0, dur: (1.6 + Math.random() * 0.9).toFixed(2), gender: i % 2 === 0 ? 'male' : 'female', gradeCounts: { PERFECT: 0, GREAT: 0, GOOD: 0, MISS: 0 } });
  }
  // 상대팀도 5명(리더 1 + 필러 4) 개인별 점수를 따로 굴려야 결과 팝업에서 "누가 MVP인지"를
  // 보여줄 수 있다 — 예전엔 oppScore 합계만 있었다.
  const oppNamePool = [opponent.leader, ...BATTLE_FILLER_NAMES];
  const oppTeammates = [];
  for (let i = 0; i < battleSize; i++) {
    oppTeammates.push({ n: oppNamePool[i] || `상대팀원${i + 1}`, score: 0, gender: i % 2 === 0 ? 'female' : 'male', gradeCounts: { PERFECT: 0, GREAT: 0, GOOD: 0, MISS: 0 } });
  }

  state.crewBattle = {
    target: randInt(40, 60), // 점수 목표 (PERFECT=2 / GREAT·GOOD=1 / MISS=0점 합산) — 테스트 편의상 낮춰둠
    opponent: { name: opponent.name, level: opponent.level },
    size: battleSize,
    myScore: 0, oppScore: 0,
    myGradeCounts: { PERFECT: 0, GREAT: 0, GOOD: 0, MISS: 0 }, // 결과 팝업에서 "나"의 개인 판정 비율용
    teammates, oppTeammates,
    tickId: null,
    result: null, // null | 'win' | 'lose' | 'draw'
    startedAt: null, timeLimitSeconds: 120,
  };
  state.exercise = { step: 0, picked: 'squat', camPhase: 'idle', camStream: null, timerId: null, seconds: 0, result: null, retakesUsed: 0, liveReps: [], replayOpen: false };
  exBattleCountdownStarted = false; // 새 대전마다 공용 카운트다운을 다시 탈 수 있게 초기화
  disconnectCrewChat(); // 홈크루 메뉴를 벗어나므로 채팅·크루원 실시간 소켓도 함께 끊는다
  state.menu = 'crewBattle';
  // 상대팀·팀원 점수도 나와 똑같이 공용 카운트다운(startBattleReadyCountdown, exercise.js)이
  // START가 되는 순간부터 오르기 시작한다 — 누구는 먼저 시작하고 누구는 늦게 시작하는 일이
  // 없게, 모든 팀의 측정 시작 시점을 하나로 맞춘다.

  if (!state.user.calibration) {
    toast('크루대전을 시작하려면 체형 캘리브레이션이 먼저 필요해요');
    openCalibrationModal();
    return;
  }
  render();
}
function startBattleTicker() {
  clearInterval(state.crewBattle.tickId);
  state.crewBattle.startedAt = Date.now();
  state.crewBattle.tickId = setInterval(() => {
    const b = state.crewBattle;
    if (!b || b.result) return;
    const idx = Math.floor(Math.random() * b.teammates.length);
    const g1 = randomBattleGrade();
    const pts1 = BATTLE_GRADE_POINTS[g1];
    b.teammates[idx].score += pts1;
    b.teammates[idx].gradeCounts[g1] = (b.teammates[idx].gradeCounts[g1] || 0) + 1;
    updateBattleUI('mate-' + idx, pts1);
    if (Math.random() < 0.9) {
      const oidx = Math.floor(Math.random() * b.oppTeammates.length);
      const g2 = randomBattleGrade();
      const pts2 = BATTLE_GRADE_POINTS[g2];
      b.oppTeammates[oidx].score += pts2;
      b.oppScore += pts2;
      b.oppTeammates[oidx].gradeCounts[g2] = (b.oppTeammates[oidx].gradeCounts[g2] || 0) + 1;
      updateBattleUI('opp', pts2);
    }
    if ((Date.now() - b.startedAt) >= b.timeLimitSeconds * 1000) {
      const teamTotal=b.myScore+b.teammates.reduce((s,t)=>s+t.score,0);
      b.result=teamTotal===b.oppScore?'draw':teamTotal>b.oppScore?'win':'lose';
      finishBattle(); return;
    }
    checkBattleEnd();
  }, 1400);
}
// 실시간 구간 전용 DOM 패치 — render()를 부르지 않는 이유는 exRegisterRep()의 주석 참고.
function updateBattleUI(bumpedKey, delta) {
  const b = state.crewBattle;
  if (!b) return;
  const teamTotal = b.myScore + b.teammates.reduce((s, t) => s + t.score, 0);
  const setText = (id, txt) => { const el = document.getElementById(id); if (el) el.textContent = txt; };
  setText('battle-team-total', teamTotal.toLocaleString());
  setText('battle-opp-total', b.oppScore.toLocaleString());
  const bar = document.getElementById('battle-progress'); if (bar) bar.style.width = Math.min(100, teamTotal / b.target * 100) + '%';
  // 스마트폰 카메라 위에 떠 있는 실시간 스코어 오버레이도 같이 갱신한다.
  setText('cam-battle-my', teamTotal.toLocaleString());
  setText('cam-battle-opp', b.oppScore.toLocaleString());
  const miniBar = document.getElementById('cam-battle-gauge'); if (miniBar) miniBar.style.width = Math.min(100, teamTotal / b.target * 100) + '%';
  setText('battle-my-score', b.myScore);
  b.teammates.forEach((t, i) => setText('battle-mate-score-' + i, t.score));
  if (bumpedKey) popBattleFx(bumpedKey, delta);
}
// MISS(0점)일 땐 "+0"이 뜨는 게 어색하니 실제로 점수가 오를 때만 팝업을 띄운다.
function popBattleFx(key, delta) {
  if (!delta) return;
  const host = document.getElementById('battle-pop-' + key);
  if (!host) return;
  const el = document.createElement('span');
  el.className = 'battle-pop';
  el.textContent = '+' + delta;
  host.appendChild(el);
  setTimeout(() => el.remove(), 900);
}
function checkBattleEnd() {
  const b = state.crewBattle;
  if (!b || b.result) return;
  const teamTotal = b.myScore + b.teammates.reduce((s, t) => s + t.score, 0);
  if (teamTotal >= b.target) { b.result = 'win'; finishBattle(); }
  else if (b.oppScore >= b.target) { b.result = 'lose'; finishBattle(); }
}
// 대전이 실제로 끝나는 시점(카메라를 먼저 정리한 뒤)에만 render()를 부른다 — 이때는 더 이상
// 살아있는 포즈 인식 루프가 없으므로 화면을 통째로 다시 그려도 안전하다.
function finishBattle() {
  clearInterval(state.crewBattle.tickId);
  if (state.exercise.camStream) { state.exercise.camStream.getTracks().forEach(t => t.stop()); state.exercise.camStream = null; }
  clearInterval(state.exercise.timerId);
  const b=state.crewBattle;
  const ourScore=b.myScore+b.teammates.reduce((s,t)=>s+t.score,0);
  const exp=b.result==='win'?100:50;
  state.crew.exp=(state.crew.exp||0)+exp;
  const ourMembers = battleMyRoster(b).map(p => ({ n:p.n, gender:p.gender || 'male', score:p.score, gradeCounts:{...(p.gradeCounts || {PERFECT:0,GREAT:0,GOOD:0,MISS:0})} }));
  const oppMembers = battleOppRoster(b).map(p => ({ n:p.n, gender:p.gender || 'male', score:p.score, gradeCounts:{...(p.gradeCounts || {PERFECT:0,GREAT:0,GOOD:0,MISS:0})} }));
  state.crew.battleHistory.unshift({
    at:new Date().toLocaleString('ko-KR'),
    size:b.size || 5,
    ourCrewName:state.crew.name || '우리 크루',
    opponentCrewName:b.opponent.name,
    opponent:b.opponent.name,
    ourScore,
    oppScore:b.oppScore,
    result:b.result==='win'?'승리':b.result==='lose'?'패배':'무승부',
    exp,
    ourMembers,
    oppMembers
  });
  toast(b.result==='win'?`🎉 크루대전 승리! 크루 경험치 +${exp}`:b.result==='draw'?`무승부 · 크루 경험치 +${exp}`:`패배 · 크루 경험치 +${exp}`);
  render();
}
function exitCrewBattle() {
  if (state.crewBattle) clearInterval(state.crewBattle.tickId);
  if (state.exercise.camStream) { state.exercise.camStream.getTracks().forEach(t => t.stop()); }
  clearInterval(state.exercise.timerId);
  state.crewBattle = null;
  state.exercise = { step: 0, picked: null, camPhase: 'idle', camStream: null, timerId: null, seconds: 0, result: null, retakesUsed: 0, liveReps: [], replayOpen: false };
  state.menu = 'crew';
  state.subtabs.crew = 0;
  connectCrewChat(); // 다시 홈크루 메뉴로 돌아왔으니 실시간 소켓을 재연결한다
  render();
}
function drawBattleTeammates() {
  if (!state.crewBattle) return;
  state.crewBattle.teammates.forEach((t, i) => {
    const c = document.getElementById('battle-char-' + i);
    if (c) drawPixelCharacter(c, {}, t.gender || (i % 2 === 0 ? 'male' : 'female'));
  });
}
// 결과 팝업의 "참여인원" 명단 — 나+팀원, 상대팀을 각각 점수 많은 순으로 정렬한다. teammates/
// oppTeammates 원본 객체를 그대로 펼쳐 쓰므로 각자의 gradeCounts(개인 판정 카운트)도 함께 딸려온다.
function battleMyRoster(b) {
  return [{ n: '나', score: b.myScore, gender: state.user.gender || 'male', gradeCounts: b.myGradeCounts }, ...b.teammates]
    .sort((a, c) => c.score - a.score);
}
function battleOppRoster(b) {
  return [...b.oppTeammates].sort((a, c) => c.score - a.score);
}
// MVP(1등)는 폰트를 헤딩용 서체(Jua)로 바꾸고 배지를 붙여서 나머지와 구분한다. 캐릭터 그림
// 대신 닉네임과 그 사람 본인의 판정 비율(PERFECT/GREAT/MISS)을 보여준다.
function renderBattleRoster(list) {
  return `
  <div style="display:flex;flex-direction:column;gap:8px;">
    ${list.map((p, i) => {
    const gc = p.gradeCounts || { PERFECT: 0, GREAT: 0, GOOD: 0, MISS: 0 };
    const gcTotal = gc.PERFECT + gc.GREAT + gc.GOOD + gc.MISS;
    const pct = n => gcTotal ? Math.round(n / gcTotal * 100) : 0;
    return `
      <div style="padding:8px 10px;border:1.5px solid ${i === 0 ? 'var(--gold)' : 'var(--line)'};border-radius:10px;${i === 0 ? 'background:var(--surface-2);' : ''}">
        <div class="flex-between">
          <span style="${i === 0 ? 'font-family:var(--font-display);font-size:15px;' : 'font-size:13px;font-weight:700;'}white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${p.n}${i === 0 ? ' <span class="pill pill-gold" style="margin-left:2px;">MVP</span>' : ''}</span>
          <span class="mono" style="font-size:13px;color:var(--ink-dim);flex:none;">${p.score}점</span>
        </div>
        <p class="hint mono" style="margin:4px 0 0;">PERFECT <b style="color:var(--accent)">${pct(gc.PERFECT)}%</b> · GREAT <b style="color:var(--gold)">${pct(gc.GREAT + gc.GOOD)}%</b> · MISS <b style="color:var(--danger)">${pct(gc.MISS)}%</b></p>
      </div>`;
  }).join('')}
  </div>`;
}

function battleGradeTotal(gc) {
  return Object.values(gc || {}).reduce((sum, n) => sum + (Number(n) || 0), 0);
}
function battleGoodPlus(gc) {
  gc = gc || {};
  return (Number(gc.PERFECT) || 0) + (Number(gc.GREAT) || 0) + (Number(gc.GOOD) || 0);
}
function sampleBattleHistory() {
  return [
    {
      sample:true, at:'2026.09.16 19:40', size:3, opponent:'헬스메이트', ourScore:86, oppScore:78, result:'승리', exp:100,
      ourMembers:[
        {n:'나',gender:'male',score:34,gradeCounts:{PERFECT:10,GREAT:7,GOOD:5,MISS:2}},
        {n:'배드민턴킹',gender:'female',score:29,gradeCounts:{PERFECT:8,GREAT:6,GOOD:6,MISS:1}},
        {n:'헬린이탈출',gender:'male',score:23,gradeCounts:{PERFECT:5,GREAT:7,GOOD:4,MISS:3}}
      ],
      oppMembers:[
        {n:'헬스메이트장',gender:'female',score:31,gradeCounts:{PERFECT:9,GREAT:5,GOOD:5,MISS:4}},
        {n:'근육강화맨',gender:'male',score:25,gradeCounts:{PERFECT:6,GREAT:6,GOOD:5,MISS:3}},
        {n:'런닝러버',gender:'female',score:22,gradeCounts:{PERFECT:5,GREAT:5,GOOD:5,MISS:5}}
      ]
    },
    {
      sample:true, at:'2026.09.14 20:10', size:2, opponent:'스쿼트연합', ourScore:55, oppScore:61, result:'패배', exp:50,
      ourMembers:[
        {n:'나',gender:'male',score:30,gradeCounts:{PERFECT:8,GREAT:6,GOOD:4,MISS:3}},
        {n:'써니핏',gender:'female',score:25,gradeCounts:{PERFECT:6,GREAT:5,GOOD:5,MISS:2}}
      ],
      oppMembers:[
        {n:'스쿼트장인',gender:'male',score:34,gradeCounts:{PERFECT:10,GREAT:6,GOOD:4,MISS:2}},
        {n:'핏걸',gender:'female',score:27,gradeCounts:{PERFECT:7,GREAT:5,GOOD:4,MISS:4}}
      ]
    },
    {
      sample:true, at:'2026.09.12 18:25', size:4, opponent:'건강루틴', ourScore:74, oppScore:74, result:'무승부', exp:50,
      ourMembers:[
        {n:'나',gender:'male',score:21,gradeCounts:{PERFECT:6,GREAT:4,GOOD:3,MISS:2}},
        {n:'플랭크신',gender:'female',score:19,gradeCounts:{PERFECT:5,GREAT:4,GOOD:2,MISS:3}},
        {n:'다이어터',gender:'male',score:18,gradeCounts:{PERFECT:4,GREAT:4,GOOD:2,MISS:2}},
        {n:'스쿼트왕',gender:'female',score:16,gradeCounts:{PERFECT:3,GREAT:4,GOOD:3,MISS:2}}
      ],
      oppMembers:[
        {n:'건강리더',gender:'female',score:22,gradeCounts:{PERFECT:6,GREAT:4,GOOD:4,MISS:2}},
        {n:'루틴러',gender:'male',score:20,gradeCounts:{PERFECT:5,GREAT:4,GOOD:3,MISS:2}},
        {n:'홈트왕',gender:'female',score:17,gradeCounts:{PERFECT:4,GREAT:3,GOOD:3,MISS:3}},
        {n:'운동초보',gender:'male',score:15,gradeCounts:{PERFECT:3,GREAT:3,GOOD:3,MISS:3}}
      ]
    }
  ];
}
function battleHistoryData() {
  return Array.isArray(state.crew.battleHistory) && state.crew.battleHistory.length
    ? state.crew.battleHistory : sampleBattleHistory();
}
function toggleBattleHistory(idx) {
  state.crew.battleHistoryOpen = state.crew.battleHistoryOpen === idx ? null : idx;
  render();
}
function renderBattleMemberDetail(p) {
  const gc = p.gradeCounts || {PERFECT:0,GREAT:0,GOOD:0,MISS:0};
  const total = battleGradeTotal(gc);
  const good = battleGoodPlus(gc);
  const characterId = `battle-result-char-${String(p.n).replace(/[^a-zA-Z0-9가-힣]/g,'')}-${Math.random().toString(36).slice(2,7)}`;
  return `
    <div class="battle-member-detail">
      <div class="battle-member-character">
        <canvas data-battle-character="${characterId}" data-gender="${p.gender || 'male'}" width="90" height="110" style="width:58px;height:72px;image-rendering:pixelated;"></canvas>
      </div>
      <div style="min-width:0;flex:1;">
        <div class="flex-between" style="gap:8px;">
          <b style="white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${p.n}</b>
          <span class="mono">${Number(p.score) || 0}점</span>
        </div>
        <div class="battle-grade-counts">
          <span>PERFECT <b>${gc.PERFECT || 0}</b></span>
          <span>GREAT <b>${gc.GREAT || 0}</b></span>
          <span>GOOD <b>${gc.GOOD || 0}</b></span>
          <span>MISS <b>${gc.MISS || 0}</b></span>
        </div>
        <div class="hint" style="margin:5px 0 0;">GOOD 이상 <b class="mono">${good} / ${total}</b>회</div>
      </div>
    </div>`;
}
function renderBattleSide(title, list) {
  return `
    <div>
      <p class="section-label" style="margin:0 0 8px;">${title}</p>
      <div class="battle-member-list">${list.map(renderBattleMemberDetail).join('')}</div>
    </div>`;
}
function renderBattleHistoryCard(item, idx) {
  const open = state.crew.battleHistoryOpen === idx;
  const ourCrewName = item.ourCrewName || state.crew.name || '우리 크루';
  const opponentCrewName = item.opponentCrewName || item.opponent || '상대 크루';
  const size = item.size || Math.max(item.ourMembers?.length || 0, item.oppMembers?.length || 0) || 2;
  return `
  <div class="battle-history-card ${open ? 'open' : ''}">
    <button class="battle-history-summary" type="button" onclick="toggleBattleHistory(${idx})" aria-expanded="${open}">
      <div class="battle-history-date">${item.at}</div>
      <div class="battle-history-opponent">vs <b>${opponentCrewName}</b></div>
      <span class="battle-history-exp mono">+${Number(item.exp) || 0} EXP</span>
      <span class="pill ${item.result === '승리' ? 'pill-accent' : item.result === '패배' ? 'pill-muted' : 'pill-gold'}">${item.result}</span>
      <span class="battle-history-arrow" aria-hidden="true">${open ? '⌃' : '⌄'}</span>
    </button>
    ${open ? `
      <div class="battle-history-detail">
        ${item.sample ? '<p class="hint battle-sample-note">레이아웃 확인용 샘플 데이터</p>' : ''}
        <div class="battle-score-strip" aria-label="대전 점수 요약">
          <span class="battle-score-team"><b>${ourCrewName}</b> <b class="mono">${Number(item.ourScore) || 0}점</b></span>
          <span class="battle-score-type">${size}vs${size}</span>
          <span class="battle-score-team battle-score-opponent"><b>${opponentCrewName}</b> <b class="mono">${Number(item.oppScore) || 0}점</b></span>
        </div>
        <div class="battle-history-columns">
          ${renderBattleSide('우리 크루원', item.ourMembers || [])}
          ${renderBattleSide('상대 크루원', item.oppMembers || [])}
        </div>
      </div>` : ''}
  </div>`;
}
function drawBattleResultCharacters() {
  document.querySelectorAll('canvas[data-battle-character]').forEach(c => {
    if (c.dataset.drawn === '1') return;
    drawPixelCharacter(c, {}, c.dataset.gender || 'male');
    c.dataset.drawn = '1';
  });
}
function renderCrewBattleMenu() {
  const history = battleHistoryData();
  return `
  <div class="view-head"><h1>크루대전</h1></div>
  <section class="crew-battle-card" aria-label="크루대전 소개">
    <div class="crew-battle-card-head">
      <h2>크루대전 <span class="crew-battle-people" aria-hidden="true">👤👤👤👤👤</span></h2>
      <button class="crew-battle-cta" type="button" onclick="openPartyInvite()">크루대전 파티 맺기</button>
    </div>
    <p class="crew-battle-desc">2vs2, 3vs3, 4vs4, 5vs5 비슷한 레벨의 크루와 실시간 점수 대결을 해보세요.</p>
    <p class="crew-battle-exp">승리 시 +100 EXP · 패배 시 +50 EXP</p>
    <p class="crew-battle-empty">아직 대전 파티가 없어요.</p>
  </section>
  <div class="view-head" style="margin-top:18px;"><h2 style="margin:0;">최근 대전 결과</h2></div>
  <div class="battle-result-list">
    ${history.map((item, idx) => renderBattleHistoryCard(item, idx)).join('')}
  </div>`;
}

function renderCrewBattle() {
  const b = state.crewBattle;
  if (!b) return renderCrewBattleMenu();
  const teamTotal = b.myScore + b.teammates.reduce((s, t) => s + t.score, 0);
  return `
  <div class="view-head flex-between">
    <h1 style="margin:0;">${b.size || 5}vs${b.size || 5} 크루대전</h1>
    <button class="btn btn-ghost btn-sm" onclick="exitCrewBattle()">나가기</button>
  </div>

  <div class="card" style="text-align:center;margin-bottom:16px;">
    <div style="display:flex;align-items:center;justify-content:center;gap:10px;margin-bottom:4px;">
      <div style="display:flex;flex-direction:column;align-items:center;gap:3px;flex:1;min-width:0;">
        <span class="pill pill-accent">Lv.${state.crew.level}</span>
        <h2 style="margin:0;font-size:16px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:100%;">${state.crew.name}</h2>
      </div>
      <span style="font-size:13px;font-weight:700;color:var(--ink-faint);flex:none;">vs</span>
      <div style="display:flex;flex-direction:column;align-items:center;gap:3px;flex:1;min-width:0;">
        <span class="pill pill-muted">Lv.${b.opponent.level}</span>
        <h2 style="margin:0;font-size:16px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:100%;">${b.opponent.name}</h2>
      </div>
    </div>
    <p style="margin:0 0 18px;font-size:16px;font-weight:700;color:var(--gold);">스쿼트 ${b.target}점 먼저 채우기</p>
    <div style="display:flex;align-items:center;justify-content:center;gap:28px;flex-wrap:wrap;">
      <div>
        <div id="battle-team-total" class="mono" style="font-size:44px;font-weight:700;color:var(--accent);">${teamTotal}</div>
        <div class="hint">${state.crew.name} (우리팀)</div>
      </div>
      <div style="font-size:20px;font-weight:700;color:var(--ink-faint);">VS</div>
      <div>
        <div id="battle-opp-total" class="mono" style="font-size:44px;font-weight:700;color:var(--coral);">${b.oppScore}</div>
        <div class="hint" style="position:relative;display:inline-block;">${b.opponent.name} <span id="battle-pop-opp" style="position:relative;display:inline-block;"></span></div>
      </div>
    </div>
    <div class="progress" style="margin-top:14px;height:10px;"><span id="battle-progress" style="width:${Math.min(100, teamTotal / b.target * 100)}%"></span></div>
    <p class="hint" style="margin-top:6px;">목표 ${b.target}점을 먼저 채우는 팀이 승리해요 (PERFECT +2점 · GREAT/GOOD +1점 · MISS +0점)</p>
  </div>

  ${b.result ? `
  <div class="card" style="max-width:640px;margin:0 auto;">
    <h2 style="margin:0 0 6px;text-align:center;">${b.result === 'win' ? '🎉 우리 팀 승리!' : b.result === 'draw' ? '무승부' : '아쉽게 패배했어요'}</h2>
    <p class="desc" style="text-align:center;margin:0 0 4px;">최종 ${teamTotal}점 : ${b.oppScore}점</p>
    ${b.result === 'win' ? `<p class="mono" style="font-weight:700;color:var(--gold);margin:0 0 14px;text-align:center;">크루 포인트 획득 +${b.target}P</p>` : '<div style="margin-bottom:14px;"></div>'}
    <div class="grid grid-2" style="align-items:start;">
      <div>
        <p class="section-label" style="margin:0 0 8px;">${state.crew.name} (우리팀)</p>
        ${renderBattleRoster(battleMyRoster(b))}
      </div>
      <div>
        <p class="section-label" style="margin:0 0 8px;">${b.opponent.name} (상대팀)</p>
        ${renderBattleRoster(battleOppRoster(b))}
      </div>
    </div>
    <button class="btn btn-primary btn-block" style="margin-top:20px;" onclick="exitCrewBattle()">크루로 돌아가기</button>
  </div>` : `
  <div class="grid cal-grid">
    <div>
      <div class="cam-stage" id="cam-stage">
        <div class="cam-placeholder" id="cam-placeholder">카메라를 확인하는 중...<br>브라우저의 카메라 권한을 허용해주세요.</div>
        <video id="cam-video" autoplay playsinline muted style="display:none;"></video>
        <canvas class="cam-overlay-canvas" id="cam-canvas"></canvas>
        <div class="cam-badge"><span class="rec-dot"></span><span id="cam-status">대기중</span></div>
        <div class="cam-timer mono" id="cam-timer">00:00</div>
        <div class="cam-battle-hud">
          <div class="scores">
            <span class="my mono" id="cam-battle-my">${teamTotal}</span>
            <span class="sep">:</span>
            <span class="opp mono" id="cam-battle-opp">${b.oppScore}</span>
          </div>
          <div class="mini-gauge"><span id="cam-battle-gauge" style="width:${Math.min(100, teamTotal / b.target * 100)}%"></span></div>
        </div>
        <div id="cam-grade-flash" class="cam-grade-flash"></div>
        <div class="cam-battle-countdown" id="cam-battle-countdown"></div>
      </div>
      <div style="margin-top:14px;display:flex;gap:10px;align-items:center;">
        <span class="hint" style="margin:0;position:relative;">내 기록 <b id="battle-my-score" class="mono">${b.myScore}</b>점 <span id="battle-pop-me" style="position:relative;display:inline-block;"></span></span>
      </div>
    </div>
    <div class="card">
      <p class="section-label">우리 팀 (실시간 자동 진행)</p>
      <div style="display:grid;grid-template-columns:repeat(2,1fr);gap:16px;">
        ${b.teammates.map((t, i) => `
          <div style="text-align:center;">
            <div class="battle-char-wrap" style="animation-duration:${t.dur}s;">
              <canvas id="battle-char-${i}" width="90" height="110" style="width:70px;height:86px;image-rendering:pixelated;"></canvas>
            </div>
            <p style="margin:4px 0 0;font-size:12px;font-weight:700;">${t.n}</p>
            <p class="mono" style="margin:0;font-size:13px;color:var(--ink-dim);position:relative;display:inline-block;">
              <span id="battle-mate-score-${i}">${t.score}</span>점 <span id="battle-pop-mate-${i}" style="position:relative;display:inline-block;"></span>
            </p>
          </div>`).join('')}
      </div>
    </div>
  </div>`}`;
}

/* ---------- 크루탈퇴: 일반 크루원 전용 ---------- */
function renderCrewLeave() {
  if (getMyCrewRole() === '팀장') return '<div class="empty-note">크루장에게는 크루탈퇴 메뉴가 표시되지 않습니다.</div>';
  return `
  <div class="crew-leave-page">
    <p class="section-label">크루 탈퇴</p>
    <h3 style="margin:0 0 8px;">현재 가입된 크루에서 탈퇴합니다.</h3>
    <p class="desc">탈퇴 후에는 해당 크루의 활동 및 크루 관련 기능을 이용할 수 없습니다.</p>
    <div class="crew-leave-action">
      <button class="btn btn-danger" onclick="confirmCrewLeave()">크루 탈퇴</button>
    </div>
  </div>`;
}
function confirmCrewLeave(){
  if(getMyCrewRole()==='팀장'){toast('크루장은 이 메뉴에서 탈퇴할 수 없습니다');return;}
  askConfirm(
    '크루 탈퇴',
    '탈퇴하면 현재 크루의 멤버 목록에서 제외되며,\n크루 활동 및 관련 기능을 더 이상 이용할 수 없습니다.\n\n[탈퇴 후 가입 제한 안내]\n- 현재 크루 재가입: 탈퇴일로부터 7일 후 가입할 수 있습니다.\n- 다른 크루 가입: 탈퇴 시점으로부터 24시간 후 가입할 수 있습니다.\n\n※ 탈퇴 후에는 위 기간 동안 크루 가입이 제한됩니다.\n정말 크루에서 탈퇴하시겠습니까?',
    ()=>{leaveCrewMember();},
    '크루 탈퇴',
    true
  );
}
async function leaveCrewMember(){
  try{
    const res=await fetch(`${API_BASE}/api/crews/me/leave`,{method:'POST',headers:{'Authorization':'Bearer '+state.token}});
    const body=await res.json();
    if(!body.success){toast(body.message||'크루 탈퇴에 실패했습니다');return;}
    state.crew.created=false;
    state.crew.id=null;
    state.crew.name=''; state.crew.desc=''; state.crew.region=''; state.crew.leaderRegion=''; state.crew.concepts=[]; state.crew.members=[];
    state.crew.joinRequests=[];
    state.subtabs.crew=0;
    closeConfirm();
    disconnectCrewChat(); disconnectJoinWait();
    toast('크루에서 탈퇴했습니다');
    await loadJoinableCrews();
    render();
  }catch(err){
    toast('서버에 연결할 수 없습니다');
  }
}

/* ---------- 크루원 정보: 조회 전용 (강퇴 기능은 크루원관리 탭으로 이동) ---------- */
function renderCrewMembers() {
  return `
  <div class="table-wrap">
    <table>
      <thead><tr><th>이름</th><th>역할</th><th>레벨</th></tr></thead>
      <tbody>
        ${state.crew.members.map(m => `
          <tr>
            <td>${m.n}${isCurrentCrewMember(m) ? ' <span class="pill pill-accent">나</span>' : ''}${m.role === '팀장' ? ' <span class="pill pill-gold">크루장</span>' : ''}</td>
            <td><span class="pill ${m.role === '팀장' ? 'pill-gold' : 'pill-muted'}">${crewRoleLabel(m.role)}</span></td>
            <td class="mono">Lv.${m.level}</td>
          </tr>`).join('')}
      </tbody>
    </table>
  </div>`;
}
/* ---------- 크루원관리: 팀장 전용 — 크루 소개 수정 + 가입요청 승인 + 강퇴 ---------- */
function renderCrewManage() {
  if (getMyCrewRole() !== '팀장') { return '<div class="empty-note">팀장만 접근할 수 있는 메뉴입니다.</div>'; }
  const reqs = state.crew.joinRequests;
  return `
  <div class="card" style="max-width:520px;margin:0 auto 20px;">
    <p class="section-label">크루 소개 수정</p>
    <div class="field"><textarea id="crew-desc-edit" rows="3">${state.crew.desc}</textarea></div>
    <div class="field">
      <label>크루 컨셉 (최대 ${CREW_CONCEPT_MAX}개, 눌러서 켜고 끄기)</label>
      <div style="display:flex;flex-wrap:wrap;gap:8px;">
        ${CREW_CONCEPTS.map(c => `<button type="button" class="btn btn-sm ${state.crew.concepts.includes(c) ? 'btn-primary' : 'btn-secondary'}" onclick="setCrewConcept('${c}')">#${c}</button>`).join('')}
      </div>
    </div>
    <button class="btn btn-secondary" onclick="updateCrewDesc()">소개 저장</button>
  </div>
  <p class="section-label">가입 요청 (${reqs.length})</p>
  <div class="grid grid-2" style="margin-bottom:24px;">
    ${reqs.length ? reqs.map((r, idx) => `
      <div class="card">
        <div class="flex-between"><h3 style="margin:0;">${r.n}</h3><span class="pill pill-gold">Lv.${r.level}</span></div>
       <p class="desc" style="margin-top:8px;">${r.msg}</p>
        <div class="flex-between" style="margin-top:10px;gap:8px;">
          <button class="btn btn-sm btn-secondary" style="flex:1;" onclick="rejectJoinRequest(${r.id})">거절</button>
          <button class="btn btn-sm btn-primary" style="flex:1;" onclick="approveJoinRequest(${r.id})">승인</button>
        </div>

      </div>`).join('') : '<div class="empty-note" style="grid-column:1/-1;">대기중인 가입 요청이 없어요.</div>'}
  </div>
  <p class="section-label">크루원 강퇴</p>
  <div class="table-wrap">
    <table>
      <thead><tr><th>이름</th><th>역할</th><th>레벨</th><th>관리</th></tr></thead>
      <tbody>
        ${state.crew.members.map(m => `
          <tr>
            <td>${m.n}${isCurrentCrewMember(m) ? ' <span class="pill pill-accent">나</span>' : ''}${m.role === '팀장' ? ' <span class="pill pill-gold">크루장</span>' : ''}</td>
            <td><span class="pill ${m.role === '팀장' ? 'pill-gold' : 'pill-muted'}">${crewRoleLabel(m.role)}</span></td>
            <td class="mono">Lv.${m.level}</td>
            <td>${!isCurrentCrewMember(m) && m.role !== '팀장' ? `<button class="btn btn-sm btn-danger" onclick="kickMember(${m.userId})">강퇴</button>` : '<span class="hint">관리 제외</span>'}</td>
          </tr>`).join('')}
      </tbody>
    </table>
  </div>`;
}
// 크루 컨셉은 최대 3개를 배열로 전송한다. 이전 서버와의 호환을 위해 concept도 함께 보낸다.
async function updateCrewDesc() {
  const v = document.getElementById('crew-desc-edit').value.trim();
  if (!v) { toast('소개글을 입력해주세요'); return; }
  try {
    const res = await fetch(`${API_BASE}/api/crews/me`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + state.token },
      body: JSON.stringify({ description: v, concepts: [...state.crew.concepts], concept: state.crew.concepts[0] })
    });
    const body = await res.json();
    if (!body.success) { toast(body.message || '저장에 실패했습니다'); return; }
    state.crew.desc = body.data.description;
    state.crew.concepts = normalizeCrewConcepts(body.data.concepts, body.data.concept || state.crew.concepts);
    toast('크루 소개를 저장했습니다');
    render();
  } catch (err) {
    toast('서버에 연결할 수 없습니다');
  }
}


async function kickMember(targetUserId) {
  const target = state.crew.members.find(m => Number(m.userId) === Number(targetUserId));
  if (!target || isCurrentCrewMember(target) || target.role === '팀장') {
    toast('크루장은 강퇴하거나 권한 변경할 수 없습니다');
    return;
  }
  try{
    const res = await fetch(`${API_BASE}/api/crews/me/members/${targetUserId}`, {
      method:'DELETE', headers:{ 'Authorization': 'Bearer ' + state.token }
    });
    const body = await res.json();
    if(!body.success){ toast(body.message || '강퇴에 실패했습니다'); return; }
    // 성공하면 서버가 /topic/crews/{id}/members로 브로드캐스트하고, 크루장인 나를 포함한
    // 크루원 전원이 handleCrewMemberEvent()에서 그 이벤트로 화면을 갱신한다 — 여기서 별도로
    // 갱신하면 브로드캐스트 도착분과 중복되어 토스트가 두 번 뜬다.
  }catch(err){
    toast('서버에 연결할 수 없습니다');
  }
}


/* ---------- 크루 랭킹: 시/구/동 드롭다운 랭킹 + 시/구 드롭다운 지도 (#18, #19) ---------- */
const CREW_NAME_POOL = ['역삼동 러너스', '합정 플랭커즈', '성수 스쿼트단', '오룡 파워워커즈', '상무 헬스메이트', '망원 버피팀', '잠실 런지크루', '봉선 조깅단'];
function getDongCrewRanking(dong) {
  const seed = hashStr(dong);
  const names = [];
  let idx = seed;
  while (names.length < 3) {
    idx = (idx * 48271 + 1) % 2147483647;
    const name = CREW_NAME_POOL[idx % CREW_NAME_POOL.length];
    if (!names.includes(name)) names.push(name);
  }
  return names.map((name, i) => ({
    rank: i + 1, name,
    level: Math.max(1, 12 - i * 2 - (seed % 3)),
    score: 5200 - i * 430 - (seed % 100),
  }));
}
function getMyCrewDong() {
  const region = state.crew.region || state.user.region || '';
  return region.trim().split(/\s+/).pop();
}
function getMyDongCrewRank() {
  const dong = getMyCrewDong();
  const myScore = state.crew.members.reduce((s, m) => s + m.score, 0);
  const others = getDongCrewRanking(dong).filter(c => c.name !== state.crew.name);
  const rows = [...others, { name: state.crew.name, score: myScore }].sort((a, b) => b.score - a.score);
  return { dong, rank: rows.findIndex(r => r.name === state.crew.name) + 1 };
}
// 실제 데이터는 GET /api/rankings/crew?city&gu 로 "구" 단위까지만 집계해서 받아온다(동 단위
// 집계는 백엔드에 없음). 동 필터는 이미 받아온 구 단위 목록을 프론트에서 한 번 더 걸러
// 다시 순위를 매기는 방식으로 처리하고, 지도 패널도 같은 데이터를 동별로 묶어서 재사용한다
// — 그래서 랭킹 목록과 지도가 항상 같은 시/구를 보게 되고(예전엔 각자 다른 시/구를 고를 수
// 있어서 지도가 목록과 다른 지역을 보여줄 수 있는 버그가 있었다), API 호출도 한 번으로 끝난다.
async function loadCrewRegionRanking() {
  const cities = Object.keys(REGION_DATA);
  const city = REGION_DATA[state.crew.rankCity] ? state.crew.rankCity : cities[0];
  const gus = Object.keys(REGION_DATA[city]);
  const gu = REGION_DATA[city][state.crew.rankGu] ? state.crew.rankGu : gus[0];
  try {
    const res = await fetch(`${API_BASE}/api/rankings/crew?city=${encodeURIComponent(city)}&gu=${encodeURIComponent(gu)}`);
    const body = await res.json();
    if (!body.success) return;
    state.rank.crew = body.data.map(r => ({ rank: r.rank, name: r.crewName, level: r.level, currentExp:r.currentExp??r.exp??0, reachedAt:r.reachedAt||'', score: r.totalScore, region: r.region }));
    render();
  } catch (err) {
    console.error('크루 랭킹 불러오기 실패', err);
  }
}
function renderCrewRegionRank() {
  const cities = Object.keys(REGION_DATA);
  const rankCity = REGION_DATA[state.crew.rankCity] ? state.crew.rankCity : cities[0];
  const rankGus = Object.keys(REGION_DATA[rankCity]);
  const rankGu = REGION_DATA[rankCity][state.crew.rankGu] ? state.crew.rankGu : rankGus[0];
  const dongs = REGION_DATA[rankCity][rankGu];
  const rankDong = dongs.includes(state.crew.rankDong) ? state.crew.rankDong : dongs[0];
  const dongRows = state.rank.crew.filter(r => r.region === rankDong).map((r, i) => ({ ...r, rank: i + 1 }));
  const rest = dongRows.filter(r => r.rank > 3);
  return `
  <p class="section-label">동네별 크루 랭킹</p>
  <div class="crew-ranking-filter-area">
    <div class="filter-bar crew-ranking-filter-bar">
      <select onchange="setCrewRankCity(this.value)">
        ${cities.map(c => `<option ${c === rankCity ? 'selected' : ''}>${c}</option>`).join('')}
      </select>
      <select onchange="setCrewRankGu(this.value)">
        ${rankGus.map(g => `<option ${g === rankGu ? 'selected' : ''}>${g}</option>`).join('')}
      </select>
      <select onchange="setCrewRankDong(this.value)">
        ${dongs.map(d => `<option ${d === rankDong ? 'selected' : ''}>${d}</option>`).join('')}
      </select>
    </div>
    <p class="ranking-criteria crew-ranking-criteria">크루 랭킹: 레벨 → 현재 경험치 → 도달 시각 순으로 순위가 결정됩니다.</p>
  </div>
  <div class="ranking-data-offset crew-ranking-data-offset">
  ${dongRows.length === 0 ? `<div class="empty-note">이 동네엔 아직 등록된 크루가 없습니다.</div>` : `
  ${renderPodium(dongRows)}
  ${rest.length ? `
  <div class="table-wrap">
    <table>
      <thead><tr><th>순위</th><th>크루명</th><th>레벨</th><th>누적 점수</th></tr></thead>
      <tbody>
        ${rest.map(r => `
          <tr>
            <td><span class="rank-num">${r.rank}</span></td>
            <td>${r.name}</td>
            <td class="mono">Lv.${r.level}</td>
            <td class="mono">${r.score.toLocaleString()}</td>
          </tr>`).join('')}
      </tbody>
    </table>
  </div>`:''}`}
  </div>`;
}
function setCrewRankCity(v) { state.crew.rankCity = v; state.crew.rankGu = null; state.crew.rankDong = null; loadCrewRegionRanking(); render(); }
function setCrewRankGu(v) { state.crew.rankGu = v; state.crew.rankDong = null; loadCrewRegionRanking(); render(); }
function setCrewRankDong(v) { state.crew.rankDong = v; render(); } // 동 변경은 이미 받아온 구 단위 데이터를 재필터링만 하면 돼서 재요청 불필요

/* ========================================================================
   4. 랭킹
   ======================================================================== */
// (FR-RK-001~002) 랭킹 탭(ranking.js)의 지역별/종목별/크루 랭킹은 모두 GET /api/rankings/*
// 실제 API로 연결됨(2026-09-10). getRegionRanking()/getDongCrewRanking()는 랭킹 탭이 아니라
// 메인 대시보드 요약 위젯에서만 쓰는 가벼운 mock이라 그대로 남겨뒀다(profile.js 참고).
