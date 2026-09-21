// utils.js — 여러 화면이 공통으로 쓰는 유틸(토스트, 확인모달, 아바타 색상/스프라이트 로딩, 등급 색상, 해시).

const AVATAR_COLORS = ['#1B3A6B','#E8532B','#C98A00','#3E8FCF','#7A5CC9','#2AA9C9'];
function avatarColor(i){return AVATAR_COLORS[i % AVATAR_COLORS.length];}
function avatarInitial(name){return (name||'홈').trim().charAt(0) || 'H';}

// 프로필 캐릭터 PNG. 투명 배경이 포함된 캐릭터를 기본 레이어로 그리고,
// profile.js의 drawPixelCharacter()가 착용 아이템 PNG를 위에 레이어링한다.
const CHAR_SPRITES = {male:null, female:null};
function loadCharSprite(gender, src){
  const img=new Image();
  img.onload=()=>{
    CHAR_SPRITES[gender]=img;
    drawAvatarCanvas(); drawTopbarAvatar(); drawPodiumChars(); drawMainCharCanvas();
  };
  img.src=src;
}
loadCharSprite('male','assets/avatar-male.png');
loadCharSprite('female','assets/avatar-female.png');

/* ========================================================================
   유틸
   ======================================================================== */
function toast(msg){
  const t=document.getElementById('toast');
  t.textContent=msg;
  t.classList.add('show');
  clearTimeout(toast._tid);
  toast._tid=setTimeout(()=>t.classList.remove('show'),2200);
}
function askConfirm(title,desc,onYes,yesLabel='확인',danger=false){
  state.confirm={title,desc,onYes,yesLabel,danger};
  render();
}
function closeConfirm(){state.confirm=null;render();}

function gradeColor(g){
  if(g==='PERFECT') return 'var(--accent)';
  if(g==='GREAT') return 'var(--gold)';
  if(g==='GOOD') return '#4A7CFF';
  return 'var(--danger)';
}
function gradePill(g){
  const cls = g==='PERFECT'?'pill-accent':g==='GREAT'?'pill-gold':g==='GOOD'?'pill-muted':'pill-danger';
  return `<span class="pill ${cls}">${g}</span>`;
}

// 사용자 등급(레벨 1~500, 500레벨을 다 채우면 다음 등급으로 승급하고 다시 1레벨부터 시작 —
// 백엔드 UserGrade/UserLevelPolicy 참고) 색상. 리그오브레전드 티어와 같은 순서·색감을 쓴다.
const USER_GRADE_COLORS = {
  IRON: '#6B6B6B',
  BRONZE: '#A9702F',
  SILVER: '#ADB8C0',
  GOLD: '#E8B339',
  PLATINUM: '#3FC1B0',
  EMERALD: '#2FBF71',
  DIAMOND: '#4FA3E3',
  MASTER: '#A855F7',
  GRANDMASTER: '#E63950',
  CHALLENGER: '#7EE7E8',
};
function userGradeColor(grade){ return USER_GRADE_COLORS[grade] || USER_GRADE_COLORS.IRON; }
// 백엔드 UserGrade.koreanName과 동일한 한글 표기 — 고객센터 FAQ의 등급 목록 등에서 재사용한다.
const USER_GRADE_NAMES = {
  IRON: '아이언', BRONZE: '브론즈', SILVER: '실버', GOLD: '골드', PLATINUM: '플래티넘',
  EMERALD: '에메랄드', DIAMOND: '다이아몬드', MASTER: '마스터', GRANDMASTER: '그랜드마스터', CHALLENGER: '챌린저',
};
// 레벨 앞에 붙는 등급 배지 — 알통(💪) 이모지 자체는 CSS color로 다시 칠할 수 없어서, 팔각형
// 배지의 배경색을 등급 색으로 채우고 그 위에 이모지를 얹는 방식으로 표현한다(.level-badge-icon,
// style.css 참고). compact=true면 topbar처럼 좁은 자리에 맞게 배지·글자를 조금 작게 그린다.
function userLevelBadge(grade, gradeName, level, compact){
  const color = userGradeColor(grade);
  const size = compact ? 24 : 32;
  const icon = `<span class="level-badge-icon" style="width:${size}px;height:${size}px;font-size:${compact ? 13 : 17}px;background:${color};" title="${gradeName || ''}">💪</span>`;
  return `<span style="display:inline-flex;align-items:center;gap:5px;vertical-align:middle;">${icon}<span class="mono" style="color:${color};font-weight:700;font-size:${compact ? 13 : 15}px;">Lv.${level}</span></span>`;
}

/* ========================================================================
   렌더 엔진 : 화면 라우팅
   ======================================================================== */
function hashStr(s){
  let h=2166136261;
  for(let i=0;i<s.length;i++){ h^=s.charCodeAt(i); h=Math.imul(h,16777619); }
  return h>>>0;
}
