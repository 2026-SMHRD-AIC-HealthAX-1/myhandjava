// main.js — 로그인 후 첫 화면인 '메인' 카테고리(대시보드).

// 게스트 모드에서 "테스트 로그인한 회원과 화면이 똑같아 보인다"는 피드백에 따라, 개인 기록이
// 담긴 영역(메인 대시보드·프로필)은 실제 값을 그대로 보여주는 대신 흐리게 처리하고 로그인
// 유도 오버레이를 덮는다. 로그인 사용자는 innerHtml을 그대로 반환해 아무 영향이 없다.
function renderGuestBlur(innerHtml, message){
  if(!state.guestMode) return innerHtml;
  return `
  <div style="position:relative;">
    <div style="filter:blur(6px);opacity:.5;pointer-events:none;user-select:none;" aria-hidden="true">${innerHtml}</div>
    <div style="position:absolute;inset:0;display:flex;flex-direction:column;align-items:center;justify-content:flex-start;gap:12px;text-align:center;padding:48px 24px 24px;">
      <div style="font-size:32px;">🔒</div>
      <p style="font-weight:700;font-size:15px;color:var(--ink);margin:0;max-width:32ch;">${message}</p>
      <button class="btn btn-primary" onclick="goto('login')">로그인하고 확인하기</button>
    </div>
  </div>`;
}
// 메인 대시보드의 캐릭터 미리보기 — topbar-avatar-canvas/avatar-char-canvas와 같은 방식(drawPixelCharacter)
// 이라 장착한 의상/배경이 그대로 반영된다. 게스트는 장착 아이템이 없으니 기본 외형으로 그린다.
function drawMainCharCanvas(){
  const canvas = document.getElementById('main-char-canvas');
  if(!canvas) return;
  drawPixelCharacter(canvas, state.guestMode ? {} : getEquipState(), state.guestMode ? 'male' : state.user.gender);
}
// 이번 달 전체를 진짜 달력처럼 보여준다. 정확히 어느 날짜에 출석했는지는 서버에 남아있지
// 않고 state.user.streak(연속 일수)만 있으므로, 오늘 이전 최대 streak일만큼을 거슬러 올라가며
// 체크된 것으로 보여주는 근사치다(월 경계를 넘는 연속출석이면 지난달 칸은 표시 못 함) — 실제
// 날짜별 출석 기록을 저장하게 되면 그걸로 교체하면 된다.
function renderAttendanceCalendar(){
  const DOW = ['월','화','수','목','금','토','일'];
  const jsToday = new Date();
  const year = jsToday.getFullYear(), month = jsToday.getMonth(), todayDate = jsToday.getDate();
  const streak = state.guestMode ? 0 : (state.user.streak||0);

  const firstDow = (new Date(year, month, 1).getDay()+6)%7; // 이번 달 1일이 무슨 요일인지(0=월)
  const daysInMonth = new Date(year, month+1, 0).getDate();

  const cells = [];
  for(let i=0;i<firstDow;i++) cells.push(null);
  for(let d=1;d<=daysInMonth;d++) cells.push(d);
  while(cells.length % 7 !== 0) cells.push(null); // 마지막 주도 7칸으로 맞춰서 요일 줄이 안 어긋나게

  const dowRow = DOW.map(l=>`<div class="att-dow-h">${l}</div>`).join('');
  const dayCells = cells.map(d=>{
    if(d==null) return `<div class="att-day empty"></div>`;
    const isToday = d===todayDate;
    const isChecked = !isToday && d<=todayDate && (todayDate-d)<streak;
    const cls = isToday ? 'att-day today' : isChecked ? 'att-day checked' : 'att-day';
    return `<div class="${cls}">${d}</div>`;
  }).join('');

  return `
  <div style="margin:10px 0;">
    <p class="hint mono" style="margin:0 0 8px;font-weight:700;color:var(--ink);">${year}년 ${month+1}월</p>
    <div class="att-month-grid">${dowRow}${dayCells}</div>
  </div>`;
}
function renderMain(){
  const isGuest = state.guestMode;
  // 게스트가 메인 화면에 왔을 때 보여줄 고정 예시값 — 실제 계정 데이터(state.user/state.history)를
  // 그대로 보여주면 이미 로그인한 것처럼 보여서 혼란을 준다는 피드백에 따라, 진짜 내 기록이 아닌
  // "가입하면 이렇게 보여요" 예시로 명확히 구분되는 값을 쓴다. (함수 안에서 만드는 이유는
  // EXP_PER_LEVEL이 이 파일에서 나중에 선언되기 때문 — 최상위 const로 두면 로드 시점에 아직
  // 없는 값을 참조해 에러가 난다.)
  const GUEST_MAIN_SAMPLE = {
    recent: [
      {ex:'스쿼트', date:'예시', reps:24, acc:88, score:320},
      {ex:'런지', date:'예시', reps:15, acc:81, score:210},
    ],
    rank:'-', total:0, expToNext:EXP_PER_LEVEL, exp:0,
    perfectPct:0, greatPct:0, missPct:0,
  };
  const stats = isGuest ? null : getProfileStats();
  const recent = isGuest ? GUEST_MAIN_SAMPLE.recent : state.history.slice(0,3);

  if(!isGuest && typeof syncAttendanceStatus==='function') syncAttendanceStatus();
  const displayStreak = isGuest ? 0 : Math.max(1, Number(state.user.streak)||1);
  const welcomeName = state.guestMode ? '게스트' : (state.user.nickname||'홈트초보');
  // 출석은 로그인하면 자동으로 기록된다(autoClaimAttendance, requirements-v2.js) — 버튼 없음.
  // 포인트는 이 연속출석 구간에서 3일/10일을 "처음" 채운 날에만 각각 지급된다(3일 +20P, 10일 +300P).
  const streakStatusLine = state.guestMode
    ? '로그인하면 자동으로 출석이 기록돼요'
    : (displayStreak>=10
      ? `연속 출석 ${displayStreak}일째 · 10일 연속 보너스 +300P 달성!`
      : displayStreak>=3
        ? `연속 출석 ${displayStreak}일째 · 3일 연속 보너스 +20P 달성`
        : `연속 출석 ${displayStreak}일째`);
  const header = `
  <div class="view-head">
    <h1>${welcomeName}님 환영합니다.</h1><p>오늘도 우리 동네 이웃들과 함께 운동해봐요.</p>
  </div>`;

  const rankLabel = isGuest ? GUEST_MAIN_SAMPLE.rank : (stats.myRank ? `#${stats.myRank}` : '-');
  const perfectPct = isGuest ? GUEST_MAIN_SAMPLE.perfectPct : stats.perfectPct;
  const greatPct = isGuest ? GUEST_MAIN_SAMPLE.greatPct : stats.greatPct;
  const missPct = isGuest ? GUEST_MAIN_SAMPLE.missPct : stats.missPct;
  const regionLabel = isGuest ? '동네를 설정하면 순위가 표시돼요' : state.user.region;

  // 동네랭킹·누적성과·누적등급비율을 캐릭터 카드 안(등급 배지 밑)에 모아 보여준다 — 등급 배지
  // 박스 자체는 requirements-v2.js의 renderUserProgressSummary()가 "Lv.N · 꾸준함을 키우는 중"
  // 바로 뒤에 문자열치환으로 끼워넣으므로, 아래 블록들은 그 뒤에 이어지는 형태로 둔다.
  const rankBlock = `
  <div style="margin-top:12px;padding-top:12px;border-top:1px solid var(--line);text-align:left;">
    <p class="section-label" style="margin:0;">동네 랭킹</p>
    <div style="display:flex;align-items:baseline;gap:10px;margin-top:4px;">
      <span class="mono" style="font-size:32px;font-weight:700;">${rankLabel}</span>
    </div>
    <p class="hint" style="margin:4px 0 0;font-size:12px;">${regionLabel}</p>
  </div>`;
  const gradeRatioBlock = `
  <div style="margin-top:12px;padding-top:12px;border-top:1px solid var(--line);text-align:left;">
    <p class="section-label" style="margin:0 0 6px;">누적 등급 비율 (전체 세션 기준)</p>
    <div style="display:flex;gap:16px;flex-wrap:wrap;font-size:13px;">
      <span>PERFECT <b style="color:var(--accent)">${perfectPct}%</b></span>
      <span>GREAT <b style="color:var(--gold)">${greatPct}%</b></span>
      <span>MISS <b style="color:var(--danger)">${missPct}%</b></span>
    </div>
  </div>`;

  // "한눈에 보기" — 위 줄은 캐릭터(랭킹·누적성과·등급비율 포함)·출석 카드 2개를 넓게 나란히,
  // 아래 줄은 최근 운동 히스토리(좁게)·크루 유도 카드를 나란히 배치한다.
  const glanceRow1 = `
  <div class="grid grid-fixed-2" style="align-items:stretch;">
    <div class="card" style="text-align:center;display:flex;flex-direction:column;">
      <p class="section-label" style="text-align:left;margin:0 0 8px;">내 캐릭터</p>
      <div style="flex:1;display:flex;flex-direction:column;justify-content:center;">
        <canvas id="main-char-canvas" style="width:180px;height:220px;margin:0 auto 12px;display:block;"></canvas>
        <p class="main-nickname-effect" style="font-weight:700;margin:0;font-size:17px;color:${isGuest ? "inherit" : (typeof getNicknameEffectColor==="function" ? getNicknameEffectColor() : "inherit")};">${welcomeName}</p>
        <p class="hint" style="margin:4px 0 0;font-size:13px;">Lv.${isGuest?0:state.user.level} · 꾸준함을 키우는 중</p>
        ${isGuest ? '' : rankBlock + gradeRatioBlock}
      </div>
      <button class="btn btn-secondary btn-sm btn-block" style="margin-top:12px;" onclick="${isGuest ? "goto('login')" : "setMenu('profile');setSub('profile',0);"}">캐릭터 꾸미기</button>
    </div>
    <div class="card" style="display:flex;flex-direction:column;">
      <div>
        <p class="section-label" style="margin:0;">오늘도 출석!</p>
        <p class="hint" style="margin:2px 0 0;">벌써 <b class="mono" style="color:var(--ink);">${displayStreak}</b>일째 함께하고 있어요.</p>
        ${renderAttendanceCalendar()}
      </div>
      <div style="margin-top:8px;padding:8px 10px;border:1px solid var(--line);border-radius:10px;font-size:12px;line-height:1.6;">
        <div><b>3일 연속 달성 시</b> <span class="mono" style="color:var(--gold);">+20P</span></div>
        <div><b>10일 연속 달성 시</b> <span class="mono" style="color:var(--gold);">+300P</span></div>
        <div class="hint" style="margin-top:4px;font-size:11px;">${streakStatusLine}</div>
      </div>
    </div>
  </div>`;
  const glanceRow2 = `
  <div class="grid glance-row-hist" style="align-items:stretch;margin-top:14px;">
    <div class="card">
      <div class="flex-between">
        <p class="section-label" style="margin:0;">최근 운동 히스토리${isGuest?' <span class="hint" style="margin:0;">(예시)</span>':''}</p>
        <button class="btn btn-ghost btn-sm" onclick="${state.guestMode ? "goto('login')" : "setMenu('profile');setSub('profile',2);"}">전체 보기 →</button>
      </div>
      ${recent.length ? `<div style="display:flex;flex-direction:column;gap:8px;margin-top:10px;">${recent.map(h=>`
        <div class="flex-between" style="border:1px solid var(--line);border-radius:10px;padding:10px 12px;">
          <div><b>${h.ex}</b><p class="hint" style="margin:2px 0 0;">${h.date} · 유효 ${h.reps}회 · 정확도 ${h.acc}%</p></div>
          <span class="mono" style="color:var(--gold);font-weight:700;">+${h.score}</span>
        </div>`).join('')}</div>` : '<p class="empty-note" style="margin-top:10px;">아직 운동 기록이 없어요. 운동을 시작해보세요!</p>'}
    </div>
    <div class="card" style="background:var(--ink);border-color:var(--ink);display:flex;flex-direction:column;justify-content:center;">
      <p class="section-label" style="color:var(--gold);margin:0 0 8px;">BETTER TOGETHER</p>
      <h3 style="margin:0 0 6px;color:#fff;font-size:19px;">혼자보다, 함께</h3>
      <p class="desc" style="color:rgba(255,255,255,.72);margin:0 0 12px;font-size:13px;">${state.crew.created ? '오늘도 우리 크루와 함께해요.' : '우리 동네 운동 친구들과 함께해요.'}</p>
      <button class="btn btn-primary btn-sm btn-block" onclick="setMenu('crew')">${state.crew.created ? '우리 크루 페이지 →' : '크루 찾기 →'}</button>
    </div>
  </div>`;
  const highlightBlock = `
  <div>${renderGuestBlur(glanceRow1+glanceRow2, '로그인하면 내 캐릭터·출석·랭킹을 볼 수 있어요')}</div>`;

  return header + highlightBlock;
}
