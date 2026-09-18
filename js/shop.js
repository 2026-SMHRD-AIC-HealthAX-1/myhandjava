// shop.js — '포인트 상점' 카테고리: 아이템 목록/구매/미리보기.

function renderShop(){
  return `
  <div class="view-head"><h1>포인트 상점</h1></div>
  ${renderMissionShop()}`;
}
const SHOP_CATEGORIES=['전체','헤어','상의','하의','신발','배경','기타'];
function renderMissionShop(){
  const f=SHOP_CATEGORIES.includes(state.shopFilter)?state.shopFilter:'전체';
  const items=state.shopItems.map((it,idx)=>({it,idx})).filter(({it})=>f==='전체'||it.category===f);
  return `
  <div class="subtabs">
    ${SHOP_CATEGORIES.map(c=>`<div class="tab ${f===c?'active':''}" onclick="setShopFilter('${c}')">${c}</div>`).join('')}
  </div>
  <div class="grid grid-3 shop-item-grid" style="max-width:640px;margin:0 auto;">
    ${items.map(({it,idx})=>`
      <div class="card shop-item-card">
        <div class="feed-media shop-item-media" style="background:#f3f7ff;overflow:hidden;display:flex;align-items:center;justify-content:center;">
          ${it.asset
            ? ((it.slot==='shoes'||it.slot==='head')
              ? `<canvas class="clean-shop-asset" data-src="${it.asset}" data-slot="${it.slot}" aria-label="${it.name}" style="width:100%;height:100%;display:block;"></canvas>`
              : `<img src="${it.asset}" alt="${it.name}" style="width:100%;height:100%;object-fit:${it.slot==='background'?'cover':'contain'};padding:${it.slot==='background'?'0':'8px'};border-radius:8px;">`)
            : it.name}
        </div>
        <div class="shop-item-body">
          <div class="flex-between shop-item-title"><h3 style="margin:0;">${it.name}</h3></div>
          <span class="pill ${it.name==='닉네임 컬러 이펙트'?'pill-accent':(it.effect.startsWith('능력치 없음')?'pill-muted':'pill-accent')} shop-item-effect">효과 · ${it.name==='닉네임 컬러 이펙트'?'닉네임 컬러 변경':it.effect}</span>
          ${it.consumable?`<p class="desc shop-item-owned">보유 수량: ${it.name==='닉네임 변경권'?(state.user.nicknameTickets||0):state.user.retakeTickets}장</p>`:''}
          <div class="shop-item-desc">${it.effectDesc}</div>
          <div class="shop-item-footer">
            <span class="shop-price">P ${it.price}</span>
            <div class="shop-item-actions">
              ${it.slot?`<button class="btn btn-sm btn-secondary" onclick="${state.guestMode ? "goto('login')" : `openItemPreview(${idx})`}">미리보기</button>`:''}
              <button class="btn btn-sm ${(it.owned && !it.consumable)?'btn-ghost':'btn-primary'}" ${(it.owned && !it.consumable)?'disabled style="opacity:.5;"':''} onclick="${state.guestMode ? "goto('login')" : `buyItem(${idx})`}">${(it.owned && !it.consumable)?'보유중':'구매하기'}</button>
            </div>
          </div>
        </div>
      </div>`).join('')}
  </div>`;
}
function setShopFilter(c){ state.shopFilter=c; render(); }


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
    img.src=canvas.dataset.src;
  });
}
const NICKNAME_PREVIEW_COLORS = [
  {name:'기본', value:'var(--gold)'},
  {name:'코랄', value:'#ff6f61'},
  {name:'하늘', value:'#4aa8ff'},
  {name:'민트', value:'#27b89a'},
  {name:'보라', value:'#8b6cff'},
  {name:'핑크', value:'#e85aa5'},
];
function getNicknameEffectColor(){
  const equipped=state.shopItems.find(it=>it.slot==='nickname' && it.owned && it.equipped);
  return equipped ? 'var(--gold)' : 'inherit';
}
function openItemPreview(idx){ state.itemPreview={open:true,idx,color:getNicknameEffectColor()}; render(); }
function closeItemPreview(){ state.itemPreview={open:false,idx:null,color:null}; render(); }
function setNicknamePreviewColor(color){ if(state.itemPreview.open){ state.itemPreview.color=color; render(); } }
function renderItemPreviewModal(){
  const it=state.shopItems[state.itemPreview.idx]; if(!it) return '';
  const isNickname=it.slot==='nickname', color=state.itemPreview.color||getNicknameEffectColor();
  const nickname=state.user.nickname||'홈트초보';
  return `
  <div class="confirm-backdrop" onclick="if(event.target===this) closeItemPreview()">
    <div class="confirm-box item-preview-modal" style="text-align:center;">
      <h3>${isNickname?'닉네임 컬러 이펙트 미리보기':`${it.name} 착용 예시`}</h3>
      ${isNickname?`
        <div class="nickname-preview-name" style="color:${color};">${nickname}</div>
        <p class="desc">색상을 선택하면 현재 닉네임에 적용된 모습을 미리 볼 수 있습니다.</p>
        <div class="nickname-preview-palette" role="group" aria-label="닉네임 컬러 선택">
          ${NICKNAME_PREVIEW_COLORS.map(c=>`<button type="button" class="nickname-preview-swatch ${color===c.value?'selected':''}" style="--preview-color:${c.value};" title="${c.name}" aria-label="${c.name}" onclick="setNicknamePreviewColor('${c.value}')"><span></span><b>${c.name}</b></button>`).join('')}
        </div>
      `:`
        <canvas id="item-preview-canvas" style="width:216px;height:264px;max-width:100%;margin:10px auto;display:block;border-radius:10px;image-rendering:auto;"></canvas>
        <p class="desc">${it.effectDesc}</p>
      `}
      <div class="confirm-actions" style="justify-content:center;"><button class="btn btn-secondary" onclick="closeItemPreview()">닫기</button></div>
    </div>
  </div>`;
}
function drawItemPreviewCanvas(){
  const canvas=document.getElementById('item-preview-canvas'), it=state.shopItems[state.itemPreview.idx];
  if(!canvas||!it)return;
  drawPixelCharacter(canvas,{...getEquipState(),[it.slot]:it.asset?it:true},state.user.gender);
}

function buyItem(idx){
  const it=state.shopItems[idx];
  if(it.owned && !it.consumable){toast('이미 보유한 아이템입니다'); return;}
  if(state.user.points<it.price){toast('포인트가 부족합니다'); return;}
  state.user.points -= it.price;
  if(it.consumable){
    if(it.name==='닉네임 변경권'){
      state.user.nicknameTickets = (state.user.nicknameTickets||0) + 1;
      toast(`${it.name} 구매 완료 (보유 ${state.user.nicknameTickets}장)`);
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
