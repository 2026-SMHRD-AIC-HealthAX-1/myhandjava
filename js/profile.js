// profile.js — '마이페이지' 카테고리 전체(캐릭터 꾸미기, 미션 달성 현황, 운동 히스토리, 계정관리).

const PROFILE_TABS = ['프로필·캐릭터 꾸미기', '미션 달성 현황', '히스토리', '계정관리'];
function renderProfile() {
  const i = state.subtabs.profile;
  const body = i === 0 ? renderMissionAvatar() :
    i === 1 ? renderMissionProgress() :
      i === 2 ? renderHistory() :
        renderSetAccount();
  return `
  <div class="view-head"><h1>마이페이지</h1></div>
  <div class="subtabs subtabs-compact">
    ${PROFILE_TABS.map((t, idx) => `<div class="tab ${i === idx ? 'active' : ''}" onclick="setSub('profile',${idx})">${t}</div>`).join('')}
  </div>
  ${renderGuestBlur(body, '로그인하면 내 캐릭터·미션·운동 기록·계정 정보를 확인할 수 있어요')}`;
}
const EXP_PER_LEVEL = 1000;
function getProfileStats() {
  const gc = { PERFECT: 0, GREAT: 0, GOOD: 0, MISS: 0 };
  state.history.forEach(h => { if (h.gc) Object.keys(gc).forEach(k => gc[k] += h.gc[k] || 0); });
  const gcTotal = Object.values(gc).reduce((a, b) => a + b, 0) || 1;
  const exCounts = {};
  state.history.forEach(h => { exCounts[h.ex] = (exCounts[h.ex] || 0) + h.reps; });
  const activeEffects = state.shopItems
    .filter(it => it.slot && it.owned && it.equipped && !it.effect.startsWith('능력치 없음'))
    .map(it => `${it.name} · ${it.effect}`);
  return {
    total: totalScore(),
    expToNext: Math.round((100 - state.user.exp) / 100 * EXP_PER_LEVEL),
    // 예전엔 이름 해시로 지어낸 가짜 이웃 순위(getRegionRanking, ranking.js)였다 — 이제
    // 로그인 직후 실제 GET /api/rankings/region으로 받아온 값(ranking.js loadMyRegionRank)을
    // 그대로 쓴다. 아직 못 불러왔거나 동네 미설정이면 null.
    myRank: state.user.regionRank,
    perfectPct: Math.round(gc.PERFECT / gcTotal * 100),
    greatPct: Math.round(gc.GREAT / gcTotal * 100),
    missPct: Math.round(gc.MISS / gcTotal * 100),
    gc, gcTotal: gc.PERFECT + gc.GREAT + gc.GOOD + gc.MISS, // 등급 비율 도넛차트(renderGradeDonut)용 원본 카운트
    exCounts: Object.entries(exCounts),
    activeEffects,
  };
}
// 퍼펙트/그레이트/굿/미스 비율을 도넛 차트 + 범례 표로 그린다(stroke-dasharray 트릭이라
// 외부 차트 라이브러리 없이 순수 SVG로 그려진다). segments의 value 합이 0이면(기록 없음)
// 빈 상태 문구만 보여준다.
function renderGradeDonut(segments, centerLabel) {
  const total = segments.reduce((s, x) => s + x.value, 0);
  if (!total) return '<p class="empty-note">아직 운동 기록이 없어요.</p>';
  const size = 140, strokeWidth = 22, r = (size - strokeWidth) / 2, C = 2 * Math.PI * r;
  let acc = 0;
  const arcs = segments.filter(s => s.value > 0).map(s => {
    const frac = s.value / total;
    const dash = frac * C, gap = C - dash;
    const offset = -acc * C;
    acc += frac;
    return `<circle cx="${size / 2}" cy="${size / 2}" r="${r}" fill="none" stroke="${s.color}" stroke-width="${strokeWidth}"
      stroke-dasharray="${dash} ${gap}" stroke-dashoffset="${offset}" transform="rotate(-90 ${size / 2} ${size / 2})"/>`;
  }).join('');
  return `
  <div style="display:flex;flex-direction:column;align-items:center;gap:14px;">
    <div style="position:relative;width:${size}px;height:${size}px;">
      <svg width="${size}" height="${size}" viewBox="0 0 ${size} ${size}">${arcs}</svg>
      <div style="position:absolute;inset:0;display:flex;flex-direction:column;align-items:center;justify-content:center;">
        <span class="hint" style="margin:0;">${centerLabel}</span>
        <span class="mono" style="font-size:22px;font-weight:700;color:var(--ink);">${total}</span>
      </div>
    </div>
    <div style="width:100%;display:flex;flex-direction:column;gap:6px;">
      ${segments.map(s => `
        <div class="flex-between" style="font-size:12.5px;">
          <span style="display:flex;align-items:center;gap:6px;"><span style="width:10px;height:10px;border-radius:50%;background:${s.color};display:inline-block;flex:none;"></span>${s.label}</span>
          <span class="mono" style="color:var(--ink-dim);">${s.value} · ${Math.round(s.value / total * 100)}%</span>
        </div>`).join('')}
    </div>
  </div>`;
}
// (FR-PF-001~003) renderMissionAvatar: 캐릭터·아이템 꾸미기 화면. 그리기 자체(drawPixelCharacter)는
// 캔버스로 그리는 순수 프론트엔드 로직이고, "저장이 필요한 동작"만 아래 두 함수에서 이어집니다.
//   자기소개 저장(saveProfileBio) > Java 프로필 API > DB 연결 > SQL UPDATE(계정 테이블 bio 컬럼)
//   아이템 착용/해제(toggleEquip) > Java 프로필 API > DB 연결 > SQL UPDATE(보유 아이템 테이블 equipped 여부)
function renderCosmeticCard(it) {
  const idx = state.shopItems.indexOf(it);
  return `
  <div class="cosmetic-item-card">
    <img src="${itemIconDataURL(it.name)}" alt="${it.name}" style="width:64px;height:64px;object-fit:contain;border-radius:6px;display:block;margin:0 auto 8px;flex:none;">
    <div class="flex-between" style="flex:none;"><b style="font-size:12.5px;">${it.name}</b>
      ${it.owned ? (it.equipped ? '<span class="pill pill-accent">착용중</span>' : '<span class="pill pill-muted">보유</span>') : '<span class="pill pill-gold">' + it.price + 'P</span>'}
    </div>
    <span class="pill ${it.name==='닉네임 컬러 이펙트' ? 'pill-accent' : (it.effect.startsWith('능력치 없음') ? 'pill-muted' : 'pill-accent')}" style="margin-top:6px;flex:none;">효과 · ${it.name==='닉네임 컬러 이펙트' ? '닉네임 컬러 변경' : it.effect}</span>
    <p class="desc" style="margin-top:6px;font-size:11.5px;">${it.effectDesc}</p>
    <button class="btn btn-sm ${it.owned ? 'btn-ghost' : 'btn-secondary'}" style="width:100%;" onclick="${it.owned ? `toggleEquip(${idx})` : `goToShopFor(${idx})`}">${it.owned ? (it.equipped ? '착용 해제' : '착용하기') : '상점에서 구매'}</button>
  </div>`;
}
function goToShopFor(idx) {
  setMenu('shop');
  toast(`${state.shopItems[idx].name}은(는) 포인트 상점에서 구매할 수 있어요`);
}
function renderMissionAvatar() {
  const cosmetics = state.shopItems.filter(it => it.slot);
  const nickColor = (typeof getNicknameEffectColor === 'function') ? getNicknameEffectColor() : 'inherit';
  return `
  <div class="grid profile-page-grid">
    <div class="card profile-main-card">
      <div class="profile-avatar-layout">
        <div class="profile-character-block">
          <p class="section-label" style="text-align:left;">내 캐릭터</p>
          <canvas id="avatar-char-canvas" class="profile-character-canvas"></canvas>
          <h3 class="profile-nickname" style="color:${nickColor};">${state.user.nickname || '홈트초보'}</h3>
          <span class="profile-grade-level" style="--profile-grade-color:${userGradeColor(state.user.grade)};color:${userGradeColor(state.user.grade)};">
            ${rankBadgeIcon(state.user.grade, state.user.gradeName, 56)}
            <span>${state.user.gradeName || USER_GRADE_NAMES[state.user.grade] || '아이언'} <b>Lv.${Math.min(500, state.user.level)}</b></span>
          </span>
        </div>
        <div class="profile-bio-block">
          <label for="profile-bio-input" class="section-label" style="display:block;">자기소개</label>
          <div class="field">
            <textarea id="profile-bio-input" rows="6" maxlength="80" placeholder="나를 소개하는 한마디를 남겨보세요">${state.user.bio || ''}</textarea>
            <button class="btn btn-sm btn-secondary" style="margin-top:8px;width:100%;" onclick="saveProfileBio()">자기소개 저장</button>
          </div>
        </div>
      </div>
    </div>
    ${renderOwnedItemsCard(cosmetics)}
  </div>`;
}
// 아이템이 늘어날수록 그리드 줄 수가 늘어나서 카드가 계속 길어지고, 옆의 "내 캐릭터" 카드까지
// (같은 그리드 행이라 align-items:stretch로) 덩달아 늘어났다 — 한 페이지에 4개(2x2)만 보여주는
// 것만으로는 부족했다(4개 찬 페이지와 1개만 있는 마지막 페이지의 내용 높이가 서로 달라서,
// 페이지를 넘길 때마다 두 카드 높이가 또 같이 바뀌었다). 그래서 style.css에서 아이템 한 칸
// (.cosmetic-item-card)을 고정 높이로 만들고, 그리드는 아이템 개수와 상관없이 항상 2행을
// 예약한다(.profile-items-grid grid-template-rows) — 이렇게 "4칸 꽉 찬 오른쪽 카드" 높이가
// 페이지마다 항상 똑같아지고, 왼쪽 "내 캐릭터" 카드는 grid align-items:stretch로 그 높이를
// 그대로 따라간다.
const PROFILE_ITEMS_PAGE_SIZE = 4;
function renderOwnedItemsCard(cosmetics){
  const owned = cosmetics.filter(it => it.owned);
  const pageCount = Math.max(1, Math.ceil(owned.length / PROFILE_ITEMS_PAGE_SIZE));
  const page = Math.min(state.profileItemsPage || 0, pageCount - 1);
  const pageItems = owned.slice(page * PROFILE_ITEMS_PAGE_SIZE, page * PROFILE_ITEMS_PAGE_SIZE + PROFILE_ITEMS_PAGE_SIZE);
  return `
  <div class="card profile-items-card">
    <div class="flex-between">
      <p class="section-label" style="margin:0;">보유 아이템</p>
      ${owned.length > PROFILE_ITEMS_PAGE_SIZE ? `
      <div style="display:flex;align-items:center;gap:8px;">
        <button type="button" class="btn btn-sm btn-ghost" style="padding:4px 10px;" ${page <= 0 ? 'disabled style="opacity:.4;"' : ''} onclick="changeProfileItemsPage(-1)">‹</button>
        <span class="hint mono" style="margin:0;">${page + 1} / ${pageCount}</span>
        <button type="button" class="btn btn-sm btn-ghost" style="padding:4px 10px;" ${page >= pageCount - 1 ? 'disabled style="opacity:.4;"' : ''} onclick="changeProfileItemsPage(1)">›</button>
      </div>` : ''}
    </div>
    <div class="grid profile-items-grid" style="grid-template-columns:repeat(2,1fr);align-content:start;margin-top:10px;">
      ${pageItems.map(it => renderCosmeticCard(it)).join('') || '<p class="empty-note" style="grid-column:1/-1;">아직 보유한 꾸미기 아이템이 없어요.</p>'}
    </div>
    <p class="hint" style="margin-top:14px;">보유 아이템을 착용/해제하면 캐릭터에 바로 반영됩니다. 새 아이템은 포인트 상점에서 구매할 수 있어요.</p>
  </div>`;
}
function changeProfileItemsPage(delta){
  state.profileItemsPage = Math.max(0, (state.profileItemsPage || 0) + delta);
  render();
}
// 예전엔 여기서 state.user.bio만 바꾸고 끝이라 화면엔 저장된 것처럼 보였지만 서버에는 전혀
// 반영이 안 됐다 — 그래서 로그아웃 후 다시 로그인해 loadMyProfile()이 서버 값(빈 문자열)으로
// 덮어쓰면 방금 쓴 소개글이 사라졌다. saveAccount()가 이미 쓰고 있는 것과 같은
// PATCH /api/users/me로 실제 저장한다.
async function saveProfileBio() {
  const el = document.getElementById('profile-bio-input');
  if (!el) return;
  const bio = el.value.trim();
  try {
    const res = await fetch(`${API_BASE}/api/users/me`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + state.token },
      body: JSON.stringify({ bio })
    });
    const body = await res.json();
    if (!body.success) { toast(body.message || '저장에 실패했습니다'); return; }
    state.user.bio = bio;
    toast('자기소개를 저장했습니다');
    render();
  } catch (err) {
    toast('서버에 연결할 수 없습니다 (백엔드가 켜져 있는지 확인해주세요)');
  }
}
function getEquipState() {
  const bySlot = {};
  state.shopItems.forEach(it => {
    if (it.slot && it.owned && it.equipped) bySlot[it.slot] = it.asset ? it : true;
  });
  return bySlot;
}
async function toggleEquip(idx) {
  const it = state.shopItems[idx];
  if (!it.owned) { toast('포인트 상점에서 구매해주세요'); return; }
  const nextEquipped = !it.equipped;
  // 서버 카탈로그 아이템(구매로 얻은 것)은 착용 상태도 서버에 반영해야 새로고침 후에도 유지된다.
  // 기본 지급 아이템(serverId 없음)은 예전처럼 로컬 상태로만 관리한다.
  if (it.serverId) {
    try {
      const res = await fetch(`${API_BASE}/api/shop/items/${it.serverId}/${nextEquipped ? 'equip' : 'unequip'}`, {
        method: 'POST',
        headers: { 'Authorization': 'Bearer ' + state.token }
      });
      const body = await res.json();
      if (!body.success) { toast(body.message || '처리에 실패했습니다'); return; }
    } catch (err) {
      console.error('착용 상태 변경 실패', err);
      toast('착용 처리 중 오류가 발생했습니다');
      return;
    }
  }
  // 같은 슬롯(예: 배경)에 아이템이 여러 개 생길 수 있어, 새로 착용할 때는 같은 슬롯의
  // 나머지 아이템을 먼저 해제해서 한 슬롯에 하나만 착용되도록 한다.
  if (nextEquipped && it.slot) {
    state.shopItems.forEach(o => { if (o !== it && o.slot === it.slot) o.equipped = false; });
  }
  it.equipped = nextEquipped;
  toast(it.equipped ? `${it.name} 착용했습니다` : `${it.name} 착용 해제했습니다`);
  render();
}
function drawAvatarCanvas() {
  const canvas = document.getElementById('avatar-char-canvas');
  if (!canvas) return;
  drawPixelCharacter(canvas, getEquipState(), state.user.gender);
}
// 상단바의 작은 프로필 캐릭터 미리보기. 캐릭터 캔버스는 144x176 논리 좌표를 유지한 채
// 실제 픽셀은 4배 해상도로 그려 큰 프로필 화면에서도 선명하게 표시한다.
function drawTopbarAvatar() {
  const canvas = document.getElementById('topbar-avatar-canvas');
  if (!canvas) return;
  // 게스트는 실제로 보유·장착한 아이템이 없는 계정이라, 장착 이펙트 없는 기본 아바타를 그린다.
  drawPixelCharacter(canvas, state.guestMode ? {} : getEquipState(), state.guestMode ? 'male' : state.user.gender);
}
// gender: 'male' | 'female' — 회원가입 캘리브레이션에서 고른 값(state.user.gender)을 그대로 받아
// 머리 모양만 구분한다. 로봇 스킨 아이템을 장착하면 성별과 무관하게 로봇 얼굴이 우선한다.
function drawPixelCharacter(canvas, equip, gender) {
  const U = 8, W = 18, H = 22, RENDER_SCALE = 4;
  const logicalWidth = W * U, logicalHeight = H * U;
  canvas.width = logicalWidth * RENDER_SCALE;
  canvas.height = logicalHeight * RENDER_SCALE;
  const ctx = canvas.getContext('2d');
  ctx.setTransform(RENDER_SCALE, 0, 0, RENDER_SCALE, 0, 0);
  ctx.imageSmoothingEnabled = true;
  ctx.imageSmoothingQuality = 'high';

  // 모자+후디+조거팬츠 조합이 실제로 그려둔 전신 사진과 일치하면, 기본 캐릭터+개별 레이어
  // 합성 대신 그 사진을 통째로 쓴다. 사진 자체가 흰 배경(또는 riverside-day 배경이 이미
  // 그려진 사진)이라 이 경우엔 drawAvatarBackground를 따로 호출하지 않는다.
  const combo = (typeof getAvatarComboKey === 'function') ? getAvatarComboKey(equip, gender) : null;
  if (combo) {
    const overlay = loadAvatarItemSprite(combo.src);
    if (overlay && overlay.complete && overlay.naturalWidth) {
      const sw = overlay.naturalWidth, sh = overlay.naturalHeight;
      const scale = Math.min(logicalWidth / sw, logicalHeight / sh);
      const dw = sw * scale, dh = sh * scale;
      ctx.drawImage(overlay, (logicalWidth - dw) / 2, logicalHeight - dh, dw, dh);
      drawAvatarWearables(ctx, equip, gender, { skipIds: ['head-cap', 'top-lavender-hoodie', 'bottom-lavender-joggers'] });
      return;
    }
  }

  drawAvatarBackground(ctx, equip.background, logicalWidth, logicalHeight);

  const sprite = CHAR_SPRITES[gender === 'female' ? 'female' : 'male'];
  if (sprite) {
    ctx.save();
    const sw = sprite.naturalWidth || sprite.width, sh = sprite.naturalHeight || sprite.height;
    const scale = Math.min((W * U) / sw, (H * U) / sh);
    const dw = sw * scale, dh = sh * scale;

    // v11.1: 기본 캐릭터 원본의 오렌지 신발만 흰색/연회색으로 보정한다.
    // 착용 아이템 레이어는 이후에 그려지므로 민트/라벤더/오렌지 신발 아이템 색상에는 영향이 없다.
    const off = document.createElement('canvas');
    off.width = sw; off.height = sh;
    const ox = off.getContext('2d', { willReadFrequently:true });
    ox.drawImage(sprite, 0, 0);
    try {
      const img = ox.getImageData(0, 0, sw, sh);
      const p = img.data;
      const shoeStartY = Math.floor(sh * 0.77);
      for (let y=shoeStartY; y<sh; y++) {
        for (let x=0; x<sw; x++) {
          const i=(y*sw+x)*4, r=p[i], g=p[i+1], b=p[i+2], a=p[i+3];
          // 오렌지/주황 계열만 선택. 피부색은 발목 위라 shoeStartY 밖에 있어 보호된다.
          if (a>20 && r>145 && r>g*1.28 && g>b*1.12) {
            const lum=Math.max(0,Math.min(255,Math.round(r*.22+g*.55+b*.23)));
            const v=Math.max(205,Math.min(250,215+Math.round(lum*.14)));
            p[i]=v; p[i+1]=v; p[i+2]=Math.min(255,v+3);
          }
        }
      }
      ox.putImageData(img,0,0);
      ctx.drawImage(off, (W * U - dw) / 2, H * U - dh, dw, dh);
    } catch(e) {
      ctx.drawImage(sprite, (W * U - dw) / 2, H * U - dh, dw, dh);
    }
    ctx.restore();
  }


  drawAvatarWearables(ctx, equip, gender);
}

// 배경 이미지는 캐릭터 캔버스(18:22)를 빈틈없이 채우는 cover 방식으로 자른다.
// 이미지 비율은 유지하므로 가로·세로로 찌그러지지 않고, 캐릭터와 착용 아이템보다 먼저 그려진다.
function drawAvatarBackground(ctx, backgroundItem, width, height) {
  if (backgroundItem && backgroundItem.asset) {
    const image = loadAvatarItemSprite(backgroundItem.asset);
    if (image && image.complete && image.naturalWidth) {
      const sourceRatio = image.naturalWidth / image.naturalHeight;
      const targetRatio = width / height;
      let sx = 0, sy = 0, sw = image.naturalWidth, sh = image.naturalHeight;
      if (sourceRatio > targetRatio) {
        sw = image.naturalHeight * targetRatio;
        sx = (image.naturalWidth - sw) / 2;
      } else {
        sh = image.naturalWidth / targetRatio;
        sy = (image.naturalHeight - sh) / 2;
      }
      ctx.drawImage(image, sx, sy, sw, sh, 0, 0, width, height);
      return;
    }
  }

  // v11.1: 기본 미리보기 배경은 검정/갈색 대신 밝은 아이보리-블루 격자.
  // 별도 배경 아이템을 착용한 경우에만 해당 배경 이미지를 사용한다.
  ctx.fillStyle = '#f8fbff';
  ctx.fillRect(0, 0, width, height);
  ctx.save();
  ctx.strokeStyle = '#dfe8f2';
  ctx.lineWidth = 0.7;
  const grid = 16;
  for (let x=0; x<=width; x+=grid) {
    ctx.beginPath(); ctx.moveTo(x,0); ctx.lineTo(x,height); ctx.stroke();
  }
  for (let y=0; y<=height; y+=grid) {
    ctx.beginPath(); ctx.moveTo(0,y); ctx.lineTo(width,y); ctx.stroke();
  }
  ctx.restore();
}

function drawAvatarWearables(ctx, equip, gender, opts) {
  const skipIds = (opts && opts.skipIds) || [];
  const items = (typeof AVATAR_WEARABLE_SLOTS === 'undefined' ? [] : AVATAR_WEARABLE_SLOTS)
    .map(slot => equip[slot])
    .filter(item => item && (item.fullCanvas || item.asset) && !skipIds.includes(item.id))
    .sort((a, b) => (a.z || 0) - (b.z || 0));

  items.forEach(item => {
    // v11.3: 긴팔/긴바지는 기존 full-canvas를 그대로 덮지 않고
    // 기본 팔/다리/기본옷 영역을 먼저 가린 뒤 캐릭터 실루엣 전용 렌더러로 교체한다.
    const id = item.id || '';
    if (id === 'top-orange-jacket' || id === 'top-lavender-hoodie') {
      drawLongTopOcclusionMask(ctx, gender);
      drawFittedAvatarTop(ctx, id);
      drawHandsOverLongSleeves(ctx, gender);
      return;
    }
    if (id === 'bottom-charcoal-pants' || id === 'bottom-lavender-joggers') {
      drawLongBottomOcclusionMask(ctx, gender);
      drawFittedAvatarBottom(ctx, id);
      return;
    }
    // v10: 착용 미리보기는 남/여 기본 캐릭터와 같은 1145x1374 좌표계로 미리 제작한
    // full-canvas 투명 PNG를 그대로 한 번에 합성한다. 따라서 머리/손목/몸/발의 위치를
    // 런타임 x/y 비율 계산으로 다시 추정하지 않는다. 상점 카드는 기존 item.asset만 사용한다.
    if (item.fullCanvas) {
      const src = item.fullCanvas[gender === 'female' ? 'female' : 'male'];
      const overlay = loadAvatarItemSprite(src);
      if (overlay && overlay.complete && overlay.naturalWidth) {
        // v11: full-canvas 레이어도 기본 캐릭터와 완전히 같은 contain 변환을 사용한다.
        // 이전 버전은 1145x1374 레이어를 144x176에 강제 stretch해서 발/손목/머리가 어긋났다.
        const sw = overlay.naturalWidth || overlay.width;
        const sh = overlay.naturalHeight || overlay.height;
        const scale = Math.min(144 / sw, 176 / sh);
        const dw = sw * scale, dh = sh * scale;
        const dx = (144 - dw) / 2, dy = 176 - dh;
        ctx.drawImage(overlay, dx, dy, dw, dh);
      }
      return;
    }

    // 이전 데이터와의 호환용 fallback.
    const layers = item.layers || [{ asset: item.asset, placement: item.placement }];
    layers.forEach(layer => {
      const sprite = loadAvatarItemSprite(layer.asset);
      if (!sprite || !sprite.complete || !sprite.naturalWidth) return;
      const p = layer.placement && (layer.placement[gender] || layer.placement);
      if (!p) return;
      drawContainedWearable(ctx, sprite, p, item.slot);
    });
  });
}

function drawContainedWearable(ctx, sprite, p, slot) {
  // 옷/하의는 캐릭터 몸 실루엣에 맞게 지정 박스에 정확히 맞춘다.
  // 신발/손목 액세서리는 비율을 보존해 찌그러짐을 막는다.
  if (slot === 'top' || slot === 'bottom') {
    ctx.drawImage(sprite, p.x, p.y, p.w, p.h);
    return;
  }
  const sw = sprite.naturalWidth || sprite.width;
  const sh = sprite.naturalHeight || sprite.height;
  const scale = Math.min(p.w / sw, p.h / sh);
  const dw = sw * scale, dh = sh * scale;
  ctx.drawImage(sprite, p.x + (p.w - dw) / 2, p.y + (p.h - dh) / 2, dw, dh);
}

function drawFittedAvatarItem(ctx, item, gender) {
  if (!item) return false;
  const catalogItem = typeof AVATAR_ITEM_CATALOG === 'undefined' ? null : AVATAR_ITEM_CATALOG.find(entry => entry.name === item.name);
  const id = item.id || (catalogItem && catalogItem.id);
  if (!id) return false;
  if (id.startsWith('top-')) { drawFittedAvatarTop(ctx, id); return true; }
  if (id.startsWith('bottom-')) { drawFittedAvatarBottom(ctx, id); return true; }
  if (id.startsWith('shoes-')) { drawFittedAvatarShoes(ctx, id); return true; }
  if (id === 'head-headband') { drawFittedHeadband(ctx); return true; }
  if (id === 'head-cap') { drawFittedCap(ctx); return true; }
  if (id === 'head-crown') { drawFittedCrown(ctx); return true; }
  if (id === 'accessory-wristbands') { drawFittedWristbands(ctx); return true; }
  if (id === 'accessory-smartwatch') { drawFittedSmartwatch(ctx); return true; }
  if (id === 'accessory-gold-medal') { drawFittedMedal(ctx); return true; }
  return false;
}

// v11.3 긴 의류 교체용 occlusion mask.
// 기본 래스터 캐릭터의 기존 반팔/반바지 및 노출 피부가 새 긴옷 밖으로 튀어나오는 것을 막는다.
function drawLongTopOcclusionMask(ctx, gender) {
  ctx.save();
  ctx.fillStyle = '#f7fbff';
  // 좌/우 팔 안쪽을 새 소매가 덮을 영역까지만 정리한다.
  ctx.beginPath();
  ctx.roundRect(38, 68, 16, 34, 7);
  ctx.roundRect(90, 68, 16, 34, 7);
  ctx.fill();
  // 몸통의 기존 기본 티셔츠 영역.
  ctx.beginPath(); ctx.roundRect(49, 64, 46, 41, 8); ctx.fill();
  ctx.restore();
}
function drawLongBottomOcclusionMask(ctx, gender) {
  ctx.save();
  ctx.fillStyle = '#f7fbff';
  // 기존 반바지와 바지 안쪽으로 들어갈 다리 영역을 제거한다.
  ctx.beginPath(); ctx.roundRect(47, 100, 50, 52, 8); ctx.fill();
  ctx.restore();
}
function drawHandsOverLongSleeves(ctx, gender) {
  const skin = gender === 'female' ? '#ffd2b5' : '#f4c39f';
  ctx.save(); ctx.fillStyle=skin; ctx.strokeStyle='#9a674e'; ctx.lineWidth=.8;
  ctx.beginPath(); ctx.ellipse(42,99,4.5,5.5,-.15,0,Math.PI*2); ctx.fill(); ctx.stroke();
  ctx.beginPath(); ctx.ellipse(102,99,4.5,5.5,.15,0,Math.PI*2); ctx.fill(); ctx.stroke();
  ctx.restore();
}

function traceAvatarLongTop(ctx) {
  ctx.beginPath();
  ctx.moveTo(57,63); ctx.quadraticCurveTo(72,67,87,63);
  ctx.lineTo(95,67); ctx.quadraticCurveTo(100,69,102,75);
  ctx.lineTo(108,94); ctx.quadraticCurveTo(106,99,101,101);
  ctx.lineTo(94,98); ctx.lineTo(93,104);
  ctx.quadraticCurveTo(72,107,51,104); ctx.lineTo(50,98);
  ctx.lineTo(43,101); ctx.quadraticCurveTo(38,99,36,94);
  ctx.lineTo(42,75); ctx.quadraticCurveTo(44,69,49,67); ctx.closePath();
}

function traceAvatarTop(ctx) {
  ctx.beginPath();
  ctx.moveTo(58, 63);
  ctx.quadraticCurveTo(72, 67, 86, 63);
  ctx.lineTo(94, 67);
  ctx.quadraticCurveTo(98, 69, 100, 74);
  ctx.lineTo(102, 79);
  ctx.quadraticCurveTo(100, 82, 96, 83);
  ctx.lineTo(94, 78);
  ctx.lineTo(94, 102);
  ctx.quadraticCurveTo(72, 105, 50, 102);
  ctx.lineTo(50, 78);
  ctx.lineTo(48, 83);
  ctx.quadraticCurveTo(44, 82, 42, 79);
  ctx.lineTo(44, 74);
  ctx.quadraticCurveTo(46, 69, 50, 67);
  ctx.closePath();
}

function traceAvatarShorts(ctx) {
  ctx.beginPath();
  ctx.moveTo(50, 101);
  ctx.quadraticCurveTo(72, 104, 94, 101);
  ctx.lineTo(96, 121);
  ctx.quadraticCurveTo(88, 124, 79, 123);
  ctx.lineTo(72, 112);
  ctx.lineTo(65, 123);
  ctx.quadraticCurveTo(56, 124, 48, 121);
  ctx.closePath();
}

function fillAndStrokeAvatarPath(ctx, color, stroke = '#3b2a23', alpha = 0.97) {
  ctx.save();
  ctx.globalAlpha = alpha;
  ctx.fillStyle = color;
  ctx.fill();
  ctx.restore();
  ctx.save();
  ctx.strokeStyle = stroke;
  ctx.lineWidth = 1.05;
  ctx.lineJoin = 'round';
  ctx.lineCap = 'round';
  ctx.stroke();
  ctx.restore();
}

function drawFittedAvatarTop(ctx, id) {
  const isJacket = id === 'top-orange-jacket';
  const isHoodie = id === 'top-lavender-hoodie';
  const color = isJacket ? '#f47b2a' : isHoodie ? '#a995e8' : '#63d9b4';
  if (isJacket || isHoodie) traceAvatarLongTop(ctx); else traceAvatarTop(ctx);
  fillAndStrokeAvatarPath(ctx, color, isJacket ? '#8e3b15' : isHoodie ? '#58458e' : '#24745f');

  ctx.save();
  ctx.lineCap = 'round';
  if (isJacket) {
    ctx.strokeStyle = '#fff5e9'; ctx.lineWidth = 1.25;
    ctx.beginPath(); ctx.moveTo(72, 65); ctx.lineTo(72, 103); ctx.stroke();
    ctx.beginPath(); ctx.moveTo(43, 73); ctx.lineTo(49, 80); ctx.stroke();
    ctx.beginPath(); ctx.moveTo(101, 73); ctx.lineTo(95, 80); ctx.stroke();
    ctx.fillStyle = '#713118'; ctx.fillRect(70.8, 68, 2.4, 4);
  } else if (isHoodie) {
    ctx.strokeStyle = '#6853a0'; ctx.lineWidth = 1.2;
    ctx.beginPath(); ctx.arc(72, 65, 13, 0.15, Math.PI - 0.15); ctx.stroke();
    ctx.beginPath(); ctx.moveTo(67, 67); ctx.lineTo(67, 78); ctx.moveTo(77, 67); ctx.lineTo(77, 78); ctx.stroke();
    ctx.strokeRect(58, 88, 28, 10);
  } else {
    ctx.strokeStyle = 'rgba(255,255,255,.75)'; ctx.lineWidth = 1.1;
    ctx.beginPath(); ctx.moveTo(52, 68); ctx.quadraticCurveTo(55, 78, 48, 86); ctx.stroke();
    ctx.beginPath(); ctx.moveTo(92, 68); ctx.quadraticCurveTo(89, 78, 96, 86); ctx.stroke();
  }
  ctx.restore();
}

function drawFittedAvatarBottom(ctx, id) {
  if (id === 'bottom-mint-shorts') {
    traceAvatarShorts(ctx);
    fillAndStrokeAvatarPath(ctx, '#63d9b4', '#24745f');
    ctx.save();
    ctx.strokeStyle = '#e9fff8'; ctx.lineWidth = 1.1;
    ctx.beginPath(); ctx.moveTo(48, 104); ctx.lineTo(45, 123); ctx.moveTo(96, 104); ctx.lineTo(99, 123); ctx.stroke();
    ctx.strokeStyle = '#24745f'; ctx.beginPath(); ctx.moveTo(72, 104); ctx.lineTo(72, 116); ctx.stroke();
    ctx.restore();
    return;
  }

  const lavender = id === 'bottom-lavender-joggers';
  const fill = lavender ? '#a995e8' : '#343943';
  const edge = lavender ? '#58458e' : '#20242b';
  ctx.save();
  ctx.beginPath();
  ctx.moveTo(51, 101); ctx.lineTo(70.5, 101); ctx.lineTo(68, 150); ctx.lineTo(53, 150); ctx.quadraticCurveTo(51, 127, 51, 101); ctx.closePath();
  ctx.moveTo(73.5, 101); ctx.lineTo(93, 101); ctx.quadraticCurveTo(93, 127, 91, 150); ctx.lineTo(76, 150); ctx.closePath();
  ctx.fillStyle = fill; ctx.fill();
  ctx.strokeStyle = edge; ctx.lineWidth = 1.1; ctx.lineJoin = 'round'; ctx.stroke();
  ctx.fillStyle = edge; ctx.fillRect(51, 101, 42, 3.5);
  if (lavender) {
    ctx.fillRect(53, 146, 15, 4); ctx.fillRect(76, 146, 15, 4);
  } else {
    ctx.strokeStyle = '#f47b2a'; ctx.lineWidth = 1.25;
    ctx.beginPath(); ctx.moveTo(54, 105); ctx.lineTo(55, 147); ctx.moveTo(90, 105); ctx.lineTo(89, 147); ctx.stroke();
  }
  ctx.restore();
}

function traceAvatarShoe(ctx, right) {
  const mirror = x => right ? 144 - x : x;
  ctx.beginPath();
  ctx.moveTo(mirror(46), 160);
  ctx.quadraticCurveTo(mirror(47), 154, mirror(53), 151);
  ctx.quadraticCurveTo(mirror(61), 149, mirror(66), 153);
  ctx.quadraticCurveTo(mirror(69), 157, mirror(69), 165);
  ctx.quadraticCurveTo(mirror(68), 169, mirror(63), 170);
  ctx.lineTo(mirror(49), 170);
  ctx.quadraticCurveTo(mirror(45), 168, mirror(46), 160);
  ctx.closePath();
}

function drawFittedAvatarShoes(ctx, id) {
  const highTop = id === 'shoes-lavender-hightops';
  const color = highTop ? '#a995e8' : '#70d9c0';
  const edge = highTop ? '#58458e' : '#287664';
  ctx.save();
  [false, true].forEach(right => {
    if (highTop) {
      const x = right ? 77 : 50;
      ctx.fillStyle = color; ctx.strokeStyle = edge; ctx.lineWidth = 1;
      ctx.beginPath(); ctx.roundRect(x, 145, 17, 15, 4); ctx.fill(); ctx.stroke();
    }
    traceAvatarShoe(ctx, right);
    ctx.save(); ctx.globalAlpha = .82; ctx.fillStyle = color; ctx.fill(); ctx.restore();
    ctx.strokeStyle = edge; ctx.lineWidth = 1.05; ctx.stroke();
    ctx.strokeStyle = '#fff'; ctx.lineWidth = .8;
    const x1 = right ? 79 : 53, x2 = right ? 91 : 65;
    ctx.beginPath(); ctx.moveTo(x1, 157); ctx.lineTo(x2, 160); ctx.moveTo(x1, 160); ctx.lineTo(x2, 163); ctx.stroke();
  });
  ctx.restore();
}

function drawFittedHeadband(ctx) {
  ctx.save();
  ctx.beginPath();
  ctx.moveTo(45, 34); ctx.quadraticCurveTo(72, 30, 99, 34);
  ctx.lineTo(98, 40); ctx.quadraticCurveTo(72, 36, 46, 40); ctx.closePath();
  ctx.fillStyle = '#f47b20'; ctx.fill();
  ctx.strokeStyle = '#9b3f12'; ctx.lineWidth = 1; ctx.stroke();
  ctx.strokeStyle = '#fff1df'; ctx.lineWidth = .9;
  ctx.beginPath(); ctx.moveTo(47, 36); ctx.quadraticCurveTo(72, 33, 97, 36); ctx.stroke();
  ctx.restore();
}

function drawFittedCap(ctx) {
  ctx.save();
  ctx.beginPath();
  ctx.moveTo(42, 29); ctx.quadraticCurveTo(45, 8, 72, 7); ctx.quadraticCurveTo(99, 8, 103, 30);
  ctx.quadraticCurveTo(72, 36, 42, 29); ctx.closePath();
  ctx.fillStyle = '#273453'; ctx.fill(); ctx.strokeStyle = '#172039'; ctx.lineWidth = 1.2; ctx.stroke();
  ctx.beginPath(); ctx.moveTo(58, 8); ctx.quadraticCurveTo(72, 18, 72, 31); ctx.moveTo(86, 8); ctx.quadraticCurveTo(72, 18, 72, 31); ctx.stroke();
  ctx.beginPath(); ctx.moveTo(38, 31); ctx.quadraticCurveTo(72, 25, 108, 34); ctx.quadraticCurveTo(76, 40, 43, 36); ctx.closePath();
  ctx.fillStyle = '#1f2a47'; ctx.fill(); ctx.stroke();
  ctx.restore();
}

function drawFittedCrown(ctx) {
  ctx.save();
  ctx.beginPath();
  ctx.moveTo(53, 31); ctx.lineTo(52, 13); ctx.lineTo(61, 21); ctx.lineTo(67, 9);
  ctx.lineTo(73, 20); ctx.lineTo(82, 8); ctx.lineTo(86, 21); ctx.lineTo(94, 13); ctx.lineTo(92, 31); ctx.closePath();
  const g = ctx.createLinearGradient(0, 4, 0, 32); g.addColorStop(0, '#ffe58b'); g.addColorStop(1, '#e5a829');
  ctx.fillStyle = g; ctx.fill(); ctx.strokeStyle = '#8f5d0d'; ctx.lineWidth = 1.1; ctx.stroke();
  ctx.fillStyle = '#f47b2a'; ctx.beginPath(); ctx.arc(72, 24, 2.6, 0, Math.PI * 2); ctx.fill();
  ctx.restore();
}

function drawFittedWristbands(ctx) {
  ctx.save(); ctx.fillStyle = '#f47b20'; ctx.strokeStyle = '#9b3f12'; ctx.lineWidth = .8;
  ctx.beginPath(); ctx.moveTo(37, 91); ctx.lineTo(44, 89); ctx.lineTo(46, 95); ctx.lineTo(39, 97); ctx.closePath(); ctx.fill(); ctx.stroke();
  ctx.beginPath(); ctx.moveTo(100, 89); ctx.lineTo(107, 91); ctx.lineTo(105, 97); ctx.lineTo(98, 95); ctx.closePath(); ctx.fill(); ctx.stroke();
  ctx.restore();
}

function drawFittedSmartwatch(ctx) {
  ctx.save();
  ctx.fillStyle = '#273453'; ctx.strokeStyle = '#172039'; ctx.lineWidth = .8;
  ctx.beginPath(); ctx.moveTo(37.5, 89.5); ctx.lineTo(44, 88); ctx.lineTo(46.5, 97); ctx.lineTo(40, 98.5); ctx.closePath(); ctx.fill(); ctx.stroke();
  ctx.fillStyle = '#83d9d1'; ctx.beginPath(); ctx.roundRect(39.5, 91, 4.5, 5, 1); ctx.fill();
  ctx.restore();
}

function drawFittedMedal(ctx) {
  ctx.save();
  ctx.strokeStyle = '#23365c'; ctx.lineWidth = 2.2;
  ctx.beginPath(); ctx.moveTo(61, 63); ctx.lineTo(72, 79); ctx.lineTo(83, 63); ctx.stroke();
  ctx.fillStyle = '#efb437'; ctx.strokeStyle = '#8f5d0d'; ctx.lineWidth = 1;
  ctx.beginPath(); ctx.arc(72, 82, 7, 0, Math.PI * 2); ctx.fill(); ctx.stroke();
  ctx.fillStyle = '#fff1a8'; ctx.beginPath(); ctx.arc(70, 80, 2, 0, Math.PI * 2); ctx.fill();
  ctx.restore();
}
const _itemIconCache = {};
function itemIconDataURL(name) {
  const item = state.shopItems.find(entry => entry.name === name);
  if (item && item.asset) return item.asset;
  if (_itemIconCache[name]) return _itemIconCache[name];
  let seed = 2166136261;
  for (let i = 0; i < name.length; i++) { seed ^= name.charCodeAt(i); seed = Math.imul(seed, 16777619); }
  seed = seed >>> 0;
  const rnd = () => { seed = (seed + 0x6D2B79F5) | 0; let t = Math.imul(seed ^ seed >>> 15, 1 | seed); t = t + Math.imul(t ^ t >>> 7, 61 | t) ^ t; return ((t ^ t >>> 14) >>> 0) / 4294967296; };
  const U = 6, N = 8;
  const c = document.createElement('canvas'); c.width = N * U; c.height = N * U;
  const ctx = c.getContext('2d'); ctx.imageSmoothingEnabled = false;
  const hue = Math.floor(rnd() * 360);
  ctx.fillStyle = `hsl(${hue},40%,18%)`; ctx.fillRect(0, 0, N * U, N * U);
  const fg1 = `hsl(${hue},70%,55%)`, fg2 = `hsl(${(hue + 40) % 360},80%,65%)`;
  for (let y = 0; y < N; y++) {
    for (let x = 0; x < N / 2; x++) {
      if (rnd() < 0.45) {
        ctx.fillStyle = rnd() < 0.5 ? fg1 : fg2;
        ctx.fillRect(x * U, y * U, U, U);
        ctx.fillRect((N - 1 - x) * U, y * U, U, U);
      }
    }
  }
  const url = c.toDataURL();
  _itemIconCache[name] = url;
  return url;
}
// (FR-SH-001) 아래 buyItem()에서 실제 결제/포인트 차감이 필요합니다.
//   아이템 구매(buyItem) > Java 상점 API > DB 연결 > SQL UPDATE(포인트 잔액) + INSERT(보유 아이템 테이블)
//   — 포인트 차감과 아이템 지급은 하나의 트랜잭션으로 묶어야 중간 실패 시 포인트만 깎이는 사고를 막을 수 있습니다.
function groupHistoryByDate() {
  const map = {};
  state.history.forEach(h => { (map[h.date] = map[h.date] || []).push(h); });
  return Object.entries(map);
}
function getScoreBonusPct() {
  const badge = state.shopItems.find(it => it.slot === 'badge');
  if (!badge || !badge.owned || !badge.equipped) return 0;
  const m = badge.effect.match(/\+(\d+)/);
  return m ? +m[1] : 0;
}
// 예전엔 '프로필·캐릭터 꾸미기' 탭 안에 있었다 — 캐릭터 꾸미기랑은 성격이 다른 운동 성과
// 통계라 날짜별 기록이 쌓이는 이 '히스토리' 탭으로 옮겼다(renderMissionAvatar 참고).
function renderHistorySummaryCard() {
  const stats = getProfileStats();
  return `
  <div class="card">
    <p class="section-label">누적 성과</p>
    <p class="desc mono" style="margin:0;">누적 점수 <b>${stats.total.toLocaleString()} 점</b> · 동네 랭킹 <b>${stats.myRank ? '#'+stats.myRank : '-'}</b> · 레벨업까지 <b>100 exp</b></p>
    <div class="progress" style="margin-top:10px;"><span style="width:${state.user.exp}%"></span></div>
    <p class="hint" style="margin-top:4px;">Lv.${state.user.level} 진행도 ${state.user.exp}%</p>
    <p class="section-label" style="margin-top:14px;">등급 비율 (전체 세션 기준)</p>
    <div style="margin-top:8px;">
      ${renderGradeDonut([
    { label: 'PERFECT', value: stats.gc.PERFECT, color: gradeColor('PERFECT') },
    { label: 'GREAT', value: stats.gc.GREAT, color: gradeColor('GREAT') },
    { label: 'GOOD', value: stats.gc.GOOD, color: gradeColor('GOOD') },
    { label: 'MISS', value: stats.gc.MISS, color: gradeColor('MISS') },
  ], '총 횟수')}
    </div>
    <p class="section-label" style="margin-top:14px;">운동 종류별 누적 횟수</p>
    <p class="desc mono" style="margin:0;">${stats.exCounts.length ? stats.exCounts.map(([ex, cnt]) => `${ex} ${cnt}회`).join(' · ') : '아직 기록이 없습니다.'}</p>
    <p class="section-label" style="margin-top:14px;">장착 아이템 보정 효과</p>
    ${stats.activeEffects.length ? stats.activeEffects.map(e => `<span class="pill pill-accent" style="margin:0 6px 6px 0;display:inline-block;">${e}</span>`).join('') : '<p class="hint">착용 중인 능력치 아이템이 없습니다.</p>'}
  </div>`;
}
function renderHistory() {
  const groups = groupHistoryByDate();
  const bonusPct = getScoreBonusPct();
  if (!groups.length) return `${renderHistorySummaryCard()}<div class="empty-note" style="margin-top:16px;">아직 운동 기록이 없습니다.</div>`;
  return `
  <div style="display:flex;flex-direction:column;gap:16px;">
    ${renderHistorySummaryCard()}
    ${groups.map(([date, entries]) => `
      <div class="card">
        <div class="flex-between" style="margin-bottom:10px;">
          <p class="section-label" style="margin:0;">${date}</p>
          <span class="pill pill-muted">${entries.length}개 종목</span>
        </div>
        <div style="display:flex;flex-direction:column;gap:10px;">
          ${entries.map(h => {
    const gc = h.gc || { PERFECT: 0, GREAT: 0, GOOD: 0, MISS: 0 };
    const gcTotal = Object.values(gc).reduce((a, b) => a + b, 0) || 1;
    const pct = k => Math.round((gc[k] || 0) / gcTotal * 100);
    const bonus = Math.round(h.score * bonusPct / 100);
    const finalScore = h.score + bonus;
    const basePts = Math.round(h.score * 0.4);
    const ptsBonus = Math.round(basePts * bonusPct / 100);
    const finalPts = basePts + ptsBonus;
    return `
            <div style="border:1px solid var(--line);border-radius:10px;padding:12px;">
              <b>${h.ex}</b>
              <p class="desc" style="margin:6px 0;">유효 횟수 ${h.reps}회 · 전체 정확도 ${h.acc}%</p>
              <p class="desc mono" style="margin:0;">PERFECT <b style="color:var(--accent)">${pct('PERFECT')}%</b> · GREAT <b style="color:var(--gold)">${pct('GREAT')}%</b> · GOOD <b>${pct('GOOD')}%</b></p>
              <p class="desc mono" style="margin-top:8px;">획득 점수 : ${h.score}${bonusPct > 0 ? ` + 아이템효과 ${bonusPct}% = ${finalScore}` : ''}</p>
              <p class="desc mono" style="margin-top:2px;">획득 포인트 : ${basePts}${bonusPct > 0 ? ` + 아이템효과 ${bonusPct}% = ${finalPts}` : ''}</p>
            </div>`;
  }).join('')}
        </div>
      </div>`).join('')}
  </div>`;
}

async function loadExerciseHistory() {
  if (!state.token) return;
  try {
    const res = await fetch(`${API_BASE}/api/exercise-records`, {
      headers: { 'Authorization': 'Bearer ' + state.token }
    });
    const body = await res.json();
    if (!body.success) return;
    state.history = body.data.map(r => {
      const d = new Date(r.recordedAt);
      const date = `${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`;
      return {
        date, ex: r.exerciseType, reps: r.reps, acc: r.accuracy, score: r.score, grade: r.grade,
        gc: { PERFECT: r.perfectCount, GREAT: r.greatCount, GOOD: r.goodCount, MISS: r.missCount },
      };
    });
  } catch (err) {
    console.error('운동 히스토리 불러오기 실패', err);
  }
}

async function loadMyCrew() {
  if (!state.token) return;
  try {
    const res = await fetch(`${API_BASE}/api/crews/me`, {
      headers: { 'Authorization': 'Bearer ' + state.token }
    });
    const body = await res.json();
    if (!body.success) return;
    const c = body.data;
    if (!c) {
      state.crew.created = false;
      state.crew.leaderRegion = '';
      state.crew.myDongRank = null;
      return;
    }
    state.crew.id = c.id;
    state.crew.created = true;
    state.crew.joinEnabled = c.joinEnabled !== false && c.recruiting !== false;
    state.crew.autoApprove = c.autoApprove === true;
    state.crew.name = c.name;
    state.crew.desc = c.description;
    state.crew.concepts = typeof normalizeCrewConcepts === 'function' ? normalizeCrewConcepts(c.concepts, c.concept) : (Array.isArray(c.concepts) ? c.concepts : [c.concept].filter(Boolean));
    state.crew.region = c.region || '';
    const leaderDto = c.leader || (c.leaderId ? {
      userId: c.leaderId, nickname: c.leaderNickname, level: c.leaderLevel, points: c.leaderPoints, role: 'LEADER',
      region: c.leaderRegion
    } : null);
    state.crew.leaderRegion = (leaderDto && leaderDto.region) || c.leaderRegion || c.region || '';
    state.crew.level = c.level;
    state.crew.exp = c.exp;
    state.crew.groupMission = { ex: c.groupMissionExercise, target: c.groupMissionTarget, progress: c.groupMissionCurrent };
    const apiMembers = Array.isArray(c.members) ? c.members : [];
    const completeMembers = leaderDto && !apiMembers.some(m => Number(m.userId) === Number(leaderDto.userId))
      ? [leaderDto, ...apiMembers] : apiMembers;
    state.crew.members = completeMembers.map(m => ({
      userId: m.userId, n: m.nickname, role: m.role === 'LEADER' ? '팀장' : '팀원', level: m.level, score: m.points
    }));
    await loadCrewBattleContributions();
    if (typeof loadMyDongCrewRank === 'function') await loadMyDongCrewRank();
  } catch (err) {
    console.error('내 크루 정보 불러오기 실패', err);
  }
}
// 크루 메인 화면의 "크루대전 기여도" 랭킹은 각 크루원의 포인트(m.points)가 아니라, 그 크루원이
// 실제로 참가한 크루대전에서 쌓은 누적 점수(CrewBattleContribution)여야 한다 — 이걸 불러와서
// state.crew.members[].score를 실제 기여도 값으로 덮어쓴다. 아직 한 번도 대전에 참가하지 않은
// 크루원은 서버 응답에 없으므로 0점으로 남는다.
async function loadCrewBattleContributions() {
  if (!state.token) return;
  try {
    const res = await fetch(`${API_BASE}/api/crews/me/battle-contributions`, {
      headers: { 'Authorization': 'Bearer ' + state.token }
    });
    const body = await res.json();
    if (!body.success) return;
    const byUserId = new Map(body.data.map(c => [Number(c.userId), c.totalScore]));
    state.crew.members.forEach(m => { m.score = byUserId.get(Number(m.userId)) || 0; });
  } catch (err) {
    console.error('크루대전 기여도 불러오기 실패', err);
  }
}



/* ========================================================================
   고객센터 · 불편사항접수
   ======================================================================== */
// (FR-CS-001) 티켓 접수/조회는 관리자용 API가 함께 필요한 구간입니다.
//   불편사항 접수(submitTicket) > Java 고객센터 API > DB 연결 > SQL INSERT(티켓 테이블)
//   운영팀 답변 등록도 같은 API에서 SQL UPDATE(티켓 테이블 reply, status 컬럼)로 처리하면 됩니다.
function renderSetAccount() {
  const a = state.settings.account;
  const cities = Object.keys(REGION_DATA);
  const city = REGION_DATA[a.regionCity] ? a.regionCity : cities[0];
  const gus = Object.keys(REGION_DATA[city]);
  const gu = REGION_DATA[city][a.regionGu] ? a.regionGu : gus[0];
  const dongs = REGION_DATA[city][gu];
  const dong = dongs.includes(a.regionDong) ? a.regionDong : dongs[0];
  // 화면엔 항상 유효한 기본값이 보이지만(드롭다운 fallback), 여긴 그냥 표시용 계산이라 실제
  // state.settings.account 쪽은 그대로 null일 수 있었다 — 그 상태로 드롭다운을 안 건드리고
  // 바로 "저장"을 누르면 서버에 null이 그대로 전송되고, 서버는 null을 무시하니 지역이 영영
  // 저장되지 않았다(계정 생성 시 지역이 비어있던 사용자가 겪은 "동네 undefined" 버그의 원인).
  // 화면에 보이는 값과 실제 저장될 값을 여기서 맞춰둔다.
  a.regionCity = city; a.regionGu = gu; a.regionDong = dong;
  const canEditNick = (state.user.nicknameTickets || 0) > 0;
  return `
  <div style="max-width:640px;margin:0 auto;">
    <div class="card">
      <p class="section-label">프로필</p>
      <div class="field">
        <label for="acc-nick">닉네임</label>
        <input id="acc-nick" value="${state.user.nickname}" ${canEditNick ? '' : 'disabled'}>
        <p class="hint">${canEditNick ? `닉네임 변경권 보유중 · 저장 시 1장이 사용됩니다 (남은 수량 ${state.user.nicknameTickets}장)` : `닉네임 변경은 포인트 상점에서 '닉네임 변경권'을 구매한 뒤 가능합니다.`}</p>
        ${canEditNick ? '' : '<button class="btn btn-sm btn-secondary" style="margin-top:6px;" onclick="setMenu(\'shop\')">포인트 상점으로 이동</button>'}
      </div>
      <div class="field">
        <label>활동 지역</label>
        <div class="field-row">
          <select onchange="setAccountCity(this.value)" style="flex:1;min-width:0;">${cities.map(c => `<option ${c === city ? 'selected' : ''}>${c}</option>`).join('')}</select>
          <select onchange="setAccountGu(this.value)" style="flex:1;min-width:0;">${gus.map(g => `<option ${g === gu ? 'selected' : ''}>${g}</option>`).join('')}</select>
          <select onchange="setAccountDong(this.value)" style="flex:1;min-width:0;">${dongs.map(d => `<option ${d === dong ? 'selected' : ''}>${d}</option>`).join('')}</select>
        </div>
      </div>
      <button class="btn btn-primary" onclick="saveAccount()">저장</button>
    </div>
    <div style="margin-top:20px;">${renderSetPrivacy()}</div>
    <div style="margin-top:20px;">${renderSetCalib()}</div>
    <div style="margin-top:20px;">${renderSetLogout()}</div>
  </div>`;
}
// 공개로 두면 랭킹 단상 아바타를 클릭한 다른 사용자가 내 레벨·자기소개·누적성과·등급비율·
// 운동별 누적횟수를 볼 수 있다(PublicProfileController 참고). 비공개면 서버가 nickname 외엔
// 아예 안 내려주니, 프론트에서 막는 게 아니라 진짜로 안 보인다.
function renderSetPrivacy() {
  const isPublic = state.settings.account.profilePublic !== false;
  return `
  <div class="card">
    <p class="section-label">프로필 공개 설정</p>
    <p class="desc">공개로 설정하면 랭킹에서 다른 사용자가 내 프로필(레벨·자기소개·누적성과·등급비율·운동별 누적횟수)을 확인할 수 있어요.</p>
    <div style="display:flex;gap:8px;">
      <button class="btn btn-sm ${isPublic ? 'btn-primary' : 'btn-secondary'}" onclick="setProfilePublic(true)">공개</button>
      <button class="btn btn-sm ${!isPublic ? 'btn-primary' : 'btn-secondary'}" onclick="setProfilePublic(false)">비공개</button>
    </div>
  </div>`;
}
async function setProfilePublic(pub) {
  if (state.settings.account.profilePublic === pub) return;
  try {
    const res = await fetch(`${API_BASE}/api/users/me`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + state.token },
      body: JSON.stringify({ profilePublic: pub })
    });
    const body = await res.json();
    if (!body.success) { toast(body.message || '저장에 실패했습니다'); return; }
    state.settings.account.profilePublic = pub;
    toast(pub ? '프로필을 공개로 설정했습니다' : '프로필을 비공개로 설정했습니다');
    render();
  } catch (err) {
    toast('서버에 연결할 수 없습니다');
  }
}
function setAccountCity(v) { state.settings.account.regionCity = v; state.settings.account.regionGu = null; state.settings.account.regionDong = null; render(); }
function setAccountGu(v) { state.settings.account.regionGu = v; state.settings.account.regionDong = null; render(); }
function setAccountDong(v) { state.settings.account.regionDong = v; render(); }
async function saveAccount() {
  const a = state.settings.account;
  let nickMsg = '';
  let nicknameToSave = state.user.nickname;
  const nickEl = document.getElementById('acc-nick');
  if (nickEl && !nickEl.disabled) {
    const newNick = nickEl.value.trim();
    if (newNick && newNick !== state.user.nickname) { nicknameToSave = newNick; }
  }
  try {
    const res = await fetch(`${API_BASE}/api/users/me`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + state.token },
      body: JSON.stringify({
        nickname: nicknameToSave,
        regionCity: a.regionCity, regionGu: a.regionGu, regionDong: a.regionDong,
        bio: state.user.bio || '',
      })
    });
    const body = await res.json();
    if (!body.success) { toast(body.message || '저장에 실패했습니다'); return; }
    if (nicknameToSave !== state.user.nickname && state.user.nicknameTickets > 0) {
      state.user.nicknameTickets--;
      nickMsg = ` · 닉네임 변경 (남은 변경권 ${state.user.nicknameTickets}장)`;
    }
    state.user.nickname = body.data.nickname;
    // regionCity 등이 서버에 null로 남아있으면 응답 JSON에 해당 키 자체가 없다(전역 Jackson
    // 설정이 null 필드를 생략함) — 그대로 템플릿에 넣으면 "undefined undefined undefined"가
    // 찍힌다. loadMyProfile()과 동일하게 셋 다 있을 때만 조합한다.
    const d = body.data;
    state.user.region = (d.regionCity && d.regionGu && d.regionDong) ? `${d.regionCity} ${d.regionGu} ${d.regionDong}` : '';
    a.nickname = state.user.nickname;
    toast(`프로필이 저장되었습니다${nickMsg}`);
    render();
  } catch (err) {
    toast('서버에 연결할 수 없습니다 (백엔드가 켜져 있는지 확인해주세요)');
  }
}

function renderSetCalib() {
  return `
  <div class="card">
    <p class="section-label">카메라 캘리브레이션</p>
    <p class="desc">촬영 각도·거리·신체 비율을 다시 측정하여 분석 정확도를 갱신합니다.</p>
    <button class="btn btn-secondary btn-block" onclick="openCalibrationModal()">캘리브레이션 다시 진행</button>
  </div>`;
}
function renderSetLogout() {
  return `
  <div class="grid grid-2">
    <div class="card">
      <p class="section-label">로그아웃</p>
      <p class="desc">현재 계정에서 로그아웃합니다.</p>
      <button class="btn btn-secondary" onclick="doLogout()">로그아웃</button>
    </div>
    <div class="card">
      <p class="section-label">회원 탈퇴</p>
      <p class="desc">모든 운동 기록과 포인트가 삭제되며 복구할 수 없습니다.</p>
      <button class="btn btn-danger" onclick="openWithdrawConfirm()">회원 탈퇴</button>
    </div>
  </div>`;
}
// 예전엔 화면만 login으로 바꾸고 토큰은 그대로 메모리에 남아있어서, 로그아웃해도 실제로는
// 로그인 상태 그대로였다(거기다 새로고침 자동복원까지 생기면 로그아웃 자체가 무의미해짐).
// 토큰·저장된 세션을 전부 지우고 랜딩페이지로 보내야 진짜 로그아웃이다.
function doLogout() {
  disconnectCrewChat(); disconnectJoinWait();
  clearSession();
  state.token = null;
  state.user.id = null;
  state.guestMode = false;
  state.screen = 'intro';
  state.menu = 'main';
  // state.user는 로그인 여부와 무관하게 항상 존재하는 하나의 공유 객체라, 로그아웃해도
  // calibration 값이 그대로 남아있었다 — 그 상태로 "지금 체험하기"(비회원) 카드를 누르면
  // 실제로는 캘리브레이션을 한 적 없는 비회원인데도 운동 시작하기가 바로 눌려버렸다.
  // 로그아웃 시점에 지워서 다음 비회원 체험이 항상 캘리브레이션부터 다시 요구하게 한다.
  state.user.calibration = null;
  // "동네를 설정해주세요" 같은 확인창은 로그인 중이던 계정 얘기라 로그아웃하면 의미가
  // 없어지는데, 안 지우면 render()가 화면(screen)과 무관하게 계속 띄워서 랜딩페이지
  // 위에까지 남아있었다.
  state.confirm = null;
  render();
}
// 예전엔 그냥 예/아니오 확인창이라 실수로 눌러도 바로 탈퇴됐다 — 되돌릴 수 없는 동작이라
// 내 닉네임을 정확히 입력해야만 탈퇴 버튼이 눌리도록 바꿨다(state.withdrawConfirm).
function openWithdrawConfirm() {
  state.withdrawConfirm = { open: true, input: '' };
  render();
}
function closeWithdrawConfirm() {
  state.withdrawConfirm = { open: false, input: '' };
  render();
}
// 매 글자마다 render()를 다시 부르면 입력창 포커스/커서 위치가 날아가므로, 버튼 활성화
// 상태만 DOM에서 직접 패치한다(updatePartyStatusModal 등과 같은 방식).
function setWithdrawConfirmInput(v) {
  state.withdrawConfirm.input = v;
  const btn = document.getElementById('withdraw-confirm-btn');
  if (!btn) return;
  const matched = v === state.user.nickname;
  btn.disabled = !matched;
  btn.style.opacity = matched ? '1' : '.5';
  btn.style.cursor = matched ? 'pointer' : 'not-allowed';
}
function confirmWithdraw() {
  if (state.withdrawConfirm.input !== state.user.nickname) return;
  closeWithdrawConfirm();
  doWithdraw();
}
function renderWithdrawConfirmModal() {
  const w = state.withdrawConfirm;
  if (!w.open) return '';
  const matched = w.input === state.user.nickname;
  return `
  <div class="confirm-backdrop" onclick="if(event.target===this) closeWithdrawConfirm()">
    <div class="confirm-box" style="max-width:380px;">
      <h3 style="margin:0 0 8px;">회원탈퇴</h3>
      <p class="desc" style="margin:0 0 12px;white-space:pre-line;">회원탈퇴를 진행하면 현재 계정으로 이용하던 서비스가 종료됩니다.
탈퇴 시 처리되는 정보는 현재 서비스의 회원탈퇴 정책과 API 응답을 기준으로 적용됩니다.

탈퇴 후에는 현재 계정으로 이용하던 기능과 정보를 다시 이용하기 어려울 수 있습니다.</p>
      <div class="field" style="margin-bottom:0;">
        <label for="withdraw-confirm-input">계속하려면 내 닉네임 <b>${escapeHtml(state.user.nickname)}</b>을(를) 그대로 입력하세요</label>
        <input id="withdraw-confirm-input" value="${escapeHtml(w.input)}" placeholder="${escapeHtml(state.user.nickname)}" oninput="setWithdrawConfirmInput(this.value)" autocomplete="off">
      </div>
      <div class="confirm-actions" style="margin-top:14px;">
        <button class="btn btn-ghost btn-sm" onclick="closeWithdrawConfirm()">취소</button>
        <button id="withdraw-confirm-btn" class="btn btn-danger btn-sm" ${matched ? '' : 'disabled style="opacity:.5;cursor:not-allowed;"'} onclick="confirmWithdraw()">회원탈퇴</button>
      </div>
    </div>
  </div>`;
}
async function doWithdraw() {
  try {
    const res = await fetch(`${API_BASE}/api/users/me`, {
      method: 'DELETE',
      headers: { 'Authorization': 'Bearer ' + state.token }
    });
    const body = await res.json();
    if (!body.success) { toast(body.message || '회원 탈퇴에 실패했습니다'); return; }
  } catch (err) {
    toast('서버에 연결할 수 없습니다 (백엔드가 켜져 있는지 확인해주세요)');
    return;
  }
  disconnectCrewChat(); disconnectJoinWait();
  clearSession(); // 탈퇴한 계정 토큰으로 새로고침 시 자동 로그인되는 걸 막는다
  toast('회원 탈퇴가 완료되었습니다');
  setTimeout(() => {
    location.reload();
  }, 900);
}

/* ---------- confirm dialog ---------- */
