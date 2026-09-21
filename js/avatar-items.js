// avatar-items.js — SD 캐릭터 위에 겹쳐 그릴 수 있는 투명 PNG 아이템 목록.
// placement 값은 profile.js의 144x176 논리 캔버스를 기준으로 하며,
// 기본 SD 캐릭터의 실제 머리·몸통·다리·손목 실루엣에 맞춘 좌표입니다.

const AVATAR_WEARABLE_SLOTS = ['top', 'bottom', 'shoes', 'head', 'accessory'];

// 모자(head-cap)·후디(top-lavender-hoodie)·조거팬츠(bottom-lavender-joggers) 세 슬롯의
// 조합(2×2×2=8가지, "아무것도 없음"은 기본 캐릭터라 제외한 7가지)마다 실제로 그려서 받은
// 전신 이미지 — 이 세 슬롯이 정확히 이 조합일 때만 레이어 합성 대신 이 사진을 통째로 쓴다.
// 배경이 흰색(불투명)이라 캐릭터 배경 아이템은 이 조합에서는 안 보인다는 제약이 있다
// (단, 'background-riverside-day' 배경은 AVATAR_COMBO_BG_RIVERSIDE_DAY로 별도 대응함).
// 키는 'cap-top-bottom' 순서로 0/1 세 자리(예: 모자만 착용 = '100').
const AVATAR_COMBO_FULL_CANVAS = {
  '100': { male: 'assets/avatar-items/full-canvas/combo-male-100.png', female: 'assets/avatar-items/full-canvas/combo-female-100.png' },
  '010': { male: 'assets/avatar-items/full-canvas/combo-male-010.png', female: 'assets/avatar-items/full-canvas/combo-female-010.png' },
  '001': { male: 'assets/avatar-items/full-canvas/combo-male-001.png', female: 'assets/avatar-items/full-canvas/combo-female-001.png' },
  '110': { male: 'assets/avatar-items/full-canvas/combo-male-110.png', female: 'assets/avatar-items/full-canvas/combo-female-110.png' },
  '101': { male: 'assets/avatar-items/full-canvas/combo-male-101.png', female: 'assets/avatar-items/full-canvas/combo-female-101.png' },
  '011': { male: 'assets/avatar-items/full-canvas/combo-male-011.png', female: 'assets/avatar-items/full-canvas/combo-female-011.png' },
  // '111'(모자+후디+조거팬츠 다 착용)은 준비된 사진이 실제로는 반바지·기본 운동화를 그리고
  // 있어(조거팬츠가 반바지로 보이고, 하이탑 신발 레이어도 씌워지지 않음) 잘못된 사진이었다.
  // 게다가 이 사진 경로는 drawAvatarBackground를 건너뛰어서 배경 아이템도 무시돼버렸다.
  // 그래서 이 조합만은 의도적으로 빼서 기존 레이어 합성(개별 아이템 벡터 렌더링) 방식으로
  // 되돌아가게 한다 — 조거팬츠·하이탑·배경이 다 정확히 반영된다.
};
// 위 세 슬롯 조합 + 'background-riverside-day' 배경(상점의 "맑은 강변 산책로")을 동시에 장착했을
// 때 쓰는 전신 사진 — 배경까지 함께 그려져 있어 AVATAR_COMBO_FULL_CANVAS와 달리 '000'(세 슬롯
// 다 미착용)도 포함한다. 이 배경일 때는 이 맵을 먼저 찾고, 없으면 기존 흰 배경 조합으로 되돌아간다.
const AVATAR_COMBO_BG_RIVERSIDE_DAY = {
  '000': { male: 'assets/avatar-items/full-canvas/combo-bg-riverside-day-male-000.png', female: 'assets/avatar-items/full-canvas/combo-bg-riverside-day-female-000.png' },
  '100': { male: 'assets/avatar-items/full-canvas/combo-bg-riverside-day-male-100.png', female: 'assets/avatar-items/full-canvas/combo-bg-riverside-day-female-100.png' },
  '010': { male: 'assets/avatar-items/full-canvas/combo-bg-riverside-day-male-010.png', female: 'assets/avatar-items/full-canvas/combo-bg-riverside-day-female-010.png' },
  '001': { male: 'assets/avatar-items/full-canvas/combo-bg-riverside-day-male-001.png', female: 'assets/avatar-items/full-canvas/combo-bg-riverside-day-female-001.png' },
  '110': { male: 'assets/avatar-items/full-canvas/combo-bg-riverside-day-male-110.png', female: 'assets/avatar-items/full-canvas/combo-bg-riverside-day-female-110.png' },
  '101': { male: 'assets/avatar-items/full-canvas/combo-bg-riverside-day-male-101.png', female: 'assets/avatar-items/full-canvas/combo-bg-riverside-day-female-101.png' },
  '011': { male: 'assets/avatar-items/full-canvas/combo-bg-riverside-day-male-011.png', female: 'assets/avatar-items/full-canvas/combo-bg-riverside-day-female-011.png' },
  // '111'(모자+후디+조거팬츠+이 배경 전부)은 사진이 없어 기존 방식(흰 배경 조합 또는 레이어 합성)으로 되돌아간다.
};
// equip(getEquipState() 결과)에서 지금 이 세 슬롯이 어떤 조합인지 키를 만든다.
// 매칭되는 사진이 없으면 null을 반환해 기존 레이어 합성 방식으로 되돌아간다.
function getAvatarComboKey(equip, gender){
  const cap = equip.head && equip.head.id === 'head-cap' ? 1 : 0;
  const hoodie = equip.top && equip.top.id === 'top-lavender-hoodie' ? 1 : 0;
  const joggers = equip.bottom && equip.bottom.id === 'bottom-lavender-joggers' ? 1 : 0;
  const key = `${cap}${hoodie}${joggers}`;
  const g = gender === 'female' ? 'female' : 'male';

  if (equip.background && equip.background.id === 'background-riverside-day') {
    const bgEntry = AVATAR_COMBO_BG_RIVERSIDE_DAY[key];
    const bgSrc = bgEntry && bgEntry[g];
    if (bgSrc) return { key: `bg-riverside-${key}`, src: bgSrc, hasBackground: true };
  }

  if (key === '000') return null;
  const entry = AVATAR_COMBO_FULL_CANVAS[key];
  if (!entry) return null;
  const src = entry[g];
  return src ? { key, src } : null;
}

const AVATAR_ITEM_CATALOG = [
  {
    id: 'head-headband', name: '오렌지 헤드밴드', price: 120,
    owned: false, equipped: false, slot: 'head', category: '헤어', levelReq: 1,
    effect: '능력치 없음 · 외형 전용', effectDesc: '운동할 때 포인트가 되는 오렌지 헤드밴드입니다.',
    asset: 'assets/avatar-items/head-headband.png', fullCanvas: { male: 'assets/avatar-items/full-canvas/head-headband-male.png', female: 'assets/avatar-items/full-canvas/head-headband-female.png' }, placement: { x: 47, y: 28, w: 50, h: 8 }, z: 40,
  },
  {
    id: 'head-cap', name: '네이비 스포츠 캡', price: 220,
    owned: false, equipped: false, slot: 'head', category: '헤어', levelReq: 2,
    effect: '능력치 없음 · 외형 전용', effectDesc: '차분한 네이비 컬러의 스포츠 캡입니다.',
    asset: 'assets/avatar-items/head-cap.png', fullCanvas: { male: 'assets/avatar-items/full-canvas/head-cap-male.png', female: 'assets/avatar-items/full-canvas/head-cap-female.png' }, placement: { x: 37, y: 10, w: 70, h: 38 }, z: 40,
  },

  {
    id: 'top-mint-shirt', name: '민트 트레이닝 티셔츠', price: 180,
    owned: false, equipped: false, slot: 'top', category: '상의', levelReq: 1,
    effect: '능력치 없음 · 외형 전용', effectDesc: '가볍게 입기 좋은 민트색 운동 티셔츠입니다.',
    asset: 'assets/avatar-items/top-mint-shirt.png', fullCanvas: { male: 'assets/avatar-items/full-canvas/top-mint-shirt-male.png', female: 'assets/avatar-items/full-canvas/top-mint-shirt-female.png' }, placement: { x: 42, y: 62, w: 60, h: 42 }, z: 20,
  },
  {
    id: 'top-orange-jacket', name: '오렌지 트랙 재킷', price: 320,
    owned: false, equipped: false, slot: 'top', category: '상의', levelReq: 3,
    effect: '능력치 없음 · 외형 전용', effectDesc: '활기찬 오렌지 컬러의 집업 트랙 재킷입니다.',
    asset: 'assets/avatar-items/top-orange-jacket.png', fullCanvas: { male: 'assets/avatar-items/full-canvas/top-orange-jacket-male.png', female: 'assets/avatar-items/full-canvas/top-orange-jacket-female.png' }, placement: { x: 39, y: 60, w: 66, h: 48 }, z: 20,
  },
  {
    id: 'top-lavender-hoodie', name: '라벤더 후디', price: 340,
    owned: false, equipped: false, slot: 'top', category: '상의', levelReq: 3,
    effect: '능력치 없음 · 외형 전용', effectDesc: '부드러운 라벤더색 후드 운동복입니다.',
    asset: 'assets/avatar-items/top-lavender-hoodie.png', fullCanvas: { male: 'assets/avatar-items/full-canvas/top-lavender-hoodie-male.png', female: 'assets/avatar-items/full-canvas/top-lavender-hoodie-female.png' }, placement: { x: 39, y: 59, w: 66, h: 50 }, z: 20,
  },
  {
    id: 'bottom-mint-shorts', name: '딥그린 숏팬츠', price: 160,
    owned: false, equipped: false, slot: 'bottom', category: '하의', levelReq: 1,
    effect: '능력치 없음 · 외형 전용', effectDesc: '차분한 딥그린 컬러의 운동용 숏팬츠입니다.',
    asset: 'assets/avatar-items/bottom-mint-shorts.png', fullCanvas: { male: 'assets/avatar-items/full-canvas/bottom-mint-shorts-male.png', female: 'assets/avatar-items/full-canvas/bottom-mint-shorts-female.png' }, placement: { x: 47, y: 101, w: 50, h: 27 }, z: 22,
  },
  {
    id: 'bottom-charcoal-pants', name: '차콜 트랙 팬츠', price: 280,
    owned: false, equipped: false, slot: 'bottom', category: '하의', levelReq: 2,
    effect: '능력치 없음 · 외형 전용', effectDesc: '오렌지 라인이 들어간 차콜 트레이닝 팬츠입니다.',
    asset: 'assets/avatar-items/bottom-charcoal-pants.png', fullCanvas: { male: 'assets/avatar-items/full-canvas/bottom-charcoal-pants-male.png', female: 'assets/avatar-items/full-canvas/bottom-charcoal-pants-female.png' }, placement: { x: 48, y: 100, w: 48, h: 54 }, z: 22,
  },
  {
    id: 'bottom-lavender-joggers', name: '라벤더 조거 팬츠', price: 300,
    owned: false, equipped: false, slot: 'bottom', category: '하의', levelReq: 3,
    effect: '능력치 없음 · 외형 전용', effectDesc: '편안한 핏의 라벤더 조거 팬츠입니다.',
    asset: 'assets/avatar-items/bottom-lavender-joggers.png', fullCanvas: { male: 'assets/avatar-items/full-canvas/bottom-lavender-joggers-male.png', female: 'assets/avatar-items/full-canvas/bottom-lavender-joggers-female.png' }, placement: { x: 48, y: 100, w: 48, h: 54 }, z: 22,
  },
  {
    id: 'shoes-mint-sneakers', name: '민트 운동화', price: 260,
    owned: false, equipped: false, slot: 'shoes', category: '신발', levelReq: 2,
    effect: '능력치 없음 · 외형 전용', effectDesc: '민트 포인트가 들어간 산뜻한 운동화입니다.',
    asset: 'assets/avatar-items/shoes-mint-sneakers.png', fullCanvas: { male: 'assets/avatar-items/full-canvas/shoes-mint-sneakers-male.png', female: 'assets/avatar-items/full-canvas/shoes-mint-sneakers-female.png' }, placement: { x: 38, y: 148, w: 68, h: 24 }, z: 30,
  },
  {
    id: 'shoes-lavender-hightops', name: '라벤더 하이탑', price: 310,
    owned: false, equipped: false, slot: 'shoes', category: '신발', levelReq: 3,
    effect: '능력치 없음 · 외형 전용', effectDesc: '발목까지 올라오는 라벤더 하이탑 운동화입니다.',
    asset: 'assets/avatar-items/shoes-lavender-hightops.png', fullCanvas: { male: 'assets/avatar-items/full-canvas/shoes-lavender-hightops-male.png', female: 'assets/avatar-items/full-canvas/shoes-lavender-hightops-female.png' }, placement: { x: 38, y: 146, w: 68, h: 28 }, z: 30,
  },
  {
    id: 'accessory-wristbands', name: '오렌지 손목밴드', price: 100,
    owned: false, equipped: false, slot: 'accessory', category: '기타', levelReq: 1,
    effect: '능력치 없음 · 외형 전용', effectDesc: '양쪽 손목에 함께 착용하는 오렌지 밴드입니다.',
    asset: 'assets/avatar-items/accessory-wristbands.png', fullCanvas: { male: 'assets/avatar-items/full-canvas/accessory-wristbands-male.png', female: 'assets/avatar-items/full-canvas/accessory-wristbands-female.png' }, z: 50,
    layers: [
      { asset: 'assets/avatar-items/accessory-wristband-left-layer.png', placement: { x: 41, y: 91, w: 9, h: 7 } },
      { asset: 'assets/avatar-items/accessory-wristband-right-layer.png', placement: { x: 94, y: 91, w: 9, h: 7 } },
    ],
  },
  {
    id: 'accessory-smartwatch', name: '네이비 스마트워치', price: 240,
    owned: false, equipped: false, slot: 'accessory', category: '기타', levelReq: 2,
    effect: '능력치 없음 · 외형 전용', effectDesc: '운동 기록을 확인하는 네이비 스마트워치입니다.',
    asset: 'assets/avatar-items/accessory-smartwatch.png', fullCanvas: { male: 'assets/avatar-items/full-canvas/accessory-smartwatch-male.png', female: 'assets/avatar-items/full-canvas/accessory-smartwatch-female.png' }, placement: { x: 98, y: 89, w: 9, h: 12 }, z: 50,
  },
  {
    id: 'accessory-gold-medal', name: '골드 메달', price: 380,
    owned: false, equipped: false, slot: 'accessory', category: '기타', levelReq: 4,
    effect: '능력치 없음 · 외형 전용', effectDesc: '꾸준한 운동을 기념하는 골드 메달입니다.',
    asset: 'assets/avatar-items/accessory-gold-medal.png', fullCanvas: { male: 'assets/avatar-items/full-canvas/accessory-gold-medal-male.png', female: 'assets/avatar-items/full-canvas/accessory-gold-medal-female.png' }, placement: { x: 61, y: 58, w: 22, h: 31 }, z: 50,
  },
  {
    id: 'background-riverside-day', name: '배경 - 맑은 강변 산책로', price: 250,
    owned: false, equipped: false, slot: 'background', category: '배경', levelReq: 1,
    effect: '능력치 없음 · 외형 전용', effectDesc: '푸른 하늘과 강변 러닝 코스가 펼쳐진 밝은 낮 배경입니다.',
    asset: 'assets/avatar-items/background-riverside-day.png', z: 0,
  },
  {
    id: 'background-riverside-sunset', name: '배경 - 노을빛 강변', price: 300,
    owned: false, equipped: false, slot: 'background', category: '배경', levelReq: 2,
    effect: '능력치 없음 · 외형 전용', effectDesc: '주황빛 노을이 물든 강변 러닝 코스 배경입니다.',
    asset: 'assets/avatar-items/background-riverside-sunset.png', z: 0,
  },
  {
    id: 'background-autumn-lake', name: '배경 - 가을 호수 공원', price: 350,
    owned: false, equipped: false, slot: 'background', category: '배경', levelReq: 3,
    effect: '능력치 없음 · 외형 전용', effectDesc: '단풍과 호수가 어우러진 따뜻한 가을 공원 배경입니다.',
    asset: 'assets/avatar-items/background-autumn-lake.png', z: 0,
  },
  {
    id: 'background-rainy-park', name: '배경 - 비 오는 가로수길', price: 320,
    owned: false, equipped: false, slot: 'background', category: '배경', levelReq: 3,
    effect: '능력치 없음 · 외형 전용', effectDesc: '가로등 불빛이 비치는 차분한 빗속 공원 배경입니다.',
    asset: 'assets/avatar-items/background-rainy-park.png', z: 0,
  },
];
