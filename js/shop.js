// shop.js — '포인트 상점' 카테고리: 아이템 목록/구매/미리보기.
// [담당] '포인트 상점' 탭(아이템 목록/구매/착용/미리보기).
// [백엔드 연동] GET /api/shop/items, POST /api/shop/items/{id}/purchase, /equip, /unequip
//              → DB: shop_items, user_items, users(포인트 차감).
// [주의] 아이템 착용 가능 레벨 제한(levelReq)은 이번 세션에 완전히 제거됐다 — 포인트만
//        충분하면 레벨 무관하게 구매 가능하니, 레벨 체크 로직을 다시 넣지 않도록 주의.

function renderShop(){
  return `
  <div class="view-head"><h1>포인트 상점</h1></div>
  ${renderMissionShop()}`;
}
const SHOP_CATEGORIES=['전체','헤어','상의','하의','신발','배경','기타'];
// 지금 실제로 판매 중인 아이템만 넣어둔 목록 — 나머지는 이미지가 멀쩡해도 "준비중"으로 표시하고
// 구매/미리보기를 막는다(공개 범위를 좁혀둔 임시 조치, unavailable 계산 참고).
const SHOP_ENABLED_ITEM_NAMES = new Set([
  '네이비 스포츠 캡', '라벤더 후디', '라벤더 조거 팬츠', '민트 운동화',
  '배경 - 맑은 강변 산책로', '배경 - 노을빛 강변', '배경 - 가을 호수 공원', '배경 - 비 오는 가로수길',
  '닉네임 컬러 이펙트', '닉네임 변경권', '순위 도전 티켓',
]);
function renderMissionShop(){
  const f=SHOP_CATEGORIES.includes(state.shopFilter)?state.shopFilter:'전체';
  const items=state.shopItems.map((it,idx)=>({it,idx})).filter(({it})=>it.name!=='운동 추가권' && (f==='전체'||it.category===f));
  return `
  <div class="subtabs">
    ${SHOP_CATEGORIES.map(c=>`<div class="tab ${f===c?'active':''}" onclick="setShopFilter('${c}')">${c}</div>`).join('')}
  </div>
  <div class="grid grid-3 shop-item-grid" style="max-width:640px;margin:0 auto;">
    ${items.map(({it,idx})=>{
      const unavailable = !it.asset || it.assetMissing || !SHOP_ENABLED_ITEM_NAMES.has(it.name);
      return `
      <div class="card shop-item-card">
        <div class="feed-media shop-item-media" style="background:#f3f7ff;overflow:hidden;display:flex;align-items:center;justify-content:center;">
          ${(!it.asset || it.assetMissing)
            ? `<span class="shop-item-comingsoon">준비중인 아이템이에요</span>`
            : `<img src="${it.asset}" alt="${it.name}" onerror="markShopAssetMissing(${idx})" style="width:100%;height:100%;object-fit:contain;padding:8px;border-radius:8px;">`}
        </div>
        <div class="shop-item-body">
          <div class="flex-between shop-item-title"><h3 style="margin:0;">${it.name}</h3></div>
          <span class="pill ${it.name==='닉네임 컬러 이펙트'?'pill-accent':(it.effect.startsWith('능력치 없음')?'pill-muted':'pill-accent')} shop-item-effect">효과 · ${it.name==='닉네임 컬러 이펙트'?'닉네임 컬러 변경':it.effect}</span>
          ${(it.consumable && it.name!=='닉네임 컬러 이펙트')?`<p class="desc shop-item-owned">보유 수량: ${it.name==='닉네임 변경권'?(state.user.nicknameTickets||0):it.name==='순위 도전 티켓'?(state.user.rankTickets||0):(state.user.retakeTickets||0)}장</p>`:''}
          <div class="shop-item-desc">${it.effectDesc}</div>
          <div class="shop-item-footer">
            <span class="shop-price">P ${it.price}</span>
            <div class="shop-item-actions">
              ${(it.slot && it.name!=='닉네임 컬러 이펙트')?`<button class="btn btn-sm btn-secondary" ${unavailable?'disabled style="opacity:.5;"':''} onclick="${state.guestMode ? "goto('login')" : `openItemPreview(${idx})`}">미리보기</button>`:''}
              <button class="btn btn-sm ${(it.owned && !it.consumable)?'btn-ghost':'btn-primary'}" ${(it.owned && !it.consumable)||unavailable?'disabled style="opacity:.5;"':''} onclick="${state.guestMode ? "goto('login')" : (it.name==='닉네임 컬러 이펙트' ? `openItemPreview(${idx})` : `buyItem(${idx})`)}">${(it.owned && !it.consumable)?'보유중':unavailable?'준비중':'구매하기'}</button>
            </div>
          </div>
        </div>
      </div>`;
    }).join('')}
  </div>`;
}
function setShopFilter(c){ state.shopFilter=c; render(); }

// 서버 카탈로그(구매 가능 아이템)를 불러와 로컬 카탈로그에 병합한다. 서버 카탈로그에 없는
// 이름(지금 판매 안 하는 아이템 등)은 매칭되는 로컬 항목이 없거나 그대로 owned:false로 둔다.
async function loadShopItems(){
  if(!state.token) return;
  try{
    const res = await fetch(`${API_BASE}/api/shop/items`, {
      headers: { 'Authorization': 'Bearer ' + state.token }
    });
    const body = await res.json();
    if(!body.success) return;
    body.data.forEach(server => {
      const local = state.shopItems.find(it => it.name === server.name);
      if(!local) return;
      local.serverId = server.id;
      local.owned = server.owned;
      local.equipped = server.equipped;
      local.price = server.price;
    });
    render();
  }catch(err){
    console.error('상점 아이템 불러오기 실패', err);
  }
}


// v11.1: 신발/모자 상품 PNG의 작은 고립 조각을 화면에서 제거한다.
// 원본 파일을 훼손하지 않고 alpha 연결요소를 분석해 신발은 큰 2개, 모자는 큰 1개 덩어리만 유지한다.
function drawCleanShopAssets(){
  document.querySelectorAll('canvas.clean-shop-asset').forEach(canvas=>{
    if(canvas.dataset.rendered==='1') return;
    const img=new Image();
    img.onload=()=>{
      const W=360,H=220; canvas.width=W; canvas.height=H;
      const c=canvas.getContext('2d'); c.clearRect(0,0,W,H);
      const scale=Math.min((W-20)/img.naturalWidth,(H-20)/img.naturalHeight);
      const dw=img.naturalWidth*scale, dh=img.naturalHeight*scale;
      c.drawImage(img,(W-dw)/2,(H-dh)/2,dw,dh);
      canvas.dataset.rendered='1';
    };
    img.onerror=()=>markShopAssetMissing(Number(canvas.dataset.idx));
    img.src=canvas.dataset.src;
  });
}
// 이미지 파일이 실제로 없거나(경로 오류 등) 로드에 실패한 상품은 카드에 "준비중인
// 아이템이에요"를 대신 보여주고 구매를 막는다 — asset 필드 자체가 비어있는 상품도 이 조건에
// 걸린다(renderMissionShop의 unavailable 계산 참고).
function markShopAssetMissing(idx){
  const it=state.shopItems[idx];
  if(!it || it.assetMissing) return;
  it.assetMissing=true;
  render();
}
const NICKNAME_PREVIEW_COLORS = [
  {name:'기본', value:'#1a1a1a'},
  {name:'코랄', value:'#ff6f61'},
  {name:'하늘', value:'#4aa8ff'},
  {name:'민트', value:'#27b89a'},
  {name:'보라', value:'#8b6cff'},
  {name:'핑크', value:'#e85aa5'},
  {name:'노랑', value:'#ffc400'},
];
// 닉네임 컬러 이펙트는 보유/착용 아이템이 아니라 소모 아이템이라, 적용된 색은 shopItems의
// owned/equipped가 아니라 로컬에 직접 저장한다(서버에 별도 컬럼이 없음 — requirements-v2.js의
// 출석 로컬 저장과 같은 방식, uid별 키).
function getStoredNicknameColor(){
  if(state.guestMode) return '';
  const uid=state.user.id || state.user.nickname || 'local';
  try{ return localStorage.getItem(`ounhome_nickname_color_${uid}`) || ''; }catch(_e){ return ''; }
}
function storeNicknameColor(color){
  if(state.guestMode) return;
  const uid=state.user.id || state.user.nickname || 'local';
  try{ localStorage.setItem(`ounhome_nickname_color_${uid}`, color || ''); }catch(_e){}
}
function getNicknameEffectColor(){
  return getStoredNicknameColor() || 'inherit';
}
function openItemPreview(idx){ state.itemPreview={open:true,idx,color:getNicknameEffectColor()}; render(); }
function closeItemPreview(){ state.itemPreview={open:false,idx:null,color:null}; render(); }
// 스와치를 눌러도 아직 포인트는 안 쓴다 — 모달 안에서는 미리보기 색만 바뀌고, 실제 차감·적용은
// 아래 "구매" 버튼(buyItem)을 눌러야 일어난다.
function setNicknamePreviewColor(color){
  if(!state.itemPreview.open) return;
  state.itemPreview.color=color;
  render();
}
function renderItemPreviewModal(){
  const it=state.shopItems[state.itemPreview.idx]; if(!it) return '';
  const isNickname=it.slot==='nickname', color=state.itemPreview.color||getNicknameEffectColor();
  const nickname=state.user.nickname||'홈트초보';
  return `
  <div class="confirm-backdrop" onclick="if(event.target===this) closeItemPreview()">
    <div class="confirm-box item-preview-modal" style="text-align:center;">
      <h3>${isNickname?'닉네임 컬러 선택':`${it.name} 착용 예시`}</h3>
      ${isNickname?`
        <div class="nickname-preview-name" style="color:${color};">${nickname}</div>
        <p class="desc">색상을 고른 뒤 구매를 눌러야 실제로 적용돼요.</p>
        <div class="nickname-preview-palette" role="group" aria-label="닉네임 컬러 선택">
          ${NICKNAME_PREVIEW_COLORS.map(c=>`<button type="button" class="nickname-preview-swatch ${color===c.value?'selected':''}" style="--preview-color:${c.value};" title="${c.name}" aria-label="${c.name}" onclick="setNicknamePreviewColor('${c.value}')"><span></span><b>${c.name}</b></button>`).join('')}
        </div>
      `:`
        <canvas id="item-preview-canvas" style="width:216px;height:264px;max-width:100%;margin:10px auto;display:block;border-radius:10px;image-rendering:auto;"></canvas>
        <p class="desc">${it.effectDesc}</p>
      `}
      <div class="confirm-actions" style="justify-content:center;">
        <button class="btn btn-secondary" onclick="closeItemPreview()">닫기</button>
        ${isNickname?`<button class="btn btn-primary" onclick="buyItem(${state.itemPreview.idx})">구매 (P ${it.price})</button>`:''}
      </div>
    </div>
  </div>`;
}
function drawItemPreviewCanvas(){
  const canvas=document.getElementById('item-preview-canvas'), it=state.shopItems[state.itemPreview.idx];
  if(!canvas||!it)return;
  drawPixelCharacter(canvas,{...getEquipState(),[it.slot]:it.asset?it:true},state.user.gender);
}

async function buyItem(idx){
  const it=state.shopItems[idx];
  if(it.owned && !it.consumable){toast('이미 보유한 아이템입니다'); return;}
  if(!it.asset || it.assetMissing){toast('아직 준비 중인 아이템입니다'); return;}
  if(state.user.points<it.price){toast('포인트가 부족합니다'); return;}
  if(!it.serverId){toast('아직 구매할 수 없는 아이템입니다'); return;}
  try{
    const res = await fetch(`${API_BASE}/api/shop/items/${it.serverId}/purchase`, {
      method: 'POST',
      headers: { 'Authorization': 'Bearer ' + state.token }
    });
    const body = await res.json();
    if(!body.success){ toast(body.message || '구매에 실패했습니다'); return; }
  }catch(err){
    console.error('구매 실패', err);
    toast('구매 중 오류가 발생했습니다');
    return;
  }
  state.user.points -= it.price;
  if(it.consumable){
    if(it.name==='닉네임 변경권'){
      state.user.nicknameTickets = (state.user.nicknameTickets||0) + 1;
      toast(`${it.name} 구매 완료 (보유 ${state.user.nicknameTickets}장)`);
    } else if(it.name==='닉네임 컬러 이펙트'){
      // 티켓처럼 쌓아두지 않는다 — 모달에서 고른 색(무료 미리보기 상태였던 값)을 결제가
      // 끝난 지금 시점에 저장·적용하고 모달을 닫는다.
      const color = state.itemPreview.open ? (state.itemPreview.color || getNicknameEffectColor()) : getNicknameEffectColor();
      storeNicknameColor(color);
      toast(`${it.name} 구매 완료 — 닉네임 색상이 적용됐어요`);
      closeItemPreview();
      return;
    } else if(it.name==='순위 도전 티켓'){
      state.user.rankTickets = (state.user.rankTickets||0) + 1;
      toast(`${it.name} 구매 완료 (보유 ${state.user.rankTickets}장)`);
    } else {
      state.user.retakeTickets = (state.user.retakeTickets||0) + 1;
      toast(`${it.name} 구매 완료 (보유 ${state.user.retakeTickets}장)`);
    }
  } else {
    it.owned=true;
    toast(`${it.name} 구매 완료`);
  }
  render();
}

/* ========================================================================
   3. 홈크루
   ======================================================================== */
// (FR-CR-001~005) 크루 생성/가입/배분/강퇴/공지/가입승인은 모두 아래 파이프라인이 필요한 구간입니다.
//   크루 생성(createCrew) / 가입(joinCrew) > Java 크루 API > DB 연결 > SQL INSERT(크루 테이블, 크루원 테이블)
//   크루 단체미션 진행도 갱신(saveExerciseResult에서 자동 반영) > Java 크루 API > DB 연결 > SQL UPDATE(크루 미션 진행도)
//   가입 요청 승인(approveJoinRequest) > Java 크루 API > DB 연결 > SQL INSERT(크루원) + DELETE(가입요청)
//   크루원 강퇴(kickMember) > Java 크루 API > DB 연결 > SQL DELETE(크루원 테이블)
//   크루공지 작성(postCrewNotice, 팀장 전용) > Java 크루 API(권한 확인) > DB 연결 > SQL INSERT(공지 테이블)
//   크루채팅 전송(sendCrewChat) > WebSocket(크루 ID 채널 브로드캐스트) > DB 연결 > SQL INSERT(채팅 메시지)
//   크루대전 파티 초대/수락(sendPartyInvites~acceptPartyInvite) > WebSocket(대상 사용자 알림) > DB 연결 > SQL INSERT/UPDATE(파티초대)
