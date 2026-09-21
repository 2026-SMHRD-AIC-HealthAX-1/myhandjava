// admin.js — '관리자모드' 전용 화면. state.screen==='admin'일 때 router.js가 renderAdminApp()을
// 부른다. 기존 오운홈 테마 클래스(.app-shell/.sidebar/.navitem/.card/.table-wrap/.btn*/.pill*)를
// 그대로 재사용하고, 이 파일에서는 새 CSS를 거의 추가하지 않는다.

function openAdminPanel(){
  if(state.guestMode || state.user.role !== 'ADMIN'){ toast('관리자만 접근할 수 있습니다'); return; }
  state.screen = 'admin';
  setAdminTab('dashboard');
}
function exitAdminPanel(){
  state.screen = 'app';
  render();
}
const ADMIN_TABS = [
  { id: 'dashboard', label: '대시보드', icon: '📊' },
  { id: 'users', label: '전체 사용자 관리', icon: '👥' },
  { id: 'reports', label: '크루채팅 신고 관리', icon: '💬' },
  { id: 'tickets', label: '고객센터 문의 관리', icon: '🎧' },
  { id: 'missions', label: '미션 관리', icon: '🛠️' },
];
function setAdminTab(tab){
  state.adminPanel.tab = tab;
  render();
  if(tab === 'dashboard') loadAdminDashboard();
  else if(tab === 'users') loadAdminUsers(state.adminPanel.usersSearch);
  else if(tab === 'reports') loadAdminCrewChatReports();
  else if(tab === 'tickets') loadAllSupportTickets();
  else if(tab === 'missions') loadAdminMissions();
}
function renderAdminApp(){
  const tab = state.adminPanel.tab;
  return `
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand" onclick="setAdminTab('dashboard')" style="cursor:pointer;" title="관리자 대시보드로">
        <div class="brand-mark"><img src="assets/logo.png" alt="오운홈"></div>
        <div class="brand-name">오운홈<small>관리자</small></div>
      </div>
      ${ADMIN_TABS.map(t => `
        <div class="navitem ${tab === t.id ? 'active' : ''}" onclick="setAdminTab('${t.id}')">
          <span class="navicon"></span><span class="navicon-emoji">${t.icon}</span><span class="navlabel">${t.label}</span>
        </div>`).join('')}
      <div class="sidebar-footer">
        <div class="navitem" onclick="exitAdminPanel()">
          <span class="navicon"></span><span class="navicon-emoji">🚪</span><span class="navlabel">일반 모드로 나가기</span>
        </div>
      </div>
    </aside>
    <div class="main">
      <div class="topbar">
        <div><div class="topbar-title">오운홈 관리자</div></div>
        <div class="user-chip">
          <span class="topbar-nick">${escapeHtml(state.user.nickname || '관리자')}</span>
        </div>
      </div>
      <div class="view">
        ${tab === 'dashboard' ? renderAdminDashboard()
          : tab === 'users' ? renderAdminUsers()
          : tab === 'reports' ? renderAdminCrewChatReports()
          : tab === 'tickets' ? renderAdminSupportTickets()
          : renderAdminMissions()}
      </div>
    </div>
  </div>`;
}

/* ---------- 대시보드 ---------- */
async function loadAdminDashboard(){
  try{
    const res = await fetch(`${API_BASE}/api/admin/dashboard`, { headers: { 'Authorization': 'Bearer ' + state.token } });
    const body = await res.json();
    if(!body.success) return;
    state.adminPanel.dashboard = body.data;
    render();
  }catch(err){ console.error('관리자 대시보드 불러오기 실패', err); }
}
function renderAdminDashboard(){
  const d = state.adminPanel.dashboard;
  if(!d) return '<p class="hint">불러오는 중...</p>';
  const statCard = (label, value, sub) => `
    <div class="card"><p class="section-label" style="margin:0 0 6px;">${label}</p><p class="mono" style="font-size:26px;font-weight:700;margin:0;">${value}</p>${sub ? `<p class="hint" style="margin-top:4px;">${sub}</p>` : ''}</div>`;
  return `
  <div class="view-head"><h1>대시보드</h1><p>오운홈 서비스 현황을 한눈에 확인하세요</p></div>
  <div class="grid" style="grid-template-columns:repeat(4,1fr);margin-bottom:20px;">
    ${statCard('전체 회원 수', d.totalUsers.toLocaleString() + '명')}
    ${statCard('오늘 운동 인증', d.todayExerciseCount.toLocaleString() + '건')}
    ${statCard('미처리 신고', d.pendingReportCount.toLocaleString() + '건', d.pendingReportCount > 0 ? '확인이 필요합니다' : '')}
    ${statCard('미답변 문의', d.unansweredTicketCount.toLocaleString() + '건')}
  </div>
  <div class="grid grid-fixed-2">
    <div class="card">
      <div class="flex-between"><h3 style="margin:0;">최근 크루채팅 신고</h3><span class="hint mono" style="cursor:pointer;" onclick="setAdminTab('reports')">전체 보기 →</span></div>
      <div style="display:flex;flex-direction:column;gap:10px;margin-top:12px;">
        ${d.recentReports.length ? d.recentReports.map(r => `
        <div class="flex-between">
          <div><b>${escapeHtml(r.reporterNickname)} → ${escapeHtml(r.targetNickname)}</b><p class="hint" style="margin:2px 0 0;">${fmtChatTime(r.reportedAt)}</p></div>
          <span class="pill ${r.status==='PENDING'?'pill-gold':'pill-accent'}">${r.status==='PENDING'?'대기':'처리완료'}</span>
        </div>`).join('') : '<p class="empty-note">신고 내역이 없습니다.</p>'}
      </div>
    </div>
    <div class="card">
      <div class="flex-between"><h3 style="margin:0;">최근 고객센터 문의</h3><span class="hint mono" style="cursor:pointer;" onclick="setAdminTab('tickets')">전체 보기 →</span></div>
      <div style="display:flex;flex-direction:column;gap:10px;margin-top:12px;">
        ${d.recentTickets.length ? d.recentTickets.map(t => `
        <div class="flex-between">
          <div><b>${escapeHtml(t.title)}</b><p class="hint" style="margin:2px 0 0;">${escapeHtml(t.authorNickname)} · ${fmtChatTime(t.createdAt)}</p></div>
          <span class="pill ${t.status==='ANSWERED'?'pill-accent':'pill-gold'}">${SUPPORT_STATUS_LABEL[t.status] || t.status}</span>
        </div>`).join('') : '<p class="empty-note">문의 내역이 없습니다.</p>'}
      </div>
    </div>
  </div>`;
}

/* ---------- 전체 사용자 관리 ---------- */
async function loadAdminUsers(search){
  try{
    const q = search ? `?search=${encodeURIComponent(search)}` : '';
    const res = await fetch(`${API_BASE}/api/admin/users${q}`, { headers: { 'Authorization': 'Bearer ' + state.token } });
    const body = await res.json();
    if(!body.success) return;
    state.adminPanel.users = body.data;
    render();
  }catch(err){ console.error('관리자 회원 목록 불러오기 실패', err); }
}
function setAdminUsersSearchInput(v){
  state.adminPanel.usersSearch = v;
}
function runAdminUsersSearch(){
  loadAdminUsers(state.adminPanel.usersSearch);
}
async function suspendAdminUser(userId){
  await patchAdminUserStatus(userId, 'suspend', '정지');
}
async function activateAdminUser(userId){
  await patchAdminUserStatus(userId, 'activate', '활성화');
}
async function patchAdminUserStatus(userId, action, label){
  try{
    const res = await fetch(`${API_BASE}/api/admin/users/${userId}/${action}`, { method:'PATCH', headers: { 'Authorization': 'Bearer ' + state.token } });
    const body = await res.json();
    if(!body.success){ toast(body.message || `${label} 처리에 실패했습니다`); return; }
    toast(`계정을 ${label}했습니다`);
    await loadAdminUsers(state.adminPanel.usersSearch);
  }catch(err){ toast('서버에 연결할 수 없습니다'); }
}
function renderAdminUsers(){
  const users = state.adminPanel.users;
  return `
  <div class="view-head"><h1>전체 사용자 관리</h1><p>가입된 모든 회원을 검색하고 상태를 관리하세요</p></div>
  <div class="flex-between" style="margin-bottom:14px;">
    <div class="admin-search-bar">
      <input placeholder="이름 또는 이메일로 검색" value="${escapeHtml(state.adminPanel.usersSearch || '')}" oninput="setAdminUsersSearchInput(this.value)" onkeydown="if(event.key==='Enter') runAdminUsersSearch();">
      <button class="btn btn-sm btn-secondary" onclick="runAdminUsersSearch()">검색</button>
    </div>
    <span class="hint">총 ${users.length}명</span>
  </div>
  <div class="table-wrap compact-table">
    <table>
      <thead><tr><th>이름</th><th>이메일</th><th>가입일</th><th>인증 횟수</th><th>상태</th><th>액션</th></tr></thead>
      <tbody>
        ${users.length ? users.map(u => `
        <tr>
          <td><span class="name-cell"><span class="user-avatar" style="background:${avatarColor(u.id)}">${avatarInitial(u.nickname)}</span>${escapeHtml(u.nickname)}</span></td>
          <td>${escapeHtml(u.email)}</td>
          <td class="mono">${new Date(u.createdAt).toLocaleDateString('ko-KR')}</td>
          <td class="mono">${u.exerciseCount}회</td>
          <td><span class="pill ${u.status==='ACTIVE' ? 'pill-accent' : 'pill-danger'}">${u.status==='ACTIVE' ? '활성' : '정지'}</span></td>
          <td>${u.status==='ACTIVE'
            ? `<button class="btn btn-sm btn-danger" onclick="suspendAdminUser(${u.id})">정지</button>`
            : `<button class="btn btn-sm btn-secondary" onclick="activateAdminUser(${u.id})">활성화</button>`}</td>
        </tr>`).join('') : '<tr><td colspan="6"><div class="empty-note">검색 결과가 없습니다.</div></td></tr>'}
      </tbody>
    </table>
  </div>`;
}

/* ---------- 크루채팅 신고 관리 ---------- */
async function loadAdminCrewChatReports(){
  try{
    const res = await fetch(`${API_BASE}/api/admin/crew-chat-reports`, { headers: { 'Authorization': 'Bearer ' + state.token } });
    const body = await res.json();
    if(!body.success) return;
    state.adminPanel.reports = body.data;
    render();
  }catch(err){ console.error('관리자 신고 목록 불러오기 실패', err); }
}
function toggleAdminReportDetail(id){
  state.adminPanel.reportDetailId = state.adminPanel.reportDetailId === id ? null : id;
  render();
}
async function resolveAdminReport(reportId){
  try{
    const res = await fetch(`${API_BASE}/api/admin/crew-chat-reports/${reportId}/resolve`, { method:'PATCH', headers: { 'Authorization': 'Bearer ' + state.token } });
    const body = await res.json();
    if(!body.success){ toast(body.message || '처리에 실패했습니다'); return; }
    toast('처리완료로 표시했습니다');
    await loadAdminCrewChatReports();
  }catch(err){ toast('서버에 연결할 수 없습니다'); }
}
async function suspendReportedUser(reportId, targetUserId){
  await patchAdminUserStatus(targetUserId, 'suspend', '정지');
  await resolveAdminReport(reportId);
}
function renderAdminCrewChatReports(){
  const reports = state.adminPanel.reports;
  return `
  <div class="view-head"><h1>크루채팅 신고 관리</h1><p>신고 접수된 채팅 내용을 확인하고 사용자를 제재하세요</p></div>
  <div class="table-wrap compact-table">
    <table>
      <thead><tr><th>신고자</th><th>피신고자</th><th>채팅 내용</th><th>채팅 일시</th><th>신고 일시</th><th>상태</th><th>액션</th></tr></thead>
      <tbody>
        ${reports.length ? reports.map(r => `
        <tr>
          <td>${escapeHtml(r.reporterNickname)}</td>
          <td>${escapeHtml(r.targetNickname)}</td>
          <td style="max-width:260px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">${escapeHtml(r.targetNickname)}: ${escapeHtml(r.messageText || '')}</td>
          <td class="mono">${fmtChatTime(r.chatSentAt)}</td>
          <td class="mono">${fmtChatTime(r.reportedAt)}</td>
          <td><span class="pill ${r.status==='PENDING'?'pill-gold':'pill-accent'}">${r.status==='PENDING'?'대기':'처리완료'}</span></td>
          <td><button class="btn btn-sm btn-secondary" onclick="toggleAdminReportDetail(${r.id})">상세보기</button></td>
        </tr>
        ${state.adminPanel.reportDetailId === r.id ? `
        <tr><td colspan="7">
          <div class="card" style="margin:0;">
            <p class="section-label" style="margin:0 0 6px;">채팅 내용</p>
            <p class="desc" style="margin:0 0 14px;">${escapeHtml(r.targetNickname)}: ${escapeHtml(r.messageText || '')}</p>
            <div style="display:flex;gap:8px;">
              ${r.status==='PENDING' ? `<button class="btn btn-sm btn-secondary" onclick="resolveAdminReport(${r.id})">처리완료로 표시</button>` : ''}
              <button class="btn btn-sm btn-danger" onclick="suspendReportedUser(${r.id}, ${r.targetUserId})">피신고자 정지</button>
            </div>
          </div>
        </td></tr>` : ''}
        `).join('') : '<tr><td colspan="7"><div class="empty-note">신고 내역이 없습니다.</div></td></tr>'}
      </tbody>
    </table>
  </div>`;
}

/* ---------- 고객센터 문의 관리 — support.js의 기존 관리자 기능을 그대로 재사용한다 ---------- */
function renderAdminSupportTickets(){
  const s = state.support;
  const list = s.filter === 'all' ? s.adminTickets : s.adminTickets.filter(t => t.status === s.filter);
  return `
  <div class="view-head"><h1>고객센터 문의 관리</h1><p>접수된 불편사항을 확인하고 답변 상태를 관리하세요</p></div>
  <div class="filter-bar" style="margin-bottom:14px;">
    ${['all','접수','처리중','답변완료'].map(f => `<button class="btn btn-sm ${s.filter===f?'btn-primary':'btn-secondary'}" onclick="setSupportFilter('${f}')">${f==='all'?'전체':f}</button>`).join('')}
  </div>
  <div class="grid grid-2">
    ${list.length===0 ? '<div class="empty-note">해당하는 문의 내역이 없습니다.</div>' : list.map(renderAdminTicketCard).join('')}
  </div>`;
}

/* ---------- 미션 관리 — 실제 백엔드(/api/admin/mission-definitions)에 연결한다 ---------- */
const ADMIN_MISSION_METRICS = [
  { value: 'REPS', label: '운동 횟수 (REPS)', min: 15, max: 30 },
  { value: 'PERFECT', label: '퍼펙트 횟수 (PERFECT)', min: 3, max: 8 },
  { value: 'SESSIONS', label: '세트 완료 수 (SESSIONS)', min: 1, max: 2 },
  { value: 'MISS_FREE_SESSION', label: 'MISS 0회 세트 (MISS_FREE_SESSION)', min: 1, max: 1 },
  { value: 'ACC_SESSION', label: '정확도 90%+ 세트 (ACC_SESSION)', min: 1, max: 2 },
];
async function loadAdminMissions(){
  try{
    const res = await fetch(`${API_BASE}/api/admin/mission-definitions`, { headers: { 'Authorization': 'Bearer ' + state.token } });
    const body = await res.json();
    if(!body.success) return;
    state.adminPanel.missions = body.data;
    render();
  }catch(err){ console.error('관리자 미션 목록 불러오기 실패', err); }
}
function adminMissionMetricInfo(value){
  return ADMIN_MISSION_METRICS.find(m => m.value === value) || ADMIN_MISSION_METRICS[0];
}
function editAdminMission(id){
  const m = state.adminPanel.missions.find(x => x.id === id);
  if(!m) return;
  state.adminPanel.editingMissionId = id;
  render();
  document.getElementById('am-scope').value = m.scope;
  document.getElementById('am-metric').value = m.metric;
  document.getElementById('am-ex').value = m.exerciseType;
  document.getElementById('am-min').value = m.minTarget;
  document.getElementById('am-max').value = m.maxTarget;
  document.getElementById('am-points').value = m.rewardPoints;
  document.getElementById('am-exp').value = m.rewardExp;
  document.getElementById('am-label').value = m.label || '';
}
function cancelEditAdminMission(){
  state.adminPanel.editingMissionId = null;
  render();
}
async function submitAdminMission(){
  const body = {
    scope: document.getElementById('am-scope').value,
    metric: document.getElementById('am-metric').value,
    exerciseType: document.getElementById('am-ex').value.trim() || '스쿼트',
    minTarget: Number(document.getElementById('am-min').value),
    maxTarget: Number(document.getElementById('am-max').value),
    rewardPoints: Number(document.getElementById('am-points').value),
    rewardExp: Number(document.getElementById('am-exp').value),
    label: document.getElementById('am-label').value.trim() || null,
  };
  const editingId = state.adminPanel.editingMissionId;
  try{
    const res = await fetch(`${API_BASE}/api/admin/mission-definitions${editingId ? '/' + editingId : ''}`, {
      method: editingId ? 'PUT' : 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + state.token },
      body: JSON.stringify(body)
    });
    const resBody = await res.json();
    if(!resBody.success){ toast(resBody.message || '저장에 실패했습니다'); return; }
    toast(editingId ? '미션을 수정했습니다' : '미션을 등록했습니다');
    state.adminPanel.editingMissionId = null;
    await loadAdminMissions();
  }catch(err){ toast('서버에 연결할 수 없습니다'); }
}
async function toggleAdminMissionActive(id, active){
  try{
    const res = await fetch(`${API_BASE}/api/admin/mission-definitions/${id}/${active ? 'deactivate' : 'activate'}`, { method:'PATCH', headers: { 'Authorization': 'Bearer ' + state.token } });
    const body = await res.json();
    if(!body.success){ toast(body.message || '상태 변경에 실패했습니다'); return; }
    await loadAdminMissions();
  }catch(err){ toast('서버에 연결할 수 없습니다'); }
}
function renderAdminMissions(){
  const missions = state.adminPanel.missions;
  const editing = state.adminPanel.editingMissionId;
  const editingMission = editing ? missions.find(m => m.id === editing) : null;
  return `
  <div class="view-head"><h1>미션 관리</h1><p>개인 미션과 크루 미션을 등록·수정·조회하고 활성 상태를 관리합니다.</p></div>
  <div class="card" style="margin-bottom:20px;">
    <p class="section-label">${editingMission ? '미션 수정' : '새 미션 등록'}</p>
    <div class="grid grid-3">
      <div class="field"><label>구분</label><select id="am-scope"><option value="PERSONAL">개인</option><option value="CREW">크루</option></select></div>
      <div class="field"><label>미션 종류</label><select id="am-metric">${ADMIN_MISSION_METRICS.map(m => `<option value="${m.value}">${m.label}</option>`).join('')}</select></div>
      <div class="field"><label>운동 종류</label><input id="am-ex" value="스쿼트"></div>
      <div class="field"><label>최소/최대 목표값</label><div class="field-row"><input id="am-min" type="number" value="15"><input id="am-max" type="number" value="30"></div></div>
      <div class="field"><label>보상(P/EXP)</label><div class="field-row"><input id="am-points" type="number" value="50"><input id="am-exp" type="number" value="50"></div></div>
      <div class="field"><label>표시 이름(선택)</label><input id="am-label" placeholder="비워두면 자동 생성"></div>
    </div>
    <div style="display:flex;gap:8px;">
      <button class="btn btn-primary" onclick="submitAdminMission()">${editingMission ? '수정 저장' : '미션 등록'}</button>
      ${editingMission ? `<button class="btn btn-secondary" onclick="cancelEditAdminMission()">취소</button>` : ''}
    </div>
  </div>
  <div class="table-wrap compact-table">
    <table>
      <thead><tr><th>구분</th><th>운동</th><th>미션 종류</th><th>범위</th><th>보상</th><th>이름</th><th>상태</th><th>관리</th></tr></thead>
      <tbody>
        ${missions.length ? missions.map(m => `
        <tr>
          <td>${m.scope==='PERSONAL'?'개인':'크루'}</td>
          <td>${escapeHtml(m.exerciseType)}</td>
          <td>${adminMissionMetricInfo(m.metric).label}</td>
          <td class="mono">${m.minTarget}~${m.maxTarget}</td>
          <td class="mono">${m.rewardPoints}P / ${m.rewardExp}EXP</td>
          <td>${escapeHtml(m.label || '-')}</td>
          <td><button class="btn btn-sm ${m.active?'btn-primary':'btn-ghost'}" onclick="toggleAdminMissionActive(${m.id}, ${m.active})">${m.active?'활성':'비활성'}</button></td>
          <td><button class="btn btn-sm btn-secondary" onclick="editAdminMission(${m.id})">수정</button></td>
        </tr>`).join('') : '<tr><td colspan="8"><div class="empty-note">등록된 미션이 없습니다.</div></td></tr>'}
      </tbody>
    </table>
  </div>`;
}
