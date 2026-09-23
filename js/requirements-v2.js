// requirements-v2.js — 2026-09-16 화면/정책 개편.
// 서버 응답이 있는 값은 서버 값을 우선하고, 아직 없는 값은 MOCK 표시된 데이터로 동작한다.
// [담당] 특정 카테고리 없음 — 다른 파일에 이미 정의된 함수를 나중에 덮어쓰는 "몽키패치" 모음.
// [백엔드 연동] 덮어쓰는 원본 함수가 뭘 호출하는지에 따라 다르다(고정된 연동 지점 없음).
// [주의] ⚠️⚠️ index.html에서 반드시 여기서 덮어쓰는 원본 파일들(main.js/crew.js 등)보다
//        나중에 로드돼야 한다. `const _fooV1 = foo; foo = function(){ ...; return _fooV1(); }`
//        패턴으로 foo를 통째로 교체하므로, 원본 foo(예: renderMain, renderCrew)를 고칠 때
//        여기서 같은 이름을 덮어쓰고 있는지 꼭 같이 확인할 것 — 안 그러면 원본 수정이 무시된다.

// 등급 이름·색·배지는 실제 서버 값(state.user.grade/gradeName)과 utils.js의
// USER_GRADE_COLORS/NAMES/rankBadgeIcon을 쓴다 — 여기 있던 gradeIndex 기반 목데이터는 제거함.
const SCORE_EXP_TABLE = [[249,0],[499,50],[749,150],[899,250],[1049,300],[1199,350],[1349,400],[1499,450],[1500,500]];
function mockExpForScore(score){ return (SCORE_EXP_TABLE.find(([max])=>score<=max)||[1500,500])[1]; }
function calculatedNextLevelExp(level){
  const safeLevel=Math.max(1,Number(level)||1);
  if(safeLevel<=10)return 100;
  return 1000+Math.floor((safeLevel-1)/10)*250;
}
function nextLevelExp(){ return Number(state.user.nextLevelExp ?? calculatedNextLevelExp(state.user.level)); }
function currentExpValue(){ return Number(state.user.currentExp ?? state.user.exp ?? 0); }
function levelProgressPct(){ return Math.min(100,Math.round(currentExpValue()/Math.max(1,nextLevelExp())*100)); }

Object.assign(state.user, {
  currentExp: Number.isFinite(state.user.currentExp) ? state.user.currentExp : state.user.exp,
  nextLevelExp: state.user.nextLevelExp ?? calculatedNextLevelExp(state.user.level),
  freeWorkoutsUsed: state.user.freeWorkoutsUsed || 0,
  rankedWorkoutsUsed: state.user.rankedWorkoutsUsed || 0,
  freeWorkoutDate: state.user.freeWorkoutDate || '',
  attendanceRewardClaimed: !!state.user.attendanceRewardClaimed,
});
state.crew.joinEnabled = state.crew.joinEnabled !== false;
state.crew.autoApprove = state.crew.autoApprove === true;
state.crew.maxMembers = 5;
state.crew.battleHistory = state.crew.battleHistory || [];
state.crew.battleRequest = state.crew.battleRequest || {size:2,status:'idle'};
function kstDateKey(){ return new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Seoul',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date()); }
function syncDailyFreeWorkouts(){
  const today=kstDateKey();
  if(state.user.freeWorkoutDate!==today){ state.user.freeWorkoutDate=today; state.user.freeWorkoutsUsed=0; state.user.rankedWorkoutsUsed=0; state.user.setsUsedToday=0; }
  else state.user.setsUsedToday=state.user.freeWorkoutsUsed;
}
syncDailyFreeWorkouts();
let dailyFreeWorkoutResetTimer=null;
function scheduleDailyFreeWorkoutReset(){
  if(dailyFreeWorkoutResetTimer) clearTimeout(dailyFreeWorkoutResetTimer);
  const today=kstDateKey();
  const nextUtc=Date.parse(`${today}T15:00:00Z`) + (Date.now() >= Date.parse(`${today}T15:00:00Z`) ? 86400000 : 0);
  dailyFreeWorkoutResetTimer=setTimeout(()=>{ syncDailyFreeWorkouts(); if(typeof render==='function') render(); scheduleDailyFreeWorkoutReset(); }, Math.max(1000,nextUtc-Date.now()));
}
scheduleDailyFreeWorkoutReset();

// 출석 표시/보상 상태를 대한민국 시간(KST) 기준으로 동기화한다.
// 마지막으로 실제 출석 보상을 받은 날짜만 로컬에 기록하며, 서버가 제공하는 streak 값은 우선 유지한다.
function syncAttendanceStatus(){
  if(state.guestMode) return;
  const today=kstDateKey();
  const uid=state.user.id || state.user.nickname || 'local';
  const key=`ounhome_attendance_${uid}`;
  let saved=null;
  try{ saved=JSON.parse(localStorage.getItem(key)||'null'); }catch(_e){}
  if(saved && saved.date){
    const [y,m,d]=saved.date.split('-').map(Number);
    const [ty,tm,td]=today.split('-').map(Number);
    const gap=Math.round((Date.UTC(ty,tm-1,td)-Date.UTC(y,m-1,d))/86400000);
    if(gap>1){ state.user.streak=1; setMilestoneState({m3:false,m10:false}); }
    state.user.attendanceRewardClaimed = saved.date===today ? !!saved.claimed : false;
  }else{
    state.user.attendanceRewardClaimed=false;
  }
}
syncAttendanceStatus();

function getStoredAttendanceDates(){
  if(state.guestMode) return new Set();
  const uid=state.user.id || state.user.nickname || 'local';
  try{
    const arr=JSON.parse(localStorage.getItem(`ounhome_attendance_dates_${uid}`)||'[]');
    return new Set(Array.isArray(arr)?arr.filter(x=>/^\\d{4}-\\d{2}-\\d{2}$/.test(x)):[]);
  }catch(_e){ return new Set(); }
}
function storeAttendanceDate(date){
  if(state.guestMode) return;
  const uid=state.user.id || state.user.nickname || 'local';
  const dates=getStoredAttendanceDates(); dates.add(date);
  try{ localStorage.setItem(`ounhome_attendance_dates_${uid}`,JSON.stringify(Array.from(dates).sort())); }catch(_e){}
}
// 3일/10일 연속출석 보너스(각 +20P)가 지급된 날짜 — 출석 캘린더에 별표(★)로 표시한다.
function getStoredBonusDates(){
  if(state.guestMode) return new Set();
  const uid=state.user.id || state.user.nickname || 'local';
  try{
    const arr=JSON.parse(localStorage.getItem(`ounhome_attendance_bonus_dates_${uid}`)||'[]');
    return new Set(Array.isArray(arr)?arr:[]);
  }catch(_e){ return new Set(); }
}
function storeBonusDate(date){
  if(state.guestMode) return;
  const uid=state.user.id || state.user.nickname || 'local';
  const dates=getStoredBonusDates(); dates.add(date);
  try{ localStorage.setItem(`ounhome_attendance_bonus_dates_${uid}`,JSON.stringify(Array.from(dates).sort())); }catch(_e){}
}
// 지금 이어지고 있는 연속출석 구간에서 3일/10일 마일스톤을 이미 받았는지 — 연속출석이
// 끊기면(gap>1, syncAttendanceStatus 참고) 다시 받을 수 있게 초기화된다.
function getMilestoneState(){
  if(state.guestMode) return {m3:false,m10:false};
  const uid=state.user.id || state.user.nickname || 'local';
  try{ return JSON.parse(localStorage.getItem(`ounhome_attendance_milestones_${uid}`)||'null') || {m3:false,m10:false}; }
  catch(_e){ return {m3:false,m10:false}; }
}
function setMilestoneState(m){
  if(state.guestMode) return;
  const uid=state.user.id || state.user.nickname || 'local';
  try{ localStorage.setItem(`ounhome_attendance_milestones_${uid}`,JSON.stringify(m)); }catch(_e){}
}
function calculateAttendanceStreak(){
  if(state.guestMode) return 0;
  const dates=getStoredAttendanceDates(), today=kstDateKey();
  if(dates.size){
    let cursor=today,count=0;
    while(dates.has(cursor)){
      count++;
      const d=new Date(cursor+'T00:00:00+09:00'); d.setDate(d.getDate()-1);
      cursor=d.toISOString().slice(0,10);
    }
    return count;
  }
  return Math.max(0,Number(state.user.streak)||0);
}
renderAttendanceCalendar=function(){
  const DOW=['월','화','수','목','금','토','일'];
  const now=new Date(),year=now.getFullYear(),month=now.getMonth(),todayDate=now.getDate();
  const stored=getStoredAttendanceDates(),bonus=getStoredBonusDates(),fallbackStreak=calculateAttendanceStreak();
  const firstDow=(new Date(year,month,1).getDay()+6)%7,daysInMonth=new Date(year,month+1,0).getDate();
  const cells=[]; for(let i=0;i<firstDow;i++)cells.push(null); for(let d=1;d<=daysInMonth;d++)cells.push(d);
  while(cells.length%7!==0)cells.push(null);
  const dowRow=DOW.map(l=>`<div class="att-dow-h">${l}</div>`).join('');
  const dayCells=cells.map(d=>{
    if(d==null)return '<div class="att-day empty"></div>';
    const iso=`${year}-${String(month+1).padStart(2,'0')}-${String(d).padStart(2,'0')}`;
    const isToday=d===todayDate,isStored=stored.has(iso),isFallback=!stored.size&&d<=todayDate&&(todayDate-d)<fallbackStreak,checked=isStored||isFallback;
    const isBonus=bonus.has(iso);
    const cls=isToday?`att-day today${checked?' checked':''}`:checked?'att-day checked':'att-day';
    // 별표(★)는 그날 3일/10일 연속출석 보너스(+20P)가 지급됐다는 표시 — 날짜 숫자 뒤에 작게 붙인다.
    return `<div class="${cls}" aria-label="${iso}${checked?' 출석':''}${isBonus?' 보너스지급':''}"><span class="att-day-number">${d}${isBonus?'<span class="att-day-star">★</span>':''}</span>${checked?'<span class="att-check-ring" aria-hidden="true"></span>':''}</div>`;
  }).join('');
  return `<div style="margin:10px 0;"><p class="hint mono" style="margin:0 0 8px;font-weight:700;color:var(--ink);">${year}년 ${month+1}월</p><div class="att-month-grid">${dowRow}${dayCells}</div></div>`;
};

function renderUserProgressSummary(){
  const grade=state.user.grade||'IRON';
  const gradeName=state.user.gradeName||USER_GRADE_NAMES[grade]||'아이언';
  const color=userGradeColor(grade);
  const total=(typeof getProfileStats==='function') ? getProfileStats().total : 0;
  const expToNext=Math.max(0, nextLevelExp()-currentExpValue());
  // 예전엔 이 자리에 "EXP·P·성별" 한 줄만 있었는데, 메인 대시보드의 누적성과 카드를 없애고
  // 그 내용(누적 점수·레벨업까지 남은 EXP)을 여기로 옮겼다 — 레벨업 바는 그대로 위에 있는 걸 쓴다.
  return `<div class="user-progress-summary">
    ${rankBadgeIcon(grade, gradeName, 32)}
    <div><b>${gradeName} · Lv.${Math.min(500,state.user.level)}</b><div class="progress"><span style="width:${levelProgressPct()}%;background:${color};"></span></div>
    <small>누적 점수 ${total.toLocaleString()}점 · 레벨업까지 ${expToNext.toLocaleString()} EXP 남았어요</small></div>
  </div>`;
}

// 2~3. 캘리브레이션은 신체정보 입력과 무관하며, 서버 calibration 여부만 사용한다.
goToTutorial = function(){
  syncDailyFreeWorkouts();
  if(state.guestMode){ goExStep(1); return; }
  // 신체정보 입력 여부가 아니라 서버가 내려준 캘리브레이션 완료 여부만 본다.
  if(state.user.calibrated === false){
    toast('운동 전에 카메라 캘리브레이션을 진행해 주세요. 키와 몸무게는 입력하지 않아도 됩니다.');
    openCalibrationModal();
    return;
  }
  const ranked = state.exercise.mode==='ranked';
  // 순위 도전은 무료 운동 횟수와 완전히 별개로, 보유한 '순위 도전 티켓'만 있으면 참여할 수
  // 있다 — 포인트/경험치는 지급하지 않고 점수만 랭킹에 반영된다(renderExStepSave 참고).
  if(ranked && (state.user.rankTickets||0)<=0){
    toast('순위 도전 티켓이 없습니다. 티켓을 구매해주세요.');
    return;
  }
  // 하루 운동 횟수 자체엔 제한이 없다 — 대신 자유 운동은 오늘 몇 번째인지에 따라 보상
  // 등급이 정해진다(1~5회 free, 6회부터 reduced). 부상 위험 경고는 모드와 무관하게
  // 자유 운동+순위 도전을 합친 "오늘 총 횟수"를 기준으로 뜬다.
  const usedFree = state.user.freeWorkoutsUsed || 0;
  const totalToday = usedFree + (state.user.rankedWorkoutsUsed || 0);
  const start = () => {
    state.exercise.sessionType = ranked ? 'ranked' : (usedFree>=5 ? 'reduced' : 'free');
    goExStep(1);
  };
  if(totalToday>=10){
    askConfirm(
      '부상 위험 안내',
      `오늘 벌써 ${totalToday}회 운동했어요. 무리하게 반복 운동을 하면 부상 위험이 높아질 수 있으니, 충분히 쉬고 다음에 다시 시도하는 걸 권장해요.\n그래도 계속 진행할까요?`,
      ()=>{ closeConfirm(); start(); },
      '그래도 진행할게요',
      true
    );
    return;
  }
  start();
};
const _renderTutorialV1=renderExStepTutorial;
renderExStepTutorial=()=>_renderTutorialV1().replaceAll('스마트폰 카메라 촬영','스마트폰 카메라 촬영').replaceAll('스마트폰 카메라','스마트폰 카메라');
startTutorialGate=function(){
  let left=TUTORIAL_GATE_SECONDS; const tick=()=>{const b=document.getElementById('ex-tutorial-start-btn');if(!b)return;b.textContent=left>0?`스마트폰 카메라 촬영 시작 (${left}초)`:'스마트폰 카메라 촬영 시작';b.disabled=left>0;b.style.opacity=left>0?'.5':'1';if(left-->0)setTimeout(tick,1000);};tick();
};

// 4, 7, 8. 1세트/실시간 점수/오늘 운동 횟수 표시
// 하루 운동 횟수 제한은 없앴다 — 대신 오늘 몇 번째 운동인지에 따라 보상이 달라진다
// (1~5회 전액 지급, 6회부터 포인트 미지급·경험치 1/3만 지급, 10회부터 부상 위험 경고를
// 띄운다). goToTutorial()이 이 등급을 정해 state.exercise.sessionType에 담아둔다.
const _renderExStepPickV1=renderExStepPick;
renderExStepPick=function(){
  syncDailyFreeWorkouts();
  const used=Number(state.user.freeWorkoutsUsed)||0;
  let html=_renderExStepPickV1()
    .replace(/<p class="hint" style="margin-top:6px;">[\s\S]*?<\/p>/,'');
  if(state.exercise.mode==='ranked'){
    // 순위 도전은 오늘 운동 횟수가 아니라 포인트 상점의 '순위 도전 티켓'(state.user.rankTickets)을
    // 소모해서 참여한다. 카드 오른쪽의 티켓 이미지를 누르면 바로 구매 확인창이 뜬다
    // (openRankTicketPurchase 참고) — shop.js buyItem()과 별개로, 여기서는 서버 카탈로그
    // 동기화(serverId) 없이도 바로 구매되는 간편 구매 경로다.
    const rankTickets=state.user.rankTickets||0;
    html=html.replace(
      /<div class="card ex-daily-card" style="margin-bottom:16px;">[\s\S]*?<\/div>/,
      `<div class="card ex-daily-card" style="margin-bottom:16px;display:flex;align-items:center;justify-content:space-between;gap:14px;">
        <div>
          <p class="section-label ex-mission-title" style="margin:0 0 4px;">순위 도전 티켓</p>
          <p class="desc mono" style="margin:0;">보유 수량 <b>${rankTickets}장</b></p>
        </div>
        <button type="button" onclick="openRankTicketPurchase()" title="순위 도전 티켓 구매하기" style="background:none;border:none;padding:0;cursor:pointer;flex:none;display:flex;flex-direction:column;align-items:center;gap:4px;">
          <img src="assets/shop-icons/rank-challenge-ticket.svg" alt="순위 도전 티켓 구매하기" style="width:76px;height:auto;border-radius:10px;display:block;">
          <span class="mono" style="font-size:11px;font-weight:700;color:var(--ink);">구매</span>
        </button>
      </div>`
    );
  } else {
    const tierText = used<5
      ? `오늘 ${used}회 완료 · 5회까지 포인트·경험치 전액 지급`
      : used<10
        ? `오늘 ${used}회 완료 · 6회부터 포인트 미지급, 경험치는 1/3만 지급돼요`
        : `오늘 ${used}회 완료 · 과도한 운동은 부상 위험이 있어요`;
    html=html
      .replace(/<p class="desc mono" style="margin:0;">[\s\S]*?<\/p>/,`<p class="desc mono" style="margin:0;">${tierText}</p>`);
  }
  return html;
};
// 순위 도전 카드의 티켓 이미지 버튼 — 포인트 상점까지 안 가고 바로 구매할 수 있는 간편 구매
// 흐름. shop.js buyItem()은 서버 카탈로그(serverId)와 동기화된 아이템만 구매되는데, 이
// 티켓은 아직 백엔드에 없어서 여기서는 그 제약 없이 포인트만 확인하고 바로 지급한다.
function openRankTicketPurchase(){
  const it=state.shopItems.find(x=>x.name==='순위 도전 티켓');
  if(!it) return;
  if(state.user.points<it.price){ toast('포인트가 부족합니다'); return; }
  askConfirm(
    '순위 도전 티켓 구매',
    `보유 포인트: ${state.user.points.toLocaleString()}P\n티켓 가격: ${it.price.toLocaleString()}P\n구매 후 남는 포인트: ${(state.user.points-it.price).toLocaleString()}P`,
    ()=>{
      state.user.points-=it.price;
      state.user.rankTickets=(state.user.rankTickets||0)+1;
      toast(`${it.name} 구매 완료 (보유 ${state.user.rankTickets}장)`);
      closeConfirm();
    },
    '구매하기'
  );
}
const _renderExStepCamV1=renderExStepCam;
renderExStepCam=function(){
  const html=_renderExStepCamV1();
  const live=`<div class="exercise-session-strip"><b>1세트 · 목표 15회</b><span>남은 시간 <strong id="exercise-time-left">02:00</strong></span><span>현재 점수 <strong id="live-score">0</strong></span></div>`;
  return live+html.replace('추정 정확도','누적 판정 점수').replace('촬영 시작','1세트 시작');
};
const _exRegisterRepV1=exRegisterRep;
exRegisterRep=function(...args){ const out=_exRegisterRepV1(...args); const score=(state.exercise.liveReps||[]).reduce((s,r)=>s+({PERFECT:100,GREAT:80,GOOD:50,MISS:0}[r.grade]||0),0); const el=document.getElementById('live-score');if(el)el.textContent=score.toLocaleString(); return out; };

// 5~6. 결과는 백엔드 awardedExp/awardedPoints/highScoreUpdated를 우선 표시한다.
renderExStepSave=function(){
  const r=state.exercise.result;if(!r)return '<div class="empty-note">저장할 결과가 없습니다.</div>';
  const counts={PERFECT:0,GREAT:0,GOOD:0,MISS:0};(r.reps||[]).forEach(x=>counts[x.grade]++);
  // sessionType: 'free'(1~5회) | 'reduced'(오늘 6회째부터, 포인트 0·경험치 1/3) |
  // 'ranked'(순위 도전 — 포인트·경험치 항상 0, 점수만 랭킹에 반영). goToTutorial() 참고.
  const type=state.exercise.sessionType;
  const ranked=type==='ranked';
  const reduced=type==='reduced';
  const serverExp=r.expAwarded!=null||r.experienceAwarded!=null;
  const mockExp=mockExpForScore(r.score);
  const exp=ranked?0:Number(r.expAwarded ?? r.experienceAwarded ?? (reduced?Math.round(mockExp/3):mockExp));
  const points=ranked?0:(r.pointsAwarded!=null ? Number(r.pointsAwarded) : (reduced?0:Math.round(r.score*.4)));
  const pillLabel=ranked?'순위 도전':reduced?'6회차 이후 운동':'무료 운동';
  const warning=ranked
    ? '<p class="ticket-warning">순위 도전은 포인트·경험치가 지급되지 않고, 점수만 랭킹에 반영됩니다.</p>'
    : reduced
      ? '<p class="ticket-warning">오늘 6회 이후 운동이라 포인트는 지급되지 않고, 경험치는 1/3만 지급됩니다.</p>'
      : '';
  return `<div class="card result-summary"><div class="flex-between"><h2>운동 결과</h2><span class="pill ${ranked||reduced?'pill-gold':'pill-accent'}">${pillLabel}</span></div>
    ${warning}
    <div class="stat-row"><div class="stat-box"><div class="num mono">${r.total}</div><div class="lbl">전체 횟수</div></div>${Object.entries(counts).map(([k,v])=>`<div class="stat-box"><div class="num mono">${v}</div><div class="lbl">${k}</div></div>`).join('')}</div>
    <div class="stat-row"><div class="stat-box"><div class="num mono">${r.score.toLocaleString()}</div><div class="lbl">총점</div></div><div class="stat-box"><div class="num mono">+${exp}</div><div class="lbl">획득 경험치${!serverExp&&!ranked?' (목)':''}</div></div><div class="stat-box"><div class="num mono">+${points}</div><div class="lbl">획득 포인트${r.pointsAwarded==null&&!ranked?' (목)':''}</div></div></div>
    <p class="${r.highScoreUpdated?'record-new':'hint'}">${r.highScoreUpdated?'🏆 최고점을 갱신했습니다!':'기존 최고점과 비교 후 갱신 여부가 표시됩니다.'}</p>
    <button class="btn btn-primary btn-block" onclick="saveExerciseResult()">결과 저장</button></div>`;
};

// 9~11. 미션/레벨/출석
if(!state.missions.today.length) state.missions.today=[
  {id:'mock-1',label:'운동 1세트 완료',target:1,current:0,reward:50,expReward:50,achieved:false,claimed:false,mock:true},
  {id:'mock-2',label:'MISS 3회 미만 달성',target:1,current:0,reward:50,expReward:50,achieved:false,claimed:false,mock:true},
  {id:'mock-3',label:'GOOD 이상 10회 달성',target:10,current:0,reward:50,expReward:50,achieved:false,claimed:false,mock:true},
];
const _renderMissionCardV1=renderMissionCard;
renderMissionCard=m=>_renderMissionCardV1(m).replace(/\+[^<\s]+P\s*<\/span>/, '+50P / +50 EXP</span>').replace('일간','개인 일일');
// 로그인/세션복원 시 자동으로 호출된다(버튼 없음, bootstrap.js·auth.js 참고) — 출석 자체는
// 매일 그냥 기록만 되고, 포인트는 3일/10일 연속출석을 "처음" 달성한 날에만 지급된다(3일 +20P, 10일 +300P).
// 같은 연속출석 구간 안에서는 한 번만 지급되고(getMilestoneState), 끊기면 다시 받을 수 있다.
function autoClaimAttendance(){
  if(state.guestMode) return;
  syncAttendanceStatus();
  if(state.user.attendanceRewardClaimed) return;
  const today=kstDateKey();
  storeAttendanceDate(today);
  const streak=Math.max(1,calculateAttendanceStreak());
  state.user.streak=streak;
  const milestones=getMilestoneState();
  let reward=0;
  if(streak>=3 && !milestones.m3){ reward+=20; milestones.m3=true; }
  if(streak>=10 && !milestones.m10){ reward+=300; milestones.m10=true; }
  if(reward>0){
    state.user.points+=reward;
    storeBonusDate(today);
    setMilestoneState(milestones);
    toast(`연속 출석 ${streak}일차 · 보너스 +${reward}P`);
  }
  state.user.attendanceRewardClaimed=true;
  const uid=state.user.id || state.user.nickname || 'local';
  try{localStorage.setItem(`ounhome_attendance_${uid}`,JSON.stringify({date:today,claimed:true}));}catch(_e){}
  render();
}

// 12~19. 크루 정책/주간미션/관리/자동매칭
renderCrewNoticeCard=()=>'';
const _renderJoinButtonV1=renderJoinButton;
renderJoinButton=function(c){
  const current=Number(c.currentMembers??c.memberCount??4),max=Number(c.maxMembers??5),enabled=c.joinEnabled!==false&&c.recruiting!==false&&current<max;
  return `<p class="hint">현재 인원 <b>${current}/${max}</b></p>`+(enabled?_renderJoinButtonV1(c):'<button class="btn btn-ghost btn-block" disabled>가입 불가</button>');
};
async function toggleCrewAutoApprove(){
  if(getMyCrewRole()!=='팀장'){toast('크루장만 자동가입승인 상태를 변경할 수 있습니다');return;}
  const previous=state.crew.autoApprove===true;
  const next=!previous;
  try{
    const res=await fetch(`${API_BASE}/api/crews/me/join-setting`,{
      method:'PATCH',
      headers:{'Content-Type':'application/json','Authorization':'Bearer '+state.token},
      body:JSON.stringify({autoApprove:next})
    });
    const body=await res.json();
    if(!body.success){toast(body.message||'자동가입승인 상태 변경에 실패했습니다');return;}
    state.crew.autoApprove=body.data && typeof body.data.autoApprove==='boolean' ? body.data.autoApprove : next;
    toast(`자동가입승인을 ${state.crew.autoApprove?'켰습니다':'껐습니다'}`);
    render();
  }catch(err){
    state.crew.autoApprove=previous;
    toast('서버에 연결할 수 없습니다');
  }
}
function isCurrentCrewMember(m){return Number(m.userId)===Number(state.user.id)||m.n==='나';}
// v1은 서버 호출 없이 로컬 state.crew.members만 바꿔서, 양도한 나한테만(그것도 새로고침
// 전까지만) 반영된 것처럼 보였다 — 새 크루장 화면엔 전혀 안 알려지고, 다음에 loadMyCrew()가
// 서버의 예전 크루장 값으로 이 로컬 변경을 덮어써서 "일정 시간 뒤 원상복구"된 것처럼 보였다.
// 백엔드에 이미 구현되어 있는 PATCH /api/crews/me/leader/{targetUserId}
// (CrewService.transferLeadership, LEADER_CHANGED 브로드캐스트까지 완비)를 그대로 호출한다.
function transferCrewLeader(userId){
  const m=state.crew.members.find(x=>Number(x.userId)===Number(userId));
  if(!m||isCurrentCrewMember(m)){toast('본인에게는 크루장을 양도할 수 없습니다');return;}
  askConfirm('크루장 양도',`${m.n}님에게 크루장을 양도하시겠습니까?`,async ()=>{
    closeConfirm();
    try{
      const res=await fetch(`${API_BASE}/api/crews/me/leader/${userId}`,{method:'PATCH',headers:{'Authorization':'Bearer '+state.token}});
      const body=await res.json();
      if(!body.success){ toast(body.message||'크루장 양도에 실패했습니다'); return; }
      toast('크루장을 양도했습니다');
      // 서버가 /topic/crews/{id}/members로 브로드캐스트하면 handleCrewMemberEvent()가 새
      // 크루장을 포함한 전원의 화면을 갱신해준다 — 그 브로드캐스트를 놓쳤을 때를 대비해
      // 내 화면만은 여기서도 한 번 더 확실히 최신화해둔다.
      await loadMyCrew();
      render();
    }catch(err){
      toast('서버에 연결할 수 없습니다');
    }
  },'양도');
}
function leaveOrDisbandCrew(){
  const leader=getMyCrewRole()==='팀장';
  const others=state.crew.members.filter(m=>!isCurrentCrewMember(m));
  if(!leader){ confirmCrewLeave(); return; }
  if(others.length){toast('다른 크루원이 있어 크루를 해체할 수 없습니다');return;}
  askConfirm(
    '크루 해체',
    '크루 정보 및 크루 활동 내역은 더 이상 이용할 수 없습니다.\n\n정말 크루를 해체하시겠습니까?',
    ()=>disbandCrew(),
    '크루 해체',
    true
  );
}
async function disbandCrew(){
  if(getMyCrewRole()!=='팀장'){toast('크루장만 크루를 해체할 수 있습니다');return;}
  try{
    const res=await fetch(`${API_BASE}/api/crews/me`,{method:'DELETE',headers:{'Authorization':'Bearer '+state.token}});
    const body=await res.json();
    if(!body.success){toast(body.message||'크루 해체에 실패했습니다');return;}
    state.crew.created=false;
    state.crew.id=null;
    state.crew.name=''; state.crew.desc=''; state.crew.region=''; state.crew.leaderRegion=''; state.crew.concepts=[]; state.crew.members=[];
    state.crew.joinRequests=[];
    state.subtabs.crew=0;
    closeConfirm();
    disconnectCrewChat(); disconnectJoinWait();
    toast('크루가 해체되었습니다');
    await loadJoinableCrews();
    render();
  }catch(err){toast('서버에 연결할 수 없습니다');}
}
function renderCrewManagementV2(){
  const leader=getMyCrewRole()==='팀장',members=state.crew.members,others=members.filter(m=>!isCurrentCrewMember(m)&&m.role!=='팀장'),requests=state.crew.joinRequests||[];
  return `<div class="card"><div class="flex-between"><h3>크루 관리</h3>${leader?`<div class="crew-join-toggle"><span class="crew-join-toggle-label">자동가입승인</span><button type="button" class="switch ${state.crew.autoApprove?'on':''}" role="switch" aria-checked="${state.crew.autoApprove?'true':'false'}" aria-label="자동가입승인 ${state.crew.autoApprove?'ON':'OFF'}" onclick="toggleCrewAutoApprove()"><span class="switch-state">${state.crew.autoApprove?'ON':'OFF'}</span><span class="knob"></span></button></div>`:''}</div>
  ${leader?`<p class="section-label" style="margin-top:16px;">가입 신청 대기 (${requests.length})</p><div class="grid grid-2">${requests.length?requests.map(r=>`<div class="card"><div class="flex-between"><span style="display:flex;align-items:center;gap:6px;"><b>${r.n}</b>${r.userId!=null?`<button type="button" class="btn-icon-plain" onclick="openPublicProfile(${r.userId})" aria-label="${r.n} 정보 보기" title="정보 보기">🔍</button>`:''}</span><span class="pill pill-gold">보류중</span></div><p class="hint">Lv.${r.level}${r.requestedAt?` · ${new Date(r.requestedAt).toLocaleString('ko-KR')}`:''}</p><p class="desc">${r.msg||'가입 메시지가 없습니다.'}</p><div class="crew-join-actions"><button class="btn btn-sm btn-secondary" onclick="rejectJoinRequest(${r.id})" aria-label="${r.n} 가입 요청 거절">거절</button><button class="btn btn-sm btn-primary" onclick="approveJoinRequest(${r.id})" aria-label="${r.n} 가입 요청 승인">승인</button></div></div>`).join(''):'<div class="empty-note" style="grid-column:1/-1;">대기 중인 가입 신청이 없습니다.</div>'}</div>`:''}
  <p class="hint">최대 인원 5명 · 현재 ${members.length}/5명</p>${leader?`<div class="table-wrap"><table><thead><tr><th>크루원</th><th>역할</th><th>레벨</th><th>관리</th></tr></thead><tbody>${members.map(m=>`<tr><td>${m.n}${isCurrentCrewMember(m)?' <span class="pill pill-accent">나</span>':''}${m.role==='팀장'?' <span class="pill pill-gold">크루장</span>':''}</td><td>${crewRoleLabel(m.role)}</td><td>Lv.${m.level}</td><td>${!isCurrentCrewMember(m)&&m.role!=='팀장'?`<button class="btn btn-sm btn-secondary" onclick="transferCrewLeader(${m.userId||0})">크루장 양도</button> <button class="btn btn-sm btn-danger" onclick="kickMember(${m.userId})">강퇴</button>`:'<span class="hint">관리 제외</span>'}</td></tr>`).join('')}</tbody></table></div>`:''}
  <div class="crew-management-danger-zone"><button class="btn btn-danger" ${leader&&others.length?'disabled style="opacity:.5"':''} onclick="leaveOrDisbandCrew()">크루 해체</button>${leader&&others.length?'<p class="hint">다른 크루원이 있는 동안 해체할 수 없습니다.</p>':''}</div></div>`;
}
const _renderCrewBattleV1=renderCrewBattle;
renderCrewBattle=function(){
  const b=state.crewBattle;if(!b)return renderCrewBattleMenu();
  const our=(b.myScore||0)+(b.teammates||[]).reduce((s,x)=>s+(x.score||0),0),opp=b.oppScore||0;
  return `<div class="card crew-battle-live-head"><div class="flex-between"><div><p class="section-label">${state.crew.name}</p><b class="mono" style="font-size:34px;">${our.toLocaleString()}</b><small> · 누적 운동 ${(b.myGradeCounts?Object.values(b.myGradeCounts).reduce((a,c)=>a+c,0):0)}회</small></div><div><b>제한시간</b><div class="mono" style="font-size:28px;">02:00</div></div><div style="text-align:right;"><p class="section-label">${b.oppName||'상대 크루'}</p><b class="mono" style="font-size:34px;">${opp.toLocaleString()}</b><small> · 누적 운동 ${b.oppReps||0}회</small></div></div><p class="hint">화면 하단 참가자별 운동 횟수와 점수 · 종료 후 대전 결과로 자동 이동</p></div>`+_renderCrewBattleV1();
};
// 예전엔 여기서 "크루 주간 미션" 카드를 하나 더 붙였는데, state.crew.weeklyMission 자체가
// 목데이터(진행도 120/300 고정값, 크루원별 40~80회 가짜 범위)였고 실제 크루원 닉네임과도
// 안 맞아서 누가 뭘 했든 상관없이 "40회"만 떴다 — 진짜 크루 주간 미션은 이미 위(원본
// renderCrewOverview)의 "개인운동에서 OO 하기" 카드가 서버 값(groupMission)으로 보여주고
// 있으므로, 라벨만 "일일"→"주간"으로 맞추고 가짜 카드는 제거한다.
const _renderCrewOverviewV1=renderCrewOverview;
renderCrewOverview=function(){return _renderCrewOverviewV1().replace('크루 미션 누적점수','크루대전 기여도').replace('오늘의 크루미션','크루 주간 미션').replace('일일','주간');};
// getCrewPageTabs()를 크루대전 탭이 포함된 순서로 바꾼다 — router.js의 setSub()/render() 훅이
// 전부 이 함수로 "지금 몇 번째 탭이 무슨 탭인지" 이름으로 찾으므로, renderCrew도 아래에서
// 직접 배열을 다시 만들지 않고 이 함수를 그대로 쓴다(하드코딩된 배열이 두 군데서 따로
// 놀면 나중에 또 어긋난다 — setSub의 '크루채팅' 인덱스가 이 배열 변경 전 기준(1번)으로 굳어있던
// 게 바로 그 사례라 router.js도 같이 고쳤다).
getCrewPageTabs=function(){
  const tabs=['크루 메인','크루대전','크루채팅','크루원 정보'];
  tabs.push(getMyCrewRole()==='팀장' ? '크루관리' : '크루탈퇴');
  return tabs;
};
const _renderCrewV1=renderCrew;
renderCrew=function(){
  if(!state.crew.created)return _renderCrewV1();
  const leader=getMyCrewRole()==='팀장';
  const tabs=getCrewPageTabs();
  const i=Math.min(state.subtabs.crew,tabs.length-1),t=tabs[i];
  return `<div class="view-head"><h1>${state.crew.name}</h1></div><div class="subtabs subtabs-compact">${tabs.map((x,n)=>`<div class="tab ${n===i?'active':''}" onclick="setSub('crew',${n})">${x}</div>`).join('')}</div>${t==='크루 메인'?renderCrewOverview():t==='크루채팅'?renderCrewChat():t==='크루원 정보'?renderCrewMembers():t==='크루대전'?renderCrewBattleMenu():leader?renderCrewManagementV2():renderCrewLeave()}`;
};

// 20. 화면 설명과 클라이언트 목 정렬. 서버는 rank 값을 확정해 내려주는 것이 원칙이다.
const _renderRankingV1=renderRanking;
renderRanking=function(){state.rank.crew.sort((a,b)=>(b.level-a.level)-0||(b.currentExp||b.exp||0)-(a.currentExp||a.exp||0)||String(a.reachedAt||'').localeCompare(String(b.reachedAt||'')));return _renderRankingV1();};

// 공통 화면에 사용자 진행 요약을 추가한다.
const _renderMainV1=renderMain;
renderMain=function(){
  const html=_renderMainV1();
  if(state.guestMode)return html;
  const anchor='<p class="hint" style="margin:4px 0 0;font-size:13px;">Lv.'+state.user.level+' · 꾸준함을 키우는 중</p>';
  return html.replace(anchor,anchor+renderUserProgressSummary());
};
