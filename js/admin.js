// admin.js — '관리자모드' 전용 화면. state.screen==='admin'일 때 router.js가 renderAdminApp()을
// 부른다. 기존 오운홈 테마 클래스(.app-shell/.sidebar/.navitem/.card/.table-wrap/.btn*/.pill*)를
// 그대로 재사용하고, 이 파일에서는 새 CSS를 거의 추가하지 않는다.
// [담당] 관리자 전용 화면 4개 탭(대시보드/전체 사용자 관리/크루채팅 신고 관리/고객센터 문의
//        관리). 일반 사용자 화면(state.screen='app')과는 완전히 분리된 별도 화면.
// [백엔드 연동] GET /api/admin/dashboard, /api/admin/users(+suspend/activate),
//              /api/admin/crew-chat-reports(+resolve)
//              → 전부 백엔드에서 @PreAuthorize("hasRole('ADMIN')")로 막혀있다.
// [주의] 로그인 계정의 role이 'ADMIN'이 아니면 사이드바에 진입 버튼 자체가 안 보인다.
//        실제 관리자 권한 부여는 DB에서 직접 users.role='ADMIN'으로 바꿔야 한다(화면에서 못 줌).

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
];
function setAdminTab(tab){
  state.adminPanel.tab = tab;
  render();
  if(tab === 'dashboard') loadAdminDashboard();
  else if(tab === 'users') loadAdminUsers(state.adminPanel.usersSearch);
  else if(tab === 'reports') loadAdminCrewChatReports();
  else if(tab === 'tickets') loadAllSupportTickets();
}
function renderAdminApp(){
  const tab = state.adminPanel.tab;
  return `
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand" onclick="setAdminTab('dashboard')" style="cursor:pointer;" title="관리자 대시보드로">
        <div class="brand-mark"><img src="assets/오운홈 로고.png" alt="오운홈"></div>
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
          : renderAdminSupportTickets()}
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
  <div class="view-head"><h1>고객센터 문의 관리</h1><p>접수된 문의사항을 확인하고 답변 상태를 관리하세요</p></div>
  <div class="filter-bar" style="margin-bottom:14px;">
    ${['all','접수','처리중','답변완료'].map(f => `<button class="btn btn-sm ${s.filter===f?'btn-primary':'btn-secondary'}" onclick="setSupportFilter('${f}')">${f==='all'?'전체':f}</button>`).join('')}
  </div>
  <div class="grid grid-2">
    ${list.length===0 ? '<div class="empty-note">해당하는 문의 내역이 없습니다.</div>' : list.map(renderAdminTicketCard).join('')}
  </div>`;
}
