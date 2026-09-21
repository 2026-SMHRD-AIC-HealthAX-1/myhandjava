// utils.js — 여러 화면이 공통으로 쓰는 유틸(토스트, 확인모달, 아바타 색상/스프라이트 로딩, 등급 색상, 해시).

const AVATAR_COLORS = ['#1B3A6B','#E8532B','#C98A00','#3E8FCF','#7A5CC9','#2AA9C9'];
function avatarColor(i){return AVATAR_COLORS[i % AVATAR_COLORS.length];}
function avatarInitial(name){return (name||'홈').trim().charAt(0) || 'H';}

// 프로필 캐릭터 픽셀아트 스프라이트. PNG 자체가 이미 검정 배경을 투명 처리해둔 컷아웃
// 이미지라, 배경 아이템(equip.background)을 씌워도 캐릭터 뒤로 비쳐 보인다.
// (주의) 원본 PNG는 검정 배경에 합성된 상태였는데, 그걸 브라우저에서 getImageData로 읽어
// 알파를 지우는 방식은 file:// 로 열었을 때 "canvas has been tainted by cross-origin data"
// 보안 오류로 막혀서 동작하지 않았다. 그래서 투명화는 스크립트 실행 전에 미리 처리해
// assets/avatar-*.png 자체를 투명 PNG로 만들어두고, 여기서는 단순히 그리기만 한다.
const CHAR_SPRITES = {male:null, female:null};
const AVATAR_ITEM_SPRITES = {};
function loadCharSprite(gender, src){
  const img=new Image();
  img.onload=()=>{
    CHAR_SPRITES[gender]=img;
    drawAvatarCanvas(); drawTopbarAvatar(); drawPodiumChars(); drawMainCharCanvas();
    if (typeof drawOnboardingAvatar === 'function') drawOnboardingAvatar();
  };
  img.src=src;
}
loadCharSprite('male','assets/avatar-male-sd.png');
loadCharSprite('female','assets/avatar-female-sd.png');

function loadAvatarItemSprite(src){
  if (!src) return null;
  if (AVATAR_ITEM_SPRITES[src]) return AVATAR_ITEM_SPRITES[src];
  const img = new Image();
  AVATAR_ITEM_SPRITES[src] = img;
  img.onload = () => {
    if (typeof drawAvatarCanvas === 'function') drawAvatarCanvas();
    if (typeof drawTopbarAvatar === 'function') drawTopbarAvatar();
    if (typeof drawMainCharCanvas === 'function') drawMainCharCanvas();
    if (typeof drawPodiumChars === 'function') drawPodiumChars();
    if (typeof drawItemPreviewCanvas === 'function') drawItemPreviewCanvas();
  };
  img.onerror = () => { console.error('아바타 아이템 이미지 로드 실패:', src); };
  img.src = src;
  return img;
}

// 첫 미리보기에서 빈 아이템이 보이지 않도록 앱 시작과 함께 이미지를 캐시에 올린다.
if (typeof AVATAR_ITEM_CATALOG !== 'undefined') {
  AVATAR_ITEM_CATALOG.forEach(item => {
    loadAvatarItemSprite(item.asset);
    (item.layers || []).forEach(layer => loadAvatarItemSprite(layer.asset));
  });
}
if (typeof AVATAR_COMBO_FULL_CANVAS !== 'undefined') {
  Object.values(AVATAR_COMBO_FULL_CANVAS).forEach(entry => {
    if (entry.male) loadAvatarItemSprite(entry.male);
    if (entry.female) loadAvatarItemSprite(entry.female);
  });
}
if (typeof AVATAR_COMBO_BG_RIVERSIDE_DAY !== 'undefined') {
  Object.values(AVATAR_COMBO_BG_RIVERSIDE_DAY).forEach(entry => {
    if (entry.male) loadAvatarItemSprite(entry.male);
    if (entry.female) loadAvatarItemSprite(entry.female);
  });
}

/* ========================================================================
   유틸
   ======================================================================== */
// 크루채팅처럼 사용자가 직접 입력한 문자열을 템플릿 문자열로 그대로 끼워 넣는 곳에서
// XSS를 막기 위한 이스케이프. <script>나 onerror= 같은 걸 메시지에 넣어도 그냥 텍스트로만 보인다.
function escapeHtml(str){
  return String(str == null ? '' : str)
    .replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
    .replace(/"/g,'&quot;').replace(/'/g,'&#39;');
}
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

// 사용자 등급(레벨 10단위로 자동 결정 — 1~10 아이언, 11~20 브론즈 ... 91~100(이상) 챌린저.
// 백엔드 UserGrade.forLevel/UserLevelPolicy 참고) 색상. 리그오브레전드 티어와 같은 순서·색감을 쓴다.
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
// 랭킹 목록처럼 레벨만 내려오고 등급 문자열은 안 내려오는 응답에서, 백엔드 UserGrade.forLevel과
// 똑같은 규칙(레벨 10단위 구간, 91+는 챌린저 고정)으로 등급 코드를 그대로 계산해낸다.
function gradeFromLevel(level){
  const codes = Object.keys(USER_GRADE_COLORS);
  const bucket = Math.max(0, Math.floor((Number(level || 1) - 1) / 10));
  return codes[Math.min(codes.length - 1, bucket)];
}
// 백엔드 UserGrade.koreanName과 동일한 한글 표기 — 고객센터 FAQ의 등급 목록 등에서 재사용한다.
const USER_GRADE_NAMES = {
  IRON: '아이언', BRONZE: '브론즈', SILVER: '실버', GOLD: '골드', PLATINUM: '플래티넘',
  EMERALD: '에메랄드', DIAMOND: '다이아몬드', MASTER: '마스터', GRANDMASTER: '그랜드마스터', CHALLENGER: '챌린저',
};
// 등급별 레벨 구간(백엔드 UserGrade.forLevel과 동일한 10레벨 단위 규칙) — 고객센터 FAQ의
// 등급 목록에 "Lv.1~10"처럼 함께 보여줄 때 쓴다.
const USER_GRADE_LEVEL_RANGE = {
  IRON: '1~10', BRONZE: '11~20', SILVER: '21~30', GOLD: '31~40', PLATINUM: '41~50',
  EMERALD: '51~60', DIAMOND: '61~70', MASTER: '71~80', GRANDMASTER: '81~90', CHALLENGER: '91~100',
};
// 등급별 완성된 배지 아트(헥사곤+보석+월계관/왕관까지 그려진 이미지) — assets/ranks/*.png.
// CSS로 직접 그리기엔 디테일이 많아 시안 이미지를 그대로 쓴다.
const RANK_ICONS = {
  IRON: 'assets/ranks/iron.png',
  BRONZE: 'assets/ranks/bronze.png',
  SILVER: 'assets/ranks/silver.png',
  GOLD: 'assets/ranks/gold.png',
  PLATINUM: 'assets/ranks/platinum.png',
  EMERALD: 'assets/ranks/emerald.png',
  DIAMOND: 'assets/ranks/diamond.png',
  MASTER: 'assets/ranks/master.png',
  GRANDMASTER: 'assets/ranks/grandmaster.png',
  CHALLENGER: 'assets/ranks/challenger.png',
};
// 등급 아이콘만(레벨 텍스트 없이) — 이미 옆에 "Lv.X"를 따로 보여주는 자리(메인
// 대시보드 캐릭터 카드 등)에서 쓴다. size는 px 한 변 길이.
function rankBadgeIcon(grade, gradeName, size){
  const src = RANK_ICONS[grade] || RANK_ICONS.IRON;
  return `<img class="rank-icon" src="${src}" alt="${gradeName || ''}" title="${gradeName || ''}" style="width:${size}px;height:${size}px;object-fit:contain;flex-shrink:0;vertical-align:middle;">`;
}
// 레벨 앞에 붙는 등급 배지 — 등급 아이콘 이미지 옆에 "Lv.X" 텍스트를 붙인다.
// compact=true면 topbar처럼 좁은 자리에 맞게 배지·글자를 조금 작게 그린다.
function userLevelBadge(grade, gradeName, level, compact){
  const size = compact ? 24 : 32;
  const badge = rankBadgeIcon(grade, gradeName, size);
  return `<span style="display:inline-flex;align-items:center;gap:5px;vertical-align:middle;">${badge}<span class="mono" style="color:${userGradeColor(grade)};font-weight:700;font-size:${compact ? 13 : 15}px;">Lv.${level}</span></span>`;
}

/* ========================================================================
   렌더 엔진 : 화면 라우팅
   ======================================================================== */
function hashStr(s){
  let h=2166136261;
  for(let i=0;i<s.length;i++){ h^=s.charCodeAt(i); h=Math.imul(h,16777619); }
  return h>>>0;
}
