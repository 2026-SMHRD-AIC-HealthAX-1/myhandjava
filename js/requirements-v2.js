// requirements-v2.js — 2026-09-16 화면/정책 개편.
// 서버 응답이 있는 값은 서버 값을 우선하고, 아직 없는 값은 MOCK 표시된 데이터로 동작한다.

const OUNHOME_GRADES = [
  ['아이언','#77808f','⬟'],['브론즈','#b56b3d','◆'],['실버','#9aa7b6','✦'],['골드','#d99a16','★'],
  ['플래티넘','#36a9a1','✧'],['에메랄드','#23a66f','⬢'],['다이아몬드','#4e93e6','◈'],
  ['마스터','#8a5bd5','♛'],['그랜드마스터','#d94d78','♜'],['챌린저','#ee6c2f','🏆']
];
const SCORE_EXP_TABLE = [[249,0],[499,50],[749,150],[899,250],[1049,300],[1199,350],[1349,400],[1499,450],[1500,500]];
function mockExpForScore(score){ return (SCORE_EXP_TABLE.find(([max])=>score<=max)||[1500,500])[1]; }
function currentGrade(){
  const tier=Math.max(0,Math.min(OUNHOME_GRADES.length-1,Number(state.user.gradeIndex||0)));
  const [name,color,icon]=OUNHOME_GRADES[tier]; return {tier,name,color,icon};
}
function calculatedNextLevelExp(level){
  const safeLevel=Math.max(1,Number(level)||1);
  if(safeLevel<=10)return 100;
  return 1000+Math.floor((safeLevel-1)/10)*250;
}
function nextLevelExp(){ return Number(state.user.nextLevelExp ?? calculatedNextLevelExp(state.user.level)); }
function currentExpValue(){ return Number(state.user.currentExp ?? state.user.exp ?? 0); }
function levelProgressPct(){ return Math.min(100,Math.round(currentExpValue()/Math.max(1,nextLevelExp())*100)); }

Object.assign(state.user, {
  gradeIndex: state.user.gradeIndex || 0,
  currentExp: Number.isFinite(state.user.currentExp) ? state.user.currentExp : state.user.exp,
  nextLevelExp: state.user.nextLevelExp ?? calculatedNextLevelExp(state.user.level),
  freeWorkoutsUsed: state.user.freeWorkoutsUsed || 0,
  freeWorkoutDate: state.user.freeWorkoutDate || '',
  attendanceRewardClaimed: !!state.user.attendanceRewardClaimed,
});
state.crew.joinEnabled = state.crew.joinEnabled !== false;
state.crew.maxMembers = 5;
state.crew.weeklyMission = state.crew.weeklyMission || {target:300,progress:120,rewardExp:500,contributions:{나:52,써니핏:68,헬스왕:44}};
state.crew.battleHistory = state.crew.battleHistory || [];
state.crew.battleRequest = state.crew.battleRequest || {size:2,status:'idle'};
state.adminMissions = state.adminMissions || [
  {id:1,type:'개인',exercise:'스쿼트',metric:'운동 세트',target:1,min:1,max:3,points:50,exp:50,active:true},
  {id:2,type:'개인',exercise:'스쿼트',metric:'GOOD 이상',target:10,min:5,max:15,points:50,exp:50,active:true},
  {id:3,type:'크루',exercise:'전체',metric:'주간 운동 횟수',target:300,min:40,max:80,points:0,exp:500,active:true},
];

function kstDateKey(){ return new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Seoul',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date()); }
function syncDailyFreeWorkouts(){
  const today=kstDateKey();
  if(state.user.freeWorkoutDate!==today){ state.user.freeWorkoutDate=today; state.user.freeWorkoutsUsed=0; state.user.setsUsedToday=0; }
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
    if(gap>1) state.user.streak=1;
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
  const stored=getStoredAttendanceDates(),fallbackStreak=calculateAttendanceStreak();
  const firstDow=(new Date(year,month,1).getDay()+6)%7,daysInMonth=new Date(year,month+1,0).getDate();
  const cells=[]; for(let i=0;i<firstDow;i++)cells.push(null); for(let d=1;d<=daysInMonth;d++)cells.push(d);
  while(cells.length%7!==0)cells.push(null);
  const dowRow=DOW.map(l=>`<div class="att-dow-h">${l}</div>`).join('');
  const dayCells=cells.map(d=>{
    if(d==null)return '<div class="att-day empty"></div>';
    const iso=`${year}-${String(month+1).padStart(2,'0')}-${String(d).padStart(2,'0')}`;
    const isToday=d===todayDate,isStored=stored.has(iso),isFallback=!stored.size&&d<=todayDate&&(todayDate-d)<fallbackStreak,checked=isStored||isFallback;
    const cls=isToday?`att-day today${checked?' checked':''}`:checked?'att-day checked':'att-day';
    return `<div class="${cls}" aria-label="${iso}${checked?' 출석':''}"><span class="att-day-number">${d}</span>${checked?'<span class="att-check-ring" aria-hidden="true"></span>':''}</div>`;
  }).join('');
  return `<div style="margin:10px 0;"><p class="hint mono" style="margin:0 0 8px;font-weight:700;color:var(--ink);">${year}년 ${month+1}월</p><div class="att-month-grid">${dowRow}${dayCells}</div></div>`;
};

// 1. 회원가입 성별 선택 UI 및 사용자 요약
const _renderSignupV1 = renderSignup;
renderSignup = function(){
  const html=_renderSignupV1();
  const gender=`<div class="field"><label>성별 및 기본 캐릭터</label><div class="gender-choice">
    <button type="button" class="btn ${state.signup.gender==='male'?'btn-primary':'btn-secondary'}" onclick="state.signup.gender='male';render()">남성</button>
    <button type="button" class="btn ${state.signup.gender==='female'?'btn-primary':'btn-secondary'}" onclick="state.signup.gender='female';render()">여성</button>
  </div><p class="hint">가입 후 선택한 성별의 기본 캐릭터가 적용됩니다.</p></div>`;
  return html.replace('<div class="field">\n        <label>활동 지역',gender+'<div class="field">\n        <label>활동 지역')
    .replaceAll('스마트폰 카메라','스마트폰 카메라');
};
function renderUserProgressSummary(){
  const g=currentGrade();
  return `<div class="user-progress-summary">
    <span class="grade-mark" style="color:${g.color};border-color:${g.color};">${g.icon}</span>
    <div><b>${g.name} · Lv.${Math.min(500,state.user.level)}</b><div class="progress"><span style="width:${levelProgressPct()}%;background:${g.color};"></span></div>
    <small>${currentExpValue().toLocaleString()} / ${nextLevelExp().toLocaleString()} EXP · P ${state.user.points.toLocaleString()} · ${state.user.gender==='female'?'여성':'남성'}</small></div>
  </div>`;
}

// 2~3. 캘리브레이션은 신체정보 입력과 무관하며, 서버 calibration 여부만 사용한다.
calUpdateBmiLabel=function(){
  const startBtn=document.getElementById('cal-start-btn');
  if(startBtn){startBtn.disabled=false;startBtn.style.opacity='1';startBtn.style.cursor='pointer';}
  const hint=document.getElementById('cal-start-hint');if(hint)hint.style.display='none';
  const lbl=document.getElementById('cal-bmi-label');if(lbl){const b=calGetBodyInfo();lbl.textContent=b.bmi?`BMI ${b.bmi.toFixed(1)} · 입력 정보를 선택적으로 반영합니다.`:'키와 몸무게 없이도 캘리브레이션을 진행할 수 있습니다.';}
};
goToTutorial = function(){
  syncDailyFreeWorkouts();
  if(state.guestMode){ goExStep(1); return; }
  // 신체정보 입력 여부가 아니라 서버가 내려준 캘리브레이션 완료 여부만 본다.
  if(state.user.calibrated === false){
    toast('운동 전에 카메라 캘리브레이션을 진행해 주세요. 키와 몸무게는 입력하지 않아도 됩니다.');
    openCalibrationModal();
    return;
  }
  if(state.user.freeWorkoutsUsed>=3){
    if((state.user.retakeTickets||0)<1){ toast('오늘의 무료 운동 3회를 모두 사용했습니다. 운동 추가권이 필요합니다.'); setMenu('shop'); return; }
    askConfirm('운동 추가권 사용',`무료 운동 3회를 모두 사용했습니다. 티켓 1장을 사용합니다.\n티켓 운동은 경험치가 지급되지 않습니다.`,()=>{closeConfirm();state.exercise.sessionType='ticket';goExStep(1);},'운동추가권 사용');
    return;
  }
  state.exercise.sessionType='free'; goExStep(1);
};
const _renderTutorialV1=renderExStepTutorial;
renderExStepTutorial=()=>_renderTutorialV1().replaceAll('스마트폰 카메라 촬영','스마트폰 카메라 촬영').replaceAll('스마트폰 카메라','스마트폰 카메라');
startTutorialGate=function(){
  let left=TUTORIAL_GATE_SECONDS; const tick=()=>{const b=document.getElementById('ex-tutorial-start-btn');if(!b)return;b.textContent=left>0?`스마트폰 카메라 촬영 시작 (${left}초)`:'스마트폰 카메라 촬영 시작';b.disabled=left>0;b.style.opacity=left>0?'.5':'1';if(left-->0)setTimeout(tick,1000);};tick();
};

// 4, 7, 8. 1세트/실시간 점수/무료횟수/티켓 표시
getDailySetLimit=()=>3;
const _renderExStepPickV1=renderExStepPick;
renderExStepPick=function(){
  syncDailyFreeWorkouts();
  const used=Math.min(3,Number(state.user.freeWorkoutsUsed)||0);
  const remain=Math.max(0,3-used);
  let html=_renderExStepPickV1()
    .replace('오늘 가능한 운동세트','오늘의 무료 운동')
    .replace(/\d+ \/ \d+세트 사용 ·/,`사용 횟수 ${used}/3회 ·`)
    .replace(/\d+세트 남음/,`${remain}회 남음`)
    .replace(/<p class="hint" style="margin-top:6px;">[\s\S]*?<\/p>/,'');
  const guide=`<div class="attendance-reward-guide" style="margin:10px 0 0;padding:12px 14px;border:1px solid var(--line);border-radius:10px;font-size:12px;line-height:1.75;">
    <div style="margin-bottom:4px;"><b>이용 안내</b></div>
    <div>• 하루 무료 운동은 <b>기본 1회와 추가 기회 2회를 합쳐 총 3회</b> 제공됩니다.</div>
    <div>• 운동을 완료할 때마다 오늘 사용한 무료 운동 횟수가 <b><code>0/3회 → 1/3회 → 2/3회 → 3/3회</code></b>로 표시됩니다.</div>
    <div>• <code>운동 추가권 구매하기</code> 버튼을 누르면 <b>포인트 상점 → 기타</b> 메뉴로 이동하여 운동 추가권을 구매할 수 있습니다.</div>
    <div>• 무료 운동 횟수는 매일 <b>대한민국 시간(KST) 오전 0시(00:00)</b>를 기준으로 초기화됩니다.</div>
    <div>• 초기화 후 오늘의 무료 운동은 <b><code>0/3회</code></b>, 남은 무료 운동은 <b><code>3회</code></b>로 표시됩니다.</div>
    <div style="margin-top:7px;">※ 무료 운동 횟수가 남아 있는 동안에는 <code>운동 추가권 구매하기</code> 버튼이 비활성화됩니다.</div>
    <button class="btn btn-primary btn-block" style="margin-top:10px;${used<3?'opacity:.45;cursor:not-allowed;':''}" ${used<3?'disabled':''} onclick="setMenu('shop');state.shopFilter='기타';render();">운동 추가권 구매하기</button>
  </div>`;
  html=html.replace('</div>\n    <div style="margin-top:20px;">','</div>'+guide+'\n    <div style="margin-top:20px;">');
  return html;
};
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
  const ticket=state.exercise.sessionType==='ticket';
  const serverExp=r.expAwarded!=null||r.experienceAwarded!=null;
  const exp=ticket?0:Number(r.expAwarded ?? r.experienceAwarded ?? mockExpForScore(r.score));
  const points=Number(r.pointsAwarded ?? Math.round(r.score*.4));
  return `<div class="card result-summary"><div class="flex-between"><h2>운동 결과</h2><span class="pill ${ticket?'pill-gold':'pill-accent'}">${ticket?'운동추가권 사용 운동':'무료 운동'}</span></div>
    ${ticket?'<p class="ticket-warning">티켓 운동에서는 경험치가 지급되지 않습니다.</p>':''}
    <div class="stat-row"><div class="stat-box"><div class="num mono">${r.total}</div><div class="lbl">전체 횟수</div></div>${Object.entries(counts).map(([k,v])=>`<div class="stat-box"><div class="num mono">${v}</div><div class="lbl">${k}</div></div>`).join('')}</div>
    <div class="stat-row"><div class="stat-box"><div class="num mono">${r.score.toLocaleString()}</div><div class="lbl">총점</div></div><div class="stat-box"><div class="num mono">+${exp}</div><div class="lbl">획득 경험치${!serverExp&&!ticket?' (목)':''}</div></div><div class="stat-box"><div class="num mono">+${points}</div><div class="lbl">획득 포인트${r.pointsAwarded==null?' (목)':''}</div></div></div>
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
claimStreakReward=function(){
  syncAttendanceStatus();
  if(state.user.attendanceRewardClaimed){toast('오늘 출석 보상을 이미 받았습니다');return;}
  const today=kstDateKey();
  storeAttendanceDate(today);
  const streak=Math.max(1,calculateAttendanceStreak());
  state.user.streak=streak;
  const reward=streak>=3?15:5;
  state.user.points+=reward;
  state.user.attendanceRewardClaimed=true;
  const uid=state.user.id || state.user.nickname || 'local';
  try{localStorage.setItem(`ounhome_attendance_${uid}`,JSON.stringify({date:kstDateKey(),claimed:true}));}catch(_e){}
  toast(`출석 ${streak}일차 · 오늘 보상 +${reward}P`);
  render();
};

// 12~19. 크루 정책/주간미션/관리/자동매칭
renderCrewNoticeCard=()=>'';
const _renderJoinButtonV1=renderJoinButton;
renderJoinButton=function(c){
  const current=Number(c.currentMembers??c.memberCount??4),max=Number(c.maxMembers??5),enabled=c.joinEnabled!==false&&c.recruiting!==false&&current<max;
  return `<p class="hint">현재 인원 <b>${current}/${max}</b></p>`+(enabled?_renderJoinButtonV1(c):'<button class="btn btn-ghost btn-block" disabled>가입 불가</button>');
};
async function toggleCrewJoinEnabled(){
  if(getMyCrewRole()!=='팀장'){toast('크루장만 가입 활성 상태를 변경할 수 있습니다');return;}
  const previous=state.crew.joinEnabled!==false;
  const next=!previous;
  try{
    const res=await fetch(`${API_BASE}/api/crews/me/join-status`,{
      method:'PATCH',
      headers:{'Content-Type':'application/json','Authorization':'Bearer '+state.token},
      body:JSON.stringify({joinEnabled:next})
    });
    const body=await res.json();
    if(!body.success){toast(body.message||'가입 활성 상태 변경에 실패했습니다');return;}
    state.crew.joinEnabled=body.data && typeof body.data.joinEnabled==='boolean' ? body.data.joinEnabled : next;
    toast(`가입을 ${state.crew.joinEnabled?'활성화':'비활성화'}했습니다`);
    render();
  }catch(err){
    state.crew.joinEnabled=previous;
    toast('서버에 연결할 수 없습니다');
  }
}
function isCurrentCrewMember(m){return Number(m.userId)===Number(state.user.id)||m.n==='나';}
function transferCrewLeader(userId){const m=state.crew.members.find(x=>Number(x.userId)===Number(userId));if(!m||isCurrentCrewMember(m)){toast('본인에게는 크루장을 양도할 수 없습니다');return;}askConfirm('크루장 양도',`${m.n}님에게 크루장을 양도하시겠습니까?`,()=>{state.crew.members.forEach(x=>x.role=x===m?'팀장':(isCurrentCrewMember(x)?'팀원':x.role));closeConfirm();toast('크루장을 양도했습니다');render();},'양도');}
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
  return `<div class="card"><div class="flex-between"><h3>크루 관리</h3>${leader?`<div class="crew-join-toggle"><span class="crew-join-toggle-label">가입 활성</span><button type="button" class="switch ${state.crew.joinEnabled?'on':''}" role="switch" aria-checked="${state.crew.joinEnabled?'true':'false'}" aria-label="가입 활성 ${state.crew.joinEnabled?'ON':'OFF'}" onclick="toggleCrewJoinEnabled()"><span class="switch-state">${state.crew.joinEnabled?'ON':'OFF'}</span><span class="knob"></span></button></div>`:''}</div>
  ${leader?`<p class="section-label" style="margin-top:16px;">가입 신청 대기 (${requests.length})</p><div class="grid grid-2">${requests.length?requests.map(r=>`<div class="card"><div class="flex-between"><b>${r.n}</b><span class="pill pill-gold">PENDING</span></div><p class="hint">Lv.${r.level}${r.requestedAt?` · ${new Date(r.requestedAt).toLocaleString('ko-KR')}`:''}</p><p class="desc">${r.msg||'가입 메시지가 없습니다.'}</p><div class="crew-join-actions"><button class="btn btn-sm btn-secondary" onclick="rejectJoinRequest(${r.id})" aria-label="${r.n} 가입 요청 거절">거절</button><button class="btn btn-sm btn-primary" onclick="approveJoinRequest(${r.id})" aria-label="${r.n} 가입 요청 승인">승인</button></div></div>`).join(''):'<div class="empty-note" style="grid-column:1/-1;">대기 중인 가입 신청이 없습니다.</div>'}</div>`:''}
  <p class="hint">최대 인원 5명 · 현재 ${members.length}/5명</p>${leader?`<div class="table-wrap"><table><thead><tr><th>크루원</th><th>역할</th><th>레벨</th><th>관리</th></tr></thead><tbody>${members.map(m=>`<tr><td>${m.n}${isCurrentCrewMember(m)?' <span class="pill pill-accent">나</span>':''}${m.role==='팀장'?' <span class="pill pill-gold">크루장</span>':''}</td><td>${crewRoleLabel(m.role)}</td><td>Lv.${m.level}</td><td>${!isCurrentCrewMember(m)&&m.role!=='팀장'?`<button class="btn btn-sm btn-secondary" onclick="transferCrewLeader(${m.userId||0})">크루장 양도</button> <button class="btn btn-sm btn-danger" onclick="kickMember(${m.userId})">강퇴</button>`:'<span class="hint">관리 제외</span>'}</td></tr>`).join('')}</tbody></table></div>`:''}
  <div class="crew-management-danger-zone"><button class="btn btn-danger" ${leader&&others.length?'disabled style="opacity:.5"':''} onclick="leaveOrDisbandCrew()">크루 해체</button>${leader&&others.length?'<p class="hint">다른 크루원이 있는 동안 해체할 수 없습니다.</p>':''}</div></div>`;
}
const CREW_BATTLE_RESULT_SAMPLES = [
  {image:'assets/crew-battle-demo-preview.png', alt:'5대5 크루대전 실시간 점수 화면 예시', title:'5대5 팀 대전', score:'1,240 : 1,180', result:'승리'},
  {image:'assets/ranking-demo-preview.png', alt:'크루대전 결과 순위 화면 예시', title:'3대3 자동 매칭', score:'860 : 910', result:'패배'},
  {image:'assets/history-demo-preview.png', alt:'크루대전 운동 기록 화면 예시', title:'2대2 빠른 대전', score:'520 : 480', result:'승리'},
];
function renderCrewBattleResultCard(){
  const history=state.crew.battleHistory||[];
  const content=history.length
    ? `<div class="battle-result-list">${history.map(x=>`<div class="battle-result-row"><span>${x.at} · vs ${x.opponent}</span><b>${x.ourScore}:${x.oppScore} · ${x.result}</b><span class="pill pill-accent">+${x.exp} EXP</span></div>`).join('')}</div>`
    : `<p class="hint">아직 실제 대전 데이터가 없어 레이아웃 확인용 예시를 표시합니다.</p><div class="battle-result-samples">${CREW_BATTLE_RESULT_SAMPLES.map(x=>`<article class="battle-result-sample"><img src="${x.image}" alt="${x.alt}" loading="lazy"><div><b>${x.title}</b><p class="mono">${x.score}</p><span class="pill ${x.result==='승리'?'pill-accent':'pill-muted'}">${x.result} · 예시</span></div></article>`).join('')}</div>`;
  return `<div class="card"><div class="flex-between"><h3>대전 결과</h3>${history.length?'<span class="pill pill-accent">실제 기록</span>':'<span class="pill pill-muted">샘플 미리보기</span>'}</div>${content}</div>`;
}
const _renderCrewBattleV1=renderCrewBattle;
renderCrewBattle=function(){
  const b=state.crewBattle;if(!b)return renderCrewBattleMenu();
  const our=(b.myScore||0)+(b.teammates||[]).reduce((s,x)=>s+(x.score||0),0),opp=b.oppScore||0;
  return `<div class="card crew-battle-live-head"><div class="flex-between"><div><p class="section-label">${state.crew.name}</p><b class="mono" style="font-size:34px;">${our.toLocaleString()}</b><small> · 누적 운동 ${(b.myGradeCounts?Object.values(b.myGradeCounts).reduce((a,c)=>a+c,0):0)}회</small></div><div><b>제한시간</b><div class="mono" style="font-size:28px;">02:00</div></div><div style="text-align:right;"><p class="section-label">${b.oppName||'상대 크루'}</p><b class="mono" style="font-size:34px;">${opp.toLocaleString()}</b><small> · 누적 운동 ${b.oppReps||0}회</small></div></div><p class="hint">화면 하단 참가자별 운동 횟수와 점수 · 종료 후 대전 결과로 자동 이동</p></div>`+_renderCrewBattleV1();
};
const _renderCrewOverviewV1=renderCrewOverview;
renderCrewOverview=function(){const w=state.crew.weeklyMission;const members=state.crew.members;return _renderCrewOverviewV1().replace('크루 미션 누적점수','크루대전 기여도').replace('오늘의 크루미션','크루 주간 미션').replace('일일','주간')+`<div class="card"><div class="flex-between"><h3>크루 주간 미션</h3><span class="pill pill-accent">보상 +500 크루 EXP</span></div><div class="gauge"><span class="fill" style="width:${Math.min(100,w.progress/w.target*100)}%"></span><span class="gauge-label">${w.progress} / ${w.target}</span></div><p class="hint">크루원별 주간 적용 범위 40~80회</p>${members.map(m=>`<p>${m.n}: ${Math.max(40,Math.min(80,w.contributions[m.n]||m.weeklyReps||40))}회</p>`).join('')}</div>`;};
const _renderCrewV1=renderCrew;
renderCrew=function(){
  if(!state.crew.created)return _renderCrewV1();
  const leader=getMyCrewRole()==='팀장';
  const tabs=['크루 메인','크루대전','크루채팅','크루원 정보',leader?'크루관리':'크루탈퇴'];
  const i=Math.min(state.subtabs.crew,tabs.length-1),t=tabs[i];
  return `<div class="view-head"><h1>${state.crew.name}</h1></div><div class="subtabs subtabs-compact">${tabs.map((x,n)=>`<div class="tab ${n===i?'active':''}" onclick="setSub('crew',${n})">${x}</div>`).join('')}</div>${t==='크루 메인'?renderCrewOverview():t==='크루채팅'?renderCrewChat():t==='크루원 정보'?renderCrewMembers():t==='크루대전'?renderCrewBattleMenu():leader?renderCrewManagementV2():renderCrewLeave()}`;
};

// 20. 화면 설명과 클라이언트 목 정렬. 서버는 rank 값을 확정해 내려주는 것이 원칙이다.
const _renderRankingV1=renderRanking;
renderRanking=function(){state.rank.crew.sort((a,b)=>(b.level-a.level)-0||(b.currentExp||b.exp||0)-(a.currentExp||a.exp||0)||String(a.reachedAt||'').localeCompare(String(b.reachedAt||'')));return _renderRankingV1();};

// 21. 관리자 미션 관리
function renderAdminMissionManagement(){
  if(state.user.role!=='ADMIN')return '<div class="empty-note">관리자만 접근할 수 있습니다.</div>';
  return `<div class="view-head"><h1>미션 관리</h1><p>개인 미션과 크루 미션을 등록·수정·조회하고 활성 상태를 관리합니다.</p></div><div class="card admin-mission-form"><div class="grid grid-3"><div class="field"><label>구분</label><select id="am-type"><option>개인</option><option>크루</option></select></div><div class="field"><label>운동 종류</label><select id="am-ex"><option>스쿼트</option><option>전체</option></select></div><div class="field"><label>목표 항목</label><input id="am-metric" value="운동 횟수"></div><div class="field"><label>목표 횟수</label><input id="am-target" type="number" value="10"></div><div class="field"><label>최소/최대 횟수</label><div class="field-row"><input id="am-min" type="number" value="5"><input id="am-max" type="number" value="15"></div></div><div class="field"><label>보상(P/EXP)</label><div class="field-row"><input id="am-points" type="number" value="50"><input id="am-exp" type="number" value="50"></div></div></div><button class="btn btn-primary" onclick="addAdminMission()">미션 등록</button></div>
  <div class="table-wrap"><table><thead><tr><th>구분</th><th>운동</th><th>목표</th><th>범위</th><th>보상</th><th>상태/관리</th></tr></thead><tbody>${state.adminMissions.map(m=>`<tr><td>${m.type}</td><td>${m.exercise}</td><td>${m.metric} ${m.target}회</td><td>${m.min}~${m.max}</td><td>${m.points}P / ${m.exp}EXP</td><td><button class="btn btn-sm ${m.active?'btn-primary':'btn-ghost'}" onclick="toggleAdminMission(${m.id})">${m.active?'활성':'비활성'}</button> <button class="btn btn-sm btn-secondary" onclick="editAdminMission(${m.id})">수정</button></td></tr>`).join('')}</tbody></table></div>`;
}
function addAdminMission(){state.adminMissions.push({id:Date.now(),type:document.getElementById('am-type').value,exercise:document.getElementById('am-ex').value,metric:document.getElementById('am-metric').value,target:+document.getElementById('am-target').value,min:+document.getElementById('am-min').value,max:+document.getElementById('am-max').value,points:+document.getElementById('am-points').value,exp:+document.getElementById('am-exp').value,active:true,mock:true});toast('미션을 등록했습니다 (목데이터)');render();}
function toggleAdminMission(id){const m=state.adminMissions.find(x=>x.id===id);if(m)m.active=!m.active;render();}
function editAdminMission(id){const m=state.adminMissions.find(x=>x.id===id);if(!m)return;m.target=Number(prompt('목표 횟수',m.target))||m.target;toast('미션을 수정했습니다 (목데이터)');render();}

// 공통 화면에 사용자 진행 요약을 추가한다.
const _renderMainV1=renderMain;
renderMain=function(){
  const html=_renderMainV1();
  if(state.guestMode)return html;
  const anchor='<p class="hint" style="margin:4px 0 0;font-size:13px;">Lv.'+state.user.level+' · 꾸준함을 키우는 중</p>';
  return html.replace(anchor,anchor+renderUserProgressSummary());
};
const _renderAppV1=renderApp;
renderApp=function(){
  const g=currentGrade();
  return _renderAppV1().replace(/<span class="topbar-nick">([\s\S]*?)<\/span>/,`<span class="topbar-nick"><span>$1</span><small style="display:block;color:${g.color};">${g.icon} ${g.name} · ${currentExpValue()}/${nextLevelExp()} EXP</small></span>`);
};
