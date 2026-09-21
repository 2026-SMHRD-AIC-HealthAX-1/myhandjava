// shop.js — '포인트 상점' 카테고리: 아이템 목록/구매/미리보기.

function renderShop(){
  return `
  <div class="view-head"><h1>포인트 상점</h1></div>
  ${renderMissionShop()}`;
}
const SHOP_CATEGORIES=['전체','헤어','상의','하의','신발','기타'];
function renderMissionShop(){
  const allItems = Array.isArray(state.shopItems) ? state.shopItems : [];
  const requested = state.shopFilter || '전체';
  const f = SHOP_CATEGORIES.includes(requested) ? requested : '전체';
  const items = allItems
    .map((it,idx)=>({it,idx}))
    .filter(({it})=> f === '전체' || it.category === f);

  return `
  <div class="subtabs">
    ${SHOP_CATEGORIES.map(c=>`<div class="tab ${f===c?'active':''}" onclick="setShopFilter('${c}')">${c}</div>`).join('')}
  </div>
  <div class="grid grid-3" style="max-width:900px;margin:0 auto;">
    ${items.length ? items.map(({it,idx})=>{
      const effect = it.effect || '능력치 없음 · 외형 전용';
      const effectDesc = it.effectDesc || '아이템 설명이 없습니다.';
      const price = Number.isFinite(Number(it.price)) ? Number(it.price) : 0;
      return `
      <div class="card">
        <div class="feed-media" style="height:88px;display:flex;align-items:center;justify-content:center;text-align:center;padding:8px;">${it.icon ? `<span style="font-size:36px;">${it.icon}</span>` : (it.name || '이름 없는 아이템')}</div>
        <div class="flex-between" style="margin-top:10px;">
          <h3 style="margin:0;">${it.name || '이름 없는 아이템'}</h3>
        </div>
        <span class="pill ${effect.startsWith('능력치 없음')?'pill-muted':'pill-accent'}" style="margin-top:8px;">효과 · ${effect}</span>
        ${it.consumable?`<p class="desc" style="margin-top:4px;color:var(--accent);">보유 수량: ${it.name==='닉네임 변경권'?(state.user.nicknameTickets||0):it.name==='운동 추가권'?(state.user.extraSets||0):(state.user.retakeTickets||0)}${it.name==='운동 추가권'?'세트':'장'}</p>`:''}
        <p class="desc" style="margin-top:8px;">${effectDesc}</p>
        <div class="flex-between" style="margin-top:6px;gap:8px;">
          <span class="shop-price">P ${price}</span>
          <div style="display:flex;gap:6px;flex-wrap:wrap;justify-content:flex-end;">
            ${it.slot?`<button class="btn btn-sm btn-secondary" onclick="${state.guestMode ? "goto('login')" : `openItemPreview(${idx})`}">착용해보기</button>`:''}
            <button class="btn btn-sm ${(it.owned && !it.consumable)?'btn-ghost':'btn-primary'}" ${(it.owned && !it.consumable)?'disabled style="opacity:.5;"':''} onclick="${state.guestMode ? "goto('login')" : `buyItem(${idx})`}">${(it.owned && !it.consumable)?'보유중':'구매하기'}</button>
          </div>
        </div>
      </div>`;
    }).join('') : `
      <div class="card" style="grid-column:1/-1;text-align:center;padding:32px 20px;">
        <h3 style="margin:0 0 8px;">표시할 아이템이 없습니다.</h3>
        <p class="desc" style="margin:0;">다른 카테고리를 선택하거나 state.shopItems 데이터를 확인해주세요.</p>
      </div>`}
  </div>`;
}
function setShopFilter(c){ state.shopFilter=c; render(); }
function openItemPreview(idx){ state.itemPreview={open:true, idx}; render(); }
function closeItemPreview(){ state.itemPreview={open:false, idx:null}; render(); }
function renderItemPreviewModal(){
  const it=state.shopItems[state.itemPreview.idx];
  return `
  <div class="confirm-backdrop" onclick="if(event.target===this) closeItemPreview()">
    <div class="confirm-box" style="text-align:center;">
      <h3>${it.name} 착용 예시</h3>
      <canvas id="item-preview-canvas" style="width:144px;height:176px;margin:10px auto;display:block;border-radius:10px;image-rendering:pixelated;"></canvas>
      <p class="desc">${it.effectDesc}</p>
      <div class="confirm-actions" style="justify-content:center;">
        <button class="btn btn-secondary" onclick="closeItemPreview()">닫기</button>
      </div>
    </div>
  </div>`;
}
function drawItemPreviewCanvas(){
  const canvas=document.getElementById('item-preview-canvas');
  const it=state.shopItems[state.itemPreview.idx];
  if(!canvas || !it) return;
  drawPixelCharacter(canvas, {...getEquipState(), [it.slot]:it}, state.user.gender);
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
    } else if(it.name==='운동 추가권'){
      state.user.extraSets = (state.user.extraSets||0) + 3;
      toast(`${it.name} 구매 완료 (오늘 가능한 운동세트 +3)`);
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

// '오늘 가능한 운동세트' 카드에서 상점 전체로 이동하지 않고 '운동 추가권'만 바로 살 수
// 있도록 하는 미니 구매 창. askConfirm(공용 확인모달)을 그대로 재사용한다.
function openExtraSetPurchase(){
  if(state.guestMode){ goto('login'); return; }
  const idx = state.shopItems.findIndex(it=>it.name==='운동 추가권');
  if(idx<0) return;
  const it = state.shopItems[idx];
  askConfirm(
    '운동 추가권 구매',
    `${it.icon || ''} P${it.price} · ${it.effectDesc}<br>보유 포인트 ${state.user.points}P`,
    ()=>{ buyItem(idx); closeConfirm(); },
    '구매하기'
  );
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
