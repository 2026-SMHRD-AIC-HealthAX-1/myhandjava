// avatar-renderers.js — 포인트 상점 아이템을 캐릭터 몸에 맞춰 그리는 부위별 렌더러.
//
// 모든 좌표는 프로필 캐릭터의 144 x 176 논리 캔버스를 기준으로 합니다.
// 상품 원본 PNG를 직사각형으로 늘리지 않고, 상의·하의·신발·헤어·손목 등 각 부위의
// 실제 윤곽을 따라 그립니다. 따라서 상점의 "착용해보기"와 마이페이지의 실제 착용 결과가
// 동일하게 보입니다.

function avatarItemId(item) {
  if (!item) return '';
  if (item.id) return item.id;
  const found = typeof AVATAR_ITEM_CATALOG === 'undefined'
    ? null
    : AVATAR_ITEM_CATALOG.find(entry => entry.name === item.name);
  return found ? found.id : '';
}

function avatarPathPaint(ctx, fill, stroke = '#3b2a23', alpha = 0.96) {
  ctx.save();
  ctx.globalAlpha = alpha;
  ctx.fillStyle = fill;
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

function avatarDrawBaseSpriteRegion(ctx, gender, makeClip) {
  const sprite = CHAR_SPRITES[gender === 'female' ? 'female' : 'male'];
  if (!sprite || !sprite.complete || !sprite.naturalWidth) return;
  ctx.save();
  ctx.beginPath();
  makeClip(ctx);
  ctx.clip();
  const sw = sprite.naturalWidth || sprite.width;
  const sh = sprite.naturalHeight || sprite.height;
  const scale = Math.min(144 / sw, 176 / sh);
  const dw = sw * scale;
  const dh = sh * scale;
  ctx.drawImage(sprite, (144 - dw) / 2, 176 - dh, dw, dh);
  ctx.restore();
}

// 헤어밴드 위로 원래 캐릭터의 앞머리만 다시 그립니다.
// 밴드 전체를 머리카락 위에 붙이는 대신 중앙 앞머리가 밴드를 가려 실제 착용처럼 보입니다.
function avatarRedrawFrontHair(ctx, gender) {
  avatarDrawBaseSpriteRegion(ctx, gender, path => {
    if (gender === 'female') {
      path.moveTo(54, 22); path.lineTo(82, 22); path.lineTo(82, 39);
      path.quadraticCurveTo(76, 34, 72, 40);
      path.quadraticCurveTo(65, 31, 60, 41);
      path.quadraticCurveTo(57, 35, 54, 39); path.closePath();
    } else {
      path.moveTo(59, 22); path.lineTo(88, 22); path.lineTo(88, 39);
      path.quadraticCurveTo(83, 34, 79, 40);
      path.quadraticCurveTo(72, 31, 67, 41);
      path.quadraticCurveTo(63, 35, 59, 40); path.closePath();
    }
  });
}

// ---------------------------------------------------------------------------
// 헤어/머리
// ---------------------------------------------------------------------------
function avatarDrawHeadband(ctx, gender) {
  ctx.save();
  ctx.beginPath();
  ctx.moveTo(45, 30);
  ctx.quadraticCurveTo(72, 26, 99, 30);
  ctx.lineTo(98, 36);
  ctx.quadraticCurveTo(72, 32, 46, 36);
  ctx.closePath();
  avatarPathPaint(ctx, '#f36f18', '#963a12');
  ctx.strokeStyle = '#fff4e9'; ctx.lineWidth = 1.15;
  ctx.beginPath(); ctx.moveTo(47, 32); ctx.quadraticCurveTo(72, 28.5, 97, 32); ctx.stroke();
  ctx.restore();
  avatarRedrawFrontHair(ctx, gender);
}

function avatarDrawCap(ctx) {
  ctx.save();
  ctx.beginPath();
  ctx.moveTo(41, 28); ctx.quadraticCurveTo(45, 7, 72, 6);
  ctx.quadraticCurveTo(100, 7, 104, 29); ctx.quadraticCurveTo(72, 35, 41, 28); ctx.closePath();
  avatarPathPaint(ctx, '#273858', '#152139');
  ctx.strokeStyle = '#8191b4'; ctx.lineWidth = .75;
  ctx.beginPath(); ctx.moveTo(58, 8); ctx.quadraticCurveTo(71, 17, 72, 31);
  ctx.moveTo(86, 8); ctx.quadraticCurveTo(74, 17, 72, 31); ctx.stroke();
  ctx.beginPath(); ctx.moveTo(38, 31); ctx.quadraticCurveTo(72, 25, 108, 34);
  ctx.quadraticCurveTo(77, 40, 43, 36); ctx.closePath();
  avatarPathPaint(ctx, '#202e4c', '#152139');
  ctx.restore();
}

function avatarDrawCrown(ctx) {
  ctx.save();
  ctx.beginPath();
  ctx.moveTo(49, 30); ctx.lineTo(47, 9); ctx.lineTo(58, 19); ctx.lineTo(66, 4);
  ctx.lineTo(73, 18); ctx.lineTo(84, 3); ctx.lineTo(89, 19); ctx.lineTo(99, 9);
  ctx.lineTo(96, 31); ctx.closePath();
  const gold = ctx.createLinearGradient(0, 4, 0, 32);
  gold.addColorStop(0, '#ffe994'); gold.addColorStop(1, '#e4a323');
  avatarPathPaint(ctx, gold, '#8a590d');
  ctx.fillStyle = '#f36f18'; ctx.beginPath(); ctx.arc(72, 23, 3, 0, Math.PI * 2); ctx.fill();
  ctx.restore();
}

function avatarDrawVisor(ctx, gender) {
  ctx.save();
  ctx.beginPath();
  ctx.moveTo(42, 29); ctx.quadraticCurveTo(72, 25, 102, 29);
  ctx.lineTo(101, 35); ctx.quadraticCurveTo(72, 31, 43, 35); ctx.closePath();
  avatarPathPaint(ctx, '#f8f8f4', '#283654');
  ctx.beginPath(); ctx.moveTo(60, 34); ctx.quadraticCurveTo(86, 31, 108, 37);
  ctx.quadraticCurveTo(88, 41, 64, 38); ctx.closePath();
  avatarPathPaint(ctx, '#f5f5f0', '#283654');
  ctx.restore();
  avatarRedrawFrontHair(ctx, gender);
}

function avatarDrawScrunchie(ctx, gender) {
  if (gender !== 'female') return;
  ctx.save(); ctx.fillStyle = '#ef7fb4'; ctx.strokeStyle = '#8f3563'; ctx.lineWidth = 1;
  ctx.beginPath();
  for (let i = 0; i < 12; i++) {
    const a = i * Math.PI / 6;
    const r = i % 2 ? 4 : 6;
    const x = 101 + Math.cos(a) * r, y = 42 + Math.sin(a) * r;
    i ? ctx.lineTo(x, y) : ctx.moveTo(x, y);
  }
  ctx.closePath(); ctx.fill(); ctx.stroke();
  ctx.fillStyle = '#6f2c4c'; ctx.beginPath(); ctx.arc(101, 42, 2, 0, Math.PI * 2); ctx.fill();
  ctx.restore();
}

function avatarDrawSunglasses(ctx) {
  ctx.save(); ctx.lineWidth = 1.2; ctx.strokeStyle = '#17233a'; ctx.fillStyle = 'rgba(50,110,150,.78)';
  ctx.beginPath(); ctx.roundRect(50, 39, 18, 10, 4); ctx.fill(); ctx.stroke();
  ctx.beginPath(); ctx.roundRect(76, 39, 18, 10, 4); ctx.fill(); ctx.stroke();
  ctx.beginPath(); ctx.moveTo(68, 42); ctx.quadraticCurveTo(72, 39, 76, 42); ctx.stroke();
  ctx.beginPath(); ctx.moveTo(50, 41); ctx.lineTo(45, 39); ctx.moveTo(94, 41); ctx.lineTo(99, 39); ctx.stroke();
  ctx.restore();
}

// ---------------------------------------------------------------------------
// 상의/아우터
// ---------------------------------------------------------------------------
function avatarTraceShortTop(ctx) {
  ctx.beginPath();
  ctx.moveTo(57, 62); ctx.quadraticCurveTo(72, 66, 87, 62);
  ctx.lineTo(98, 67); ctx.lineTo(109, 79); ctx.quadraticCurveTo(107, 84, 102, 87);
  ctx.lineTo(98, 83); ctx.lineTo(98, 102); ctx.quadraticCurveTo(72, 106, 46, 102);
  ctx.lineTo(46, 83); ctx.lineTo(41, 87); ctx.quadraticCurveTo(37, 84, 35, 79);
  ctx.lineTo(47, 67); ctx.closePath();
}

function avatarTraceLongTop(ctx) {
  ctx.beginPath();
  ctx.moveTo(57, 62); ctx.quadraticCurveTo(72, 66, 87, 62);
  ctx.lineTo(98, 67); ctx.lineTo(105, 78); ctx.lineTo(108, 96);
  ctx.quadraticCurveTo(104, 100, 98, 99); ctx.lineTo(92, 77);
  ctx.lineTo(98, 102); ctx.quadraticCurveTo(72, 106, 46, 102);
  ctx.lineTo(52, 77); ctx.lineTo(46, 99); ctx.quadraticCurveTo(40, 100, 36, 96);
  ctx.lineTo(39, 78); ctx.lineTo(47, 67); ctx.closePath();
}

function avatarDrawTop(ctx, id) {
  const palette = {
    'top-mint-shirt': ['#62d9b2', '#24735d'],
    'top-orange-jacket': ['#f47a25', '#913b16'],
    'top-lavender-hoodie': ['#aa98e9', '#5d4a92'],
    'top-white-tank': ['#fafaf7', '#827d76'],
    'top-navy-longsleeve': ['#283956', '#17233a'],
  };
  const colors = palette[id] || palette['top-mint-shirt'];
  ctx.save();
  if (id === 'top-white-tank') {
    ctx.beginPath();
    ctx.moveTo(59, 63); ctx.quadraticCurveTo(64, 69, 72, 69); ctx.quadraticCurveTo(80, 69, 85, 63);
    ctx.lineTo(92, 68); ctx.lineTo(96, 102); ctx.quadraticCurveTo(72, 105, 48, 102);
    ctx.lineTo(52, 68); ctx.closePath();
  } else if (id === 'top-navy-longsleeve') avatarTraceLongTop(ctx);
  else avatarTraceShortTop(ctx);
  avatarPathPaint(ctx, colors[0], colors[1]);

  ctx.lineCap = 'round';
  if (id === 'top-orange-jacket') {
    ctx.strokeStyle = '#fff4e8'; ctx.lineWidth = 1.2;
    ctx.beginPath(); ctx.moveTo(72, 65); ctx.lineTo(72, 103); ctx.stroke();
    ctx.beginPath(); ctx.moveTo(43, 73); ctx.lineTo(50, 80); ctx.moveTo(101, 73); ctx.lineTo(94, 80); ctx.stroke();
    ctx.fillStyle = '#743216'; ctx.fillRect(70.8, 68, 2.4, 4);
  } else if (id === 'top-lavender-hoodie') {
    ctx.strokeStyle = '#66539b'; ctx.lineWidth = 1.1;
    ctx.beginPath(); ctx.arc(72, 65, 13, .15, Math.PI - .15); ctx.stroke();
    ctx.beginPath(); ctx.moveTo(67, 67); ctx.lineTo(67, 78); ctx.moveTo(77, 67); ctx.lineTo(77, 78); ctx.stroke();
    ctx.strokeRect(58, 88, 28, 10);
  } else if (id === 'top-navy-longsleeve') {
    ctx.strokeStyle = '#92a6c3'; ctx.lineWidth = .9;
    ctx.beginPath(); ctx.moveTo(52, 68); ctx.lineTo(42, 96); ctx.moveTo(92, 68); ctx.lineTo(102, 96); ctx.stroke();
  } else {
    ctx.strokeStyle = 'rgba(255,255,255,.72)'; ctx.lineWidth = 1;
    ctx.beginPath(); ctx.moveTo(53, 68); ctx.quadraticCurveTo(57, 78, 49, 86); ctx.stroke();
    ctx.beginPath(); ctx.moveTo(91, 68); ctx.quadraticCurveTo(87, 78, 95, 86); ctx.stroke();
  }
  ctx.restore();
}

function avatarDrawOuterVest(ctx) {
  ctx.save();
  ctx.beginPath();
  ctx.moveTo(58, 64); ctx.quadraticCurveTo(64, 69, 72, 69); ctx.quadraticCurveTo(80, 69, 86, 64);
  ctx.lineTo(92, 68); ctx.lineTo(94, 101); ctx.quadraticCurveTo(72, 104, 50, 101);
  ctx.lineTo(52, 68); ctx.closePath();
  avatarPathPaint(ctx, 'rgba(244,122,37,.92)', '#913b16');
  ctx.strokeStyle = '#fff4e8'; ctx.lineWidth = 1.1;
  ctx.beginPath(); ctx.moveTo(72, 69); ctx.lineTo(72, 102); ctx.stroke();
  ctx.restore();
}

// ---------------------------------------------------------------------------
// 하의/양말/보호대/신발
// ---------------------------------------------------------------------------
function avatarTraceShorts(ctx) {
  ctx.beginPath();
  ctx.moveTo(46, 100); ctx.quadraticCurveTo(72, 103, 98, 100);
  ctx.lineTo(101, 124); ctx.quadraticCurveTo(91, 128, 80, 128);
  ctx.lineTo(72, 112); ctx.lineTo(64, 128); ctx.quadraticCurveTo(53, 128, 43, 124); ctx.closePath();
}

function avatarDrawBottom(ctx, id) {
  ctx.save();
  if (id === 'bottom-mint-shorts') {
    avatarTraceShorts(ctx); avatarPathPaint(ctx, '#62d9b2', '#24735d');
    ctx.strokeStyle = '#effff9'; ctx.lineWidth = 1.05;
    ctx.beginPath(); ctx.moveTo(48, 103); ctx.lineTo(45, 122); ctx.moveTo(96, 103); ctx.lineTo(99, 122); ctx.stroke();
    ctx.strokeStyle = '#24735d'; ctx.beginPath(); ctx.moveTo(72, 103); ctx.lineTo(72, 115); ctx.stroke();
    ctx.restore(); return;
  }
  const colors = {
    'bottom-charcoal-pants': ['#343943', '#20242b', '#f47a25'],
    'bottom-lavender-joggers': ['#aa98e9', '#5d4a92', '#7d6abe'],
    'bottom-black-leggings': ['#20242d', '#11151c', '#515a68'],
    'bottom-grey-training-pants': ['#707780', '#3d434b', '#aab0b6'],
  }[id] || ['#343943', '#20242b', '#f47a25'];
  const slim = id === 'bottom-black-leggings';
  ctx.beginPath();
  ctx.moveTo(46, 100); ctx.lineTo(71, 100); ctx.lineTo(slim ? 66 : 68, 150); ctx.lineTo(50, 150); ctx.closePath();
  ctx.moveTo(73, 100); ctx.lineTo(98, 100); ctx.lineTo(94, 150); ctx.lineTo(slim ? 78 : 76, 150); ctx.closePath();
  avatarPathPaint(ctx, colors[0], colors[1]);
  ctx.fillStyle = colors[1]; ctx.fillRect(47, 100, 50, 4);
  ctx.strokeStyle = colors[2]; ctx.lineWidth = 1.15;
  ctx.beginPath(); ctx.moveTo(51, 104); ctx.lineTo(53, 147); ctx.moveTo(93, 104); ctx.lineTo(91, 147); ctx.stroke();
  if (id === 'bottom-lavender-joggers') {
    ctx.fillStyle = colors[1]; ctx.fillRect(50, 146, 18, 5); ctx.fillRect(76, 146, 18, 5);
  }
  ctx.restore();
}

function avatarDrawSocks(ctx, id) {
  const orange = id === 'socks-orange-crew';
  ctx.save(); ctx.fillStyle = orange ? '#f47a25' : '#fafaf7'; ctx.strokeStyle = orange ? '#913b16' : '#aaa49c'; ctx.lineWidth = .8;
  const top = orange ? 132 : 139;
  [[49, 69], [75, 95]].forEach(([x1, x2]) => {
    ctx.beginPath(); ctx.moveTo(x1, top); ctx.lineTo(x2, top); ctx.lineTo(x2 - 2, 151); ctx.lineTo(x1 + 2, 151); ctx.closePath(); ctx.fill(); ctx.stroke();
  });
  if (orange) {
    ctx.strokeStyle = '#fff4e8'; ctx.lineWidth = 1;
    ctx.beginPath(); ctx.moveTo(50, 136); ctx.lineTo(68, 136); ctx.moveTo(76, 136); ctx.lineTo(94, 136); ctx.stroke();
  }
  ctx.restore();
}

function avatarTraceShoe(ctx, right) {
  const mirror = x => right ? 144 - x : x;
  ctx.beginPath();
  ctx.moveTo(mirror(38), 160); ctx.quadraticCurveTo(mirror(39), 154, mirror(45), 151);
  ctx.quadraticCurveTo(mirror(53), 148, mirror(59), 152); ctx.quadraticCurveTo(mirror(62), 156, mirror(63), 164);
  ctx.quadraticCurveTo(mirror(62), 168, mirror(57), 169); ctx.lineTo(mirror(41), 169);
  ctx.quadraticCurveTo(mirror(37), 167, mirror(38), 160); ctx.closePath();
}

function avatarDrawShoes(ctx, id) {
  if (id === 'shoes-orange-runners') return; // 기본 캐릭터에 이미 자연스럽게 착용되어 있음
  const high = id === 'shoes-lavender-hightops';
  const fill = high ? '#aa98e9' : '#65d5b6';
  const edge = high ? '#5d4a92' : '#287461';
  ctx.save();
  [false, true].forEach(right => {
    if (high) {
      const x = right ? 86 : 41;
      ctx.fillStyle = fill; ctx.strokeStyle = edge; ctx.lineWidth = 1;
      ctx.beginPath(); ctx.roundRect(x, 144, 17, 15, 4); ctx.fill(); ctx.stroke();
    }
    avatarTraceShoe(ctx, right); avatarPathPaint(ctx, fill, edge, .9);
    ctx.strokeStyle = '#fff'; ctx.lineWidth = .85;
    const x1 = right ? 88 : 43, x2 = right ? 100 : 56;
    ctx.beginPath(); ctx.moveTo(x1, 155); ctx.lineTo(x2, 160); ctx.moveTo(x1, 159); ctx.lineTo(x2, 164); ctx.stroke();
  });
  ctx.restore();
}

function avatarDrawLegwear(ctx, id) {
  ctx.save();
  if (id === 'legwear-black-kneepads') {
    ctx.fillStyle = '#20242d'; ctx.strokeStyle = '#11151c'; ctx.lineWidth = .9;
    [[50, 121], [78, 121]].forEach(([x, y]) => { ctx.beginPath(); ctx.roundRect(x, y, 16, 9, 3); ctx.fill(); ctx.stroke(); });
  } else {
    ctx.fillStyle = '#f47a25'; ctx.strokeStyle = '#913b16'; ctx.lineWidth = .9;
    [[50, 130], [78, 130]].forEach(([x, y]) => { ctx.beginPath(); ctx.roundRect(x, y, 16, 15, 4); ctx.fill(); ctx.stroke(); });
    ctx.strokeStyle = '#fff4e8'; ctx.beginPath(); ctx.moveTo(51, 135); ctx.lineTo(65, 135); ctx.moveTo(79, 135); ctx.lineTo(93, 135); ctx.stroke();
  }
  ctx.restore();
}

// ---------------------------------------------------------------------------
// 손목/목/손/가방
// ---------------------------------------------------------------------------
function avatarDrawWristbands(ctx) {
  ctx.save(); ctx.fillStyle = '#f47a20'; ctx.strokeStyle = '#923910'; ctx.lineWidth = .8;
  ctx.beginPath(); ctx.moveTo(36.8, 96.5); ctx.lineTo(45.5, 94.3); ctx.lineTo(47, 100.5); ctx.lineTo(38.5, 102.5); ctx.closePath(); ctx.fill(); ctx.stroke();
  ctx.beginPath(); ctx.moveTo(98.5, 94.3); ctx.lineTo(107.2, 96.5); ctx.lineTo(105.5, 102.5); ctx.lineTo(97, 100.5); ctx.closePath(); ctx.fill(); ctx.stroke();
  ctx.strokeStyle = '#fff4e8'; ctx.lineWidth = .9;
  ctx.beginPath(); ctx.moveTo(38, 98.8); ctx.lineTo(45.8, 96.9); ctx.moveTo(98.2, 96.9); ctx.lineTo(106, 98.8); ctx.stroke();
  ctx.restore();
}

function avatarDrawSmartwatch(ctx) {
  ctx.save(); ctx.fillStyle = '#263753'; ctx.strokeStyle = '#152139'; ctx.lineWidth = .85;
  ctx.beginPath(); ctx.moveTo(37, 95); ctx.lineTo(45.4, 93.2); ctx.lineTo(47, 101.6); ctx.lineTo(38.7, 103.2); ctx.closePath(); ctx.fill(); ctx.stroke();
  ctx.fillStyle = '#78d4cb'; ctx.beginPath(); ctx.roundRect(39.4, 96, 5.1, 5, 1); ctx.fill();
  ctx.restore();
}

function avatarDrawMedal(ctx) {
  ctx.save(); ctx.strokeStyle = '#23365c'; ctx.lineWidth = 2.1;
  ctx.beginPath(); ctx.moveTo(61, 63); ctx.lineTo(72, 79); ctx.lineTo(83, 63); ctx.stroke();
  ctx.fillStyle = '#efb437'; ctx.strokeStyle = '#8f5d0d'; ctx.lineWidth = 1;
  ctx.beginPath(); ctx.arc(72, 82, 7, 0, Math.PI * 2); ctx.fill(); ctx.stroke();
  ctx.fillStyle = '#fff1a8'; ctx.beginPath(); ctx.arc(70, 80, 2, 0, Math.PI * 2); ctx.fill(); ctx.restore();
}

function avatarDrawNeckwarmer(ctx) {
  ctx.save();
  ctx.beginPath(); ctx.moveTo(58, 61); ctx.quadraticCurveTo(72, 66, 86, 61);
  ctx.lineTo(84, 70); ctx.quadraticCurveTo(72, 74, 60, 70); ctx.closePath();
  avatarPathPaint(ctx, '#7d838b', '#41464d');
  ctx.restore();
}

function avatarDrawHipsack(ctx) {
  ctx.save(); ctx.strokeStyle = '#20242d'; ctx.lineWidth = 2.2;
  ctx.beginPath(); ctx.moveTo(51, 99); ctx.quadraticCurveTo(72, 94, 95, 100); ctx.stroke();
  ctx.fillStyle = '#242a34'; ctx.strokeStyle = '#11151c'; ctx.lineWidth = 1;
  ctx.beginPath(); ctx.roundRect(80, 94, 19, 12, 4); ctx.fill(); ctx.stroke();
  ctx.strokeStyle = '#f47a25'; ctx.lineWidth = 1; ctx.beginPath(); ctx.moveTo(84, 98); ctx.lineTo(95, 98); ctx.stroke(); ctx.restore();
}

function avatarDrawBottle(ctx) {
  ctx.save(); ctx.fillStyle = '#72d5cc'; ctx.strokeStyle = '#244d58'; ctx.lineWidth = .9;
  ctx.beginPath(); ctx.roundRect(101, 100, 9, 19, 3); ctx.fill(); ctx.stroke();
  ctx.fillStyle = '#f47a25'; ctx.fillRect(103, 97, 5, 4);
  ctx.strokeStyle = '#fff'; ctx.lineWidth = .75; ctx.beginPath(); ctx.moveTo(103, 107); ctx.lineTo(108, 107); ctx.stroke(); ctx.restore();
}

function avatarDrawGloves(ctx) {
  ctx.save(); ctx.fillStyle = '#263753'; ctx.strokeStyle = '#152139'; ctx.lineWidth = .85;
  ctx.beginPath(); ctx.roundRect(34.5, 101, 13, 13, 5); ctx.fill(); ctx.stroke();
  ctx.beginPath(); ctx.roundRect(96.5, 101, 13, 13, 5); ctx.fill(); ctx.stroke();
  ctx.strokeStyle = '#71809b'; ctx.lineWidth = .7;
  ctx.beginPath(); ctx.moveTo(38, 103); ctx.lineTo(38, 110); ctx.moveTo(106, 103); ctx.lineTo(106, 110); ctx.stroke(); ctx.restore();
}

// 각 부위별 전용 코드의 단일 진입점. true면 PNG 합성 없이 몸에 맞춘 렌더링 완료.
function drawFittedAvatarItemV2(ctx, item, gender) {
  const id = avatarItemId(item);
  if (!id) return false;
  if (id.startsWith('top-')) { avatarDrawTop(ctx, id); return true; }
  if (id === 'outer-orange-vest') { avatarDrawOuterVest(ctx); return true; }
  if (id.startsWith('bottom-')) { avatarDrawBottom(ctx, id); return true; }
  if (id.startsWith('shoes-')) { avatarDrawShoes(ctx, id); return true; }
  if (id.startsWith('socks-')) { avatarDrawSocks(ctx, id); return true; }
  if (id.startsWith('legwear-')) { avatarDrawLegwear(ctx, id); return true; }
  if (id === 'head-headband') { avatarDrawHeadband(ctx, gender); return true; }
  if (id === 'head-cap') { avatarDrawCap(ctx); return true; }
  if (id === 'head-crown') { avatarDrawCrown(ctx); return true; }
  if (id === 'head-white-visor') { avatarDrawVisor(ctx, gender); return true; }
  if (id === 'hair-pink-scrunchie') { avatarDrawScrunchie(ctx, gender); return true; }
  if (id === 'eyewear-sport-sunglasses') { avatarDrawSunglasses(ctx); return true; }
  if (id === 'accessory-wristbands') { avatarDrawWristbands(ctx); return true; }
  if (id === 'accessory-smartwatch') { avatarDrawSmartwatch(ctx); return true; }
  if (id === 'accessory-gold-medal') { avatarDrawMedal(ctx); return true; }
  if (id === 'neck-grey-neckwarmer') { avatarDrawNeckwarmer(ctx); return true; }
  if (id === 'bag-black-hipsack') { avatarDrawHipsack(ctx); return true; }
  if (id === 'hands-handheld-bottle') { avatarDrawBottle(ctx); return true; }
  if (id === 'hands-running-gloves') { avatarDrawGloves(ctx); return true; }
  return false;
}
