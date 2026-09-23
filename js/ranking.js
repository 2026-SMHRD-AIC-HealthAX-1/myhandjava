// ranking.js — '랭킹' 카테고리: 지역별/크루 랭킹.
// [담당] '랭킹' 탭(지역별/크루) + 랭킹 단상 클릭 시 뜨는 공개 프로필 팝업.
// [백엔드 연동] GET /api/rankings/region, /api/rankings/crew,
//              GET /api/users/{id}/public-profile → DB: users, exercise_records 집계 쿼리.
// [주의] "전체" 옵션을 선택하면 city/gu/dong을 아예 안 보내 전국 랭킹을 받는다(null 파라미터를
//        서버가 필터 없음으로 처리) — 빈 문자열과 null을 혼동하면 랭킹이 안 나온다.

const RANK_TABS=['지역별 랭킹','크루 랭킹'];
function renderRanking(){
  const i=state.subtabs.ranking;
  return `
  <div class="view-head"><h1>랭킹</h1></div>
  <div class="subtabs">
    ${RANK_TABS.map((t,idx)=>`<div class="tab ${i===idx?'active':''}" onclick="setSub('ranking',${idx})">${t}</div>`).join('')}
  </div>
  ${i===0?renderRankRegion():renderCrewRegionRank()}`;
}
// (#7) 1~3등은 캐릭터를 올림픽 단상 형태로, 4등부터는 기존 리스트로 보여주는 공용 포디움 컴포넌트.
// rows는 이미 순위(rank)가 매겨진 배열이어야 하며, name/level/score 필드를 사용한다.
// (#2) 1~3위는 아바타 원이 아니라 실제 픽셀 캐릭터를 단상 위에 세운다. 랭킹에 오른 다른
// 사용자의 실제 장착 아이템·성별 데이터는 없으므로, 이름을 시드로 한 결정론적 값으로
// 캐릭터 외형(성별·의상 유무)만 살짝 다르게 흉내낸다.
// opts.showScore=false면 점수 줄을 아예 안 그린다(크루 랭킹 — 크루명·레벨만 보여준다),
// opts.scoreUnit은 점수 뒤에 붙일 단위 문자열이다(지역별 랭킹 — " P"로 포인트임을 표시).
function renderPodium(rows,opts={}){
  const showScore=opts.showScore!==false;
  const scoreUnit=opts.scoreUnit||'';
  const byRank=r=>rows.find(x=>x.rank===r);
  const first=byRank(1), second=byRank(2), third=byRank(3);
  const step=(r,cls,size)=>{
    if(!r) return '<div class="podium-step" style="visibility:hidden;"></div>';
    const cid=`podium-char-${cls}-${Math.abs(hashStr(r.name+cls))}`;
    // 크루 랭킹 단상은 userId가 없다(크루는 사람이 아니니까) — 그런 경우엔 클릭 안 먹게 둔다.
    const clickable = r.userId != null;
    return `
    <div class="podium-step ${cls}" ${clickable ? `style="cursor:pointer;" onclick="openPublicProfile(${r.userId})" title="프로필 보기"` : ''}>
      <canvas class="podium-canvas" id="${cid}" data-seed="${r.name}" style="width:${size}px;height:${Math.round(size*1.22)}px;"></canvas>
      <div class="podium-name">${r.name}${r.isMe?' <span class="pill pill-accent">나</span>':''}</div>
      ${r.level!=null?`<div class="podium-lv mono">Lv.${r.level}</div>`:''}
      ${showScore?`<div class="podium-score mono">${r.isMe?'내 점수 ':''}${r.score.toLocaleString()}${scoreUnit}</div>`:''}
      <div class="podium-stand">${r.rank}</div>
    </div>`;
  };
  return `<div class="podium">${step(second,'rank2',52)}${step(first,'rank1',66)}${step(third,'rank3',52)}</div>`;
}
// 화면에 존재하는 모든 포디움 캔버스를 그린다. (render() 끝에서 매번 호출 — 포디움이 없는
// 화면에서는 querySelectorAll 결과가 비어 있어 아무 일도 하지 않는다.)
function drawPodiumChars(){
  document.querySelectorAll('canvas.podium-canvas').forEach(canvas=>{
    const seed=canvas.dataset.seed||'x';
    const h=hashStr(seed);
    const equip={ outfit:h%2===0, crown:false, badge:false, background:false, skin:false };
    const gender=h%3===0?'female':'male';
    drawPixelCharacter(canvas, equip, gender);
  });
}
function totalScore(){ return state.history.reduce((s,h)=>s+h.score,0); }
// 시 드롭다운에서 고를 수 있는 "전체" 선택지 — 세 랭킹 탭(지역별·종목별·크루) 모두 이 값이면
// city(필요하면 gu·dong도)를 서버에 아예 안 보내 전국 단위로 조회한다. 백엔드 쿼리들이 null
// 파라미터는 필터링하지 않도록 되어 있다(UserRepository/CrewRepository 참고).
const RANK_ALL_CITY='전체';
// 필터(state.rankFilter)에서 실제로 쓸 city/gu/dong을 골라낸다 — null이거나 더 이상 REGION_DATA에
// 없는 값이면 그 단계의 첫 항목으로 대체한다. render()와 loadRegionRanking()이 항상 같은 값을
// 쓰도록 로직을 한 곳에 모아둔다.
function resolveRegionFilter(f){
  const cities=Object.keys(REGION_DATA);
  if(f.city===RANK_ALL_CITY){
    return {cities, city:RANK_ALL_CITY, gus:[], gu:null, dongs:[], dong:null};
  }
  const city=REGION_DATA[f.city]?f.city:cities[0];
  const gus=Object.keys(REGION_DATA[city]);
  const gu=REGION_DATA[city][f.gu]?f.gu:gus[0];
  const dongs=REGION_DATA[city][gu];
  const dong=dongs.includes(f.dong)?f.dong:dongs[0];
  return {cities, city, gus, gu, dongs, dong};
}
// (#16) 지역별 랭킹: 실제 GET /api/rankings/region?city&gu&dong 결과를 state.rank.region에 채운다.
async function loadRegionRanking(){
  const {city,gu,dong}=resolveRegionFilter(state.rankFilter);
  const isAll=city===RANK_ALL_CITY;
  const regionQuery=isAll?'':`city=${encodeURIComponent(city)}&gu=${encodeURIComponent(gu)}&dong=${encodeURIComponent(dong)}`;
  try{
    const res = await fetch(`${API_BASE}/api/rankings/region?${regionQuery}`, {
      headers: state.token ? { 'Authorization': 'Bearer ' + state.token } : {}
    });
    const body = await res.json();
    if(!body.success) return;
    state.rank.region = body.data.map(r=>({ rank:r.rank, userId:r.userId, name:r.nickname, level:r.level, score:r.score, isMe:r.me }));
    render();
  }catch(err){
    console.error('지역 랭킹 불러오기 실패', err);
  }
}
// 메인 대시보드·마이페이지 누적성과 카드의 "동네 랭킹 #N"이 예전엔 이름 시드 기반 mock
// (getRegionRanking, 아래)을 썼다 — 랭킹 탭과 같은 실제 API로 내 동(dong) 순위만 따로
// 불러와 state.user.regionRank에 채운다. 로그인·세션복원 직후(auth.js/bootstrap.js)에 호출한다.
async function loadMyRegionRank(){
  if(!state.token || !state.user.region) return;
  const parts=state.user.region.trim().split(/\s+/);
  if(parts.length<3) return;
  const [city,gu,dong]=parts;
  try{
    const res = await fetch(`${API_BASE}/api/rankings/region?city=${encodeURIComponent(city)}&gu=${encodeURIComponent(gu)}&dong=${encodeURIComponent(dong)}`, {
      headers: { 'Authorization': 'Bearer ' + state.token }
    });
    const body = await res.json();
    if(!body.success) return;
    const mine = body.data.find(r=>r.me);
    state.user.regionRank = mine ? mine.rank : null;
  }catch(err){
    console.error('내 동네 랭킹 불러오기 실패', err);
  }
}
function renderRankRegion(){
  const {cities,city,gus,gu,dongs,dong}=resolveRegionFilter(state.rankFilter);
  const isAll=city===RANK_ALL_CITY;
  const rows=state.rank.region;
  const rest=rows.filter(r=>r.rank>3);
  return `
  <div class="filter-bar">
    <select onchange="setRankCity(this.value)">
      <option ${isAll?'selected':''}>${RANK_ALL_CITY}</option>
      ${cities.map(c=>`<option ${c===city?'selected':''}>${c}</option>`).join('')}
    </select>
    <select onchange="setRankGu(this.value)" ${isAll?'disabled':''}>${isAll?`<option>--</option>`:gus.map(g=>`<option ${g===gu?'selected':''}>${g}</option>`).join('')}</select>
    <select onchange="setRankDong(this.value)" ${isAll?'disabled':''}>${isAll?`<option>--</option>`:dongs.map(d=>`<option ${d===dong?'selected':''}>${d}</option>`).join('')}</select>
  </div>
  ${rows.length===0 ? `<div class="empty-note">${isAll?'아직':'이 지역엔 아직'} 랭킹 데이터가 없습니다.</div>` : `
  ${renderPodium(rows,{scoreUnit:' 점'})}
  ${rest.length?`
  <div class="table-wrap compact-table">
    <table>
      <thead><tr><th>순위</th><th>닉네임</th><th>레벨</th><th>티어</th><th>누적 점수</th><th>정보확인</th></tr></thead>
      <tbody>
        ${rest.map(r=>`
          <tr>
            <td><span class="rank-num">${r.rank}</span></td>
            <td><span class="name-cell"><span class="user-avatar" style="background:${avatarColor(r.rank-1)}">${avatarInitial(r.name)}</span>${r.name}${r.isMe?' <span class="pill pill-accent">나</span>':''}</span></td>
            <td class="mono">Lv.${r.level}</td>
            <td>${rankBadgeIcon(gradeFromLevel(r.level), USER_GRADE_NAMES[gradeFromLevel(r.level)], 24)}</td>
            <td class="mono">${r.score.toLocaleString()} 점</td>
            <td>${r.userId!=null?`<button class="btn btn-sm btn-secondary" onclick="openPublicProfile(${r.userId})">${r.isMe?'내 정보':'상대정보'}</button>`:''}</td>
          </tr>`).join('')}
      </tbody>
    </table>
  </div>`:''}`}`;
}
function setRankCity(v){ state.rankFilter={city:v, gu:null, dong:null}; loadRegionRanking(); render(); }
function setRankGu(v){ state.rankFilter.gu=v; state.rankFilter.dong=null; loadRegionRanking(); render(); }
function setRankDong(v){ state.rankFilter.dong=v; loadRegionRanking(); render(); }

// 랭킹 단상(top3) 아바타를 클릭하면 그 사람의 공개 프로필을 팝업으로 보여준다. 백엔드가
// isPublic=false면 nickname 말고는 아무것도 안 내려주니, 여기서 프론트가 뭘 더 막을 필요는 없다.
async function openPublicProfile(userId){
  state.publicProfileModal = { open:true, loading:true, data:null };
  render();
  try{
    const res = await fetch(`${API_BASE}/api/users/${userId}/public-profile`);
    const body = await res.json();
    if(!body.success){ toast(body.message || '프로필을 불러오지 못했습니다'); closePublicProfile(); return; }
    state.publicProfileModal = { open:true, loading:false, data:body.data };
    render();
  }catch(err){
    toast('서버에 연결할 수 없습니다');
    closePublicProfile();
  }
}
function closePublicProfile(){ state.publicProfileModal = { open:false, loading:false, data:null }; render(); }
function renderPublicProfileModal(){
  const m = state.publicProfileModal;
  const d = m.data;
  let gc = null, gcTotal = 1;
  if(d && d.isPublic){
    gc = { PERFECT:d.perfectCount, GREAT:d.greatCount, GOOD:d.goodCount, MISS:d.missCount };
    gcTotal = Object.values(gc).reduce((a,b)=>a+b,0) || 1;
  }
  return `
  <div class="confirm-backdrop" onclick="if(event.target===this) closePublicProfile()">
    <div class="confirm-box" style="max-width:380px;text-align:left;">
      ${m.loading ? `<p class="hint" style="margin:0;">불러오는 중...</p>`
        : !d ? `<p class="hint" style="margin:0;">프로필을 불러오지 못했습니다.</p>`
        : !d.isPublic ? `
          <h3 style="margin:0 0 6px;">${d.nickname}</h3>
          <p class="empty-note" style="padding:24px 0;">🔒 프로필 비공개입니다.</p>`
        : `
          <div style="display:flex;align-items:center;gap:10px;margin:0 0 4px;">
            ${rankBadgeIcon(gradeFromLevel(d.level), USER_GRADE_NAMES[gradeFromLevel(d.level)], 40)}
            <h3 style="margin:0;">${d.nickname} <span class="pill pill-gold">Lv.${d.level}</span></h3>
          </div>
          <p class="desc" style="margin:0 0 14px;">${d.bio || '자기소개가 없어요.'}</p>
          <p class="section-label" style="margin:0 0 6px;">누적 성과</p>
          <p class="desc mono" style="margin:0 0 14px;">누적 점수 <b>${d.totalScore.toLocaleString()}점</b></p>
          <p class="section-label" style="margin:0 0 6px;">등급 비율 (전체 세션 기준)</p>
          <div style="display:flex;gap:14px;flex-wrap:wrap;margin-bottom:14px;">
            <span>PERFECT <b style="color:var(--accent)">${Math.round(gc.PERFECT/gcTotal*100)}%</b></span>
            <span>GREAT <b style="color:var(--gold)">${Math.round(gc.GREAT/gcTotal*100)}%</b></span>
            <span>MISS <b style="color:var(--danger)">${Math.round(gc.MISS/gcTotal*100)}%</b></span>
          </div>
          <p class="section-label" style="margin:0 0 6px;">운동 종류별 누적 횟수</p>
          ${d.exerciseCounts.length ? `<div style="display:flex;flex-direction:column;gap:4px;margin-bottom:16px;">${d.exerciseCounts.map(e=>`<p class="desc mono" style="margin:0;">${e.exerciseType} <b>${e.count}</b>회</p>`).join('')}</div>` : `<p class="empty-note" style="margin:0 0 16px;">아직 운동 기록이 없어요.</p>`}`
      }
      <div class="confirm-actions" style="margin-top:14px;"><button class="btn btn-primary btn-sm" onclick="closePublicProfile()">닫기</button></div>
    </div>
  </div>`;
}
