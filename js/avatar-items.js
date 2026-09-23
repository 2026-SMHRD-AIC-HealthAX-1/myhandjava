// avatar-items.js — SD 캐릭터 위에 겹쳐 그릴 수 있는 투명 PNG 아이템 목록.
// placement 값은 profile.js의 144x176 논리 캔버스를 기준으로 하며,
// 기본 SD 캐릭터의 실제 머리·몸통·다리·손목 실루엣에 맞춘 좌표입니다.
// [담당] 마이페이지 '캐릭터 꾸미기'용 아이템 카탈로그(프론트 전용 데이터).
// [백엔드 연동] 없음 — 실제 구매/보유 여부는 shop.js가 GET /api/shop/items로 받아와 merge한다.
// [주의] AVATAR_COMBO_FULL_CANVAS의 조합 키가 실제 PNG 아트와 안 맞으면 캐릭터가 이상하게 합성된다
//        — profile.js drawPixelCharacter 참고. '1011'(모자+하의+신발) 조합만 제작된 그림이
//        없어서 의도적으로 빠져 있다(레이어 합성으로 자동 폴백).

const AVATAR_WEARABLE_SLOTS = ['top', 'bottom', 'shoes', 'head', 'accessory'];

// 모자(head-cap)·상의(top-lavender-hoodie)·하의(bottom-lavender-joggers)·신발(shoes-mint-sneakers)
// 네 슬롯의 조합(2^4=16가지)마다 실제로 그려서 받은 전신 일러스트 — 이 네 아이템이 정확히
// 이 조합일 때만 레이어 합성 대신 이 그림을 통째로 쓴다. 키는 'cap-top-bottom-shoes' 순서로
// 0/1 네 자리(예: 모자만 착용 = '1000'). assets/character_items/{성별}_배경없음 폴더 기준이며,
// 딱 하나(모자+하의+신발, '1011')만 제작된 그림이 없어서 그 조합일 때는 기존 레이어 합성으로
// 자동 폴백한다.
const AVATAR_COMBO_FILE_BY_KEY = {
  '1000': '02_모자.png', '0100': '03_상의.png', '0010': '04_하의.png', '0001': '05_신발.png',
  '1100': '06_모자+상의.png', '1010': '07_모자+하의.png', '1001': '08_모자+신발.png',
  '0110': '09_상의+하의.png', '0101': '10_상의+신발.png', '0011': '11_하의+신발.png',
  '1110': '12_모자+상의+하의.png', '1101': '13_모자+상의+신발.png', '0111': '14_상의+하의+신발.png',
  '1111': '15_모자+상의+하의+신발.png',
};
const AVATAR_COMBO_FULL_CANVAS = {};
// 위 네 슬롯 조합 + 'background-riverside-day' 배경(상점의 "맑은 강변 산책로")을 동시에 장착했을
// 때 쓰는 전신 사진 — 배경까지 함께 그려져 있어 AVATAR_COMBO_FULL_CANVAS와 달리 '0000'(네 슬롯
// 다 미착용, 배경만 착용)도 포함한다. 이 배경일 때는 이 맵을 먼저 찾고, 없으면 기존 방식으로 되돌아간다.
const AVATAR_COMBO_BG_RIVERSIDE_DAY = {
  '0000': { male: 'assets/character_items/남자_강변배경/01_기본.png', female: 'assets/character_items/여자_강변배경/01_기본.png' },
};
Object.entries(AVATAR_COMBO_FILE_BY_KEY).forEach(([key, file]) => {
  AVATAR_COMBO_FULL_CANVAS[key] = {
    male: `assets/character_items/남자_배경없음/${file}`,
    female: `assets/character_items/여자_배경없음/${file}`,
  };
  AVATAR_COMBO_BG_RIVERSIDE_DAY[key] = {
    male: `assets/character_items/남자_강변배경/${file}`,
    female: `assets/character_items/여자_강변배경/${file}`,
  };
});
// equip(getEquipState() 결과)에서 지금 이 네 슬롯이 어떤 조합인지 키를 만든다.
// 매칭되는 사진이 없으면 null을 반환해 기존 레이어 합성 방식으로 되돌아간다.
function getAvatarComboKey(equip, gender){
  const cap = equip.head && equip.head.id === 'head-cap' ? 1 : 0;
  const top = equip.top && equip.top.id === 'top-lavender-hoodie' ? 1 : 0;
  const bottom = equip.bottom && equip.bottom.id === 'bottom-lavender-joggers' ? 1 : 0;
  const shoes = equip.shoes && equip.shoes.id === 'shoes-mint-sneakers' ? 1 : 0;
  const key = `${cap}${top}${bottom}${shoes}`;
  const g = gender === 'female' ? 'female' : 'male';

  if (equip.background && equip.background.id === 'background-riverside-day') {
    const bgEntry = AVATAR_COMBO_BG_RIVERSIDE_DAY[key];
    const bgSrc = bgEntry && bgEntry[g];
    if (bgSrc) return { key: `bg-riverside-${key}`, src: bgSrc, hasBackground: true };
  }

  if (key === '0000') return null; // 아무것도 안 입었으면 기본 캐릭터(레이어 방식)로 — 사진으로 대체하지 않는다
  const entry = AVATAR_COMBO_FULL_CANVAS[key];
  if (!entry) return null;
  const src = entry[g];
  return src ? { key, src } : null;
}

const AVATAR_ITEM_CATALOG = [
  {
    id: 'head-headband', name: '오렌지 헤드밴드', price: 120,
    owned: false, equipped: false, slot: 'head', category: '헤어',
    effect: '능력치 없음 · 외형 전용', effectDesc: '운동할 때 포인트가 되는 오렌지 헤드밴드입니다.',
    asset: 'assets/avatar-items/head-headband.png', fullCanvas: { male: 'assets/avatar-items/full-canvas/head-headband-male.png', female: 'assets/avatar-items/full-canvas/head-headband-female.png' }, placement: { x: 47, y: 28, w: 50, h: 8 }, z: 40,
  },
  {
    id: 'head-cap', name: '네이비 스포츠 캡', price: 220,
    owned: false, equipped: false, slot: 'head', category: '헤어',
    effect: '능력치 없음 · 외형 전용', effectDesc: '차분한 네이비 컬러의 스포츠 캡입니다.',
    asset: 'assets/avatar-items/네이비 스포츠 캡.png', fullCanvas: { male: 'assets/avatar-items/full-canvas/head-cap-male.png', female: 'assets/avatar-items/full-canvas/head-cap-female.png' }, placement: { x: 37, y: 10, w: 70, h: 38 }, z: 40,
  },

  {
    id: 'top-mint-shirt', name: '민트 트레이닝 티셔츠', price: 180,
    owned: false, equipped: false, slot: 'top', category: '상의',
    effect: '능력치 없음 · 외형 전용', effectDesc: '가볍게 입기 좋은 민트색 운동 티셔츠입니다.',
    asset: 'assets/avatar-items/top-mint-shirt.png', fullCanvas: { male: 'assets/avatar-items/full-canvas/top-mint-shirt-male.png', female: 'assets/avatar-items/full-canvas/top-mint-shirt-female.png' }, placement: { x: 42, y: 62, w: 60, h: 42 }, z: 20,
  },
  {
    id: 'top-orange-jacket', name: '오렌지 트랙 재킷', price: 320,
    owned: false, equipped: false, slot: 'top', category: '상의',
    effect: '능력치 없음 · 외형 전용', effectDesc: '활기찬 오렌지 컬러의 집업 트랙 재킷입니다.',
    asset: 'assets/avatar-items/top-orange-jacket.png', fullCanvas: { male: 'assets/avatar-items/full-canvas/top-orange-jacket-male.png', female: 'assets/avatar-items/full-canvas/top-orange-jacket-female.png' }, placement: { x: 39, y: 60, w: 66, h: 48 }, z: 20,
  },
  {
    id: 'top-lavender-hoodie', name: '라벤더 후디', price: 340,
    owned: false, equipped: false, slot: 'top', category: '상의',
    effect: '능력치 없음 · 외형 전용', effectDesc: '부드러운 라벤더색 후드 운동복입니다.',
    asset: 'assets/avatar-items/top-lavender-hoodie.png', fullCanvas: { male: 'assets/avatar-items/full-canvas/top-lavender-hoodie-male.png', female: 'assets/avatar-items/full-canvas/top-lavender-hoodie-female.png' }, placement: { x: 39, y: 59, w: 66, h: 50 }, z: 20,
  },
  {
    id: 'bottom-mint-shorts', name: '딥그린 숏팬츠', price: 160,
    owned: false, equipped: false, slot: 'bottom', category: '하의',
    effect: '능력치 없음 · 외형 전용', effectDesc: '차분한 딥그린 컬러의 운동용 숏팬츠입니다.',
    asset: 'assets/avatar-items/bottom-mint-shorts.png', fullCanvas: { male: 'assets/avatar-items/full-canvas/bottom-mint-shorts-male.png', female: 'assets/avatar-items/full-canvas/bottom-mint-shorts-female.png' }, placement: { x: 47, y: 101, w: 50, h: 27 }, z: 22,
  },
  {
    id: 'bottom-charcoal-pants', name: '차콜 트랙 팬츠', price: 280,
    owned: false, equipped: false, slot: 'bottom', category: '하의',
    effect: '능력치 없음 · 외형 전용', effectDesc: '오렌지 라인이 들어간 차콜 트레이닝 팬츠입니다.',
    asset: 'assets/avatar-items/bottom-charcoal-pants.png', fullCanvas: { male: 'assets/avatar-items/full-canvas/bottom-charcoal-pants-male.png', female: 'assets/avatar-items/full-canvas/bottom-charcoal-pants-female.png' }, placement: { x: 48, y: 100, w: 48, h: 54 }, z: 22,
  },
  {
    id: 'bottom-lavender-joggers', name: '라벤더 조거 팬츠', price: 300,
    owned: false, equipped: false, slot: 'bottom', category: '하의',
    effect: '능력치 없음 · 외형 전용', effectDesc: '편안한 핏의 라벤더 조거 팬츠입니다.',
    asset: 'assets/avatar-items/bottom-lavender-joggers.png', fullCanvas: { male: 'assets/avatar-items/full-canvas/bottom-lavender-joggers-male.png', female: 'assets/avatar-items/full-canvas/bottom-lavender-joggers-female.png' }, placement: { x: 48, y: 100, w: 48, h: 54 }, z: 22,
  },
  {
    id: 'shoes-mint-sneakers', name: '민트 운동화', price: 260,
    owned: false, equipped: false, slot: 'shoes', category: '신발',
    effect: '능력치 없음 · 외형 전용', effectDesc: '민트 포인트가 들어간 산뜻한 운동화입니다.',
    asset: 'assets/avatar-items/민트 운동화.png', fullCanvas: { male: 'assets/avatar-items/full-canvas/shoes-mint-sneakers-male.png', female: 'assets/avatar-items/full-canvas/shoes-mint-sneakers-female.png' }, placement: { x: 38, y: 148, w: 68, h: 24 }, z: 30,
  },
  {
    id: 'shoes-lavender-hightops', name: '라벤더 하이탑', price: 310,
    owned: false, equipped: false, slot: 'shoes', category: '신발',
    effect: '능력치 없음 · 외형 전용', effectDesc: '발목까지 올라오는 라벤더 하이탑 운동화입니다.',
    asset: 'assets/avatar-items/shoes-lavender-hightops.png', fullCanvas: { male: 'assets/avatar-items/full-canvas/shoes-lavender-hightops-male.png', female: 'assets/avatar-items/full-canvas/shoes-lavender-hightops-female.png' }, placement: { x: 38, y: 146, w: 68, h: 28 }, z: 30,
  },
  {
    id: 'accessory-wristbands', name: '오렌지 손목밴드', price: 100,
    owned: false, equipped: false, slot: 'accessory', category: '기타',
    effect: '능력치 없음 · 외형 전용', effectDesc: '양쪽 손목에 함께 착용하는 오렌지 밴드입니다.',
    asset: 'assets/avatar-items/accessory-wristbands.png', fullCanvas: { male: 'assets/avatar-items/full-canvas/accessory-wristbands-male.png', female: 'assets/avatar-items/full-canvas/accessory-wristbands-female.png' }, z: 50,
    layers: [
      { asset: 'assets/avatar-items/accessory-wristband-left-layer.png', placement: { x: 41, y: 91, w: 9, h: 7 } },
      { asset: 'assets/avatar-items/accessory-wristband-right-layer.png', placement: { x: 94, y: 91, w: 9, h: 7 } },
    ],
  },
  {
    id: 'accessory-smartwatch', name: '네이비 스마트워치', price: 240,
    owned: false, equipped: false, slot: 'accessory', category: '기타',
    effect: '능력치 없음 · 외형 전용', effectDesc: '운동 기록을 확인하는 네이비 스마트워치입니다.',
    asset: 'assets/avatar-items/accessory-smartwatch.png', fullCanvas: { male: 'assets/avatar-items/full-canvas/accessory-smartwatch-male.png', female: 'assets/avatar-items/full-canvas/accessory-smartwatch-female.png' }, placement: { x: 98, y: 89, w: 9, h: 12 }, z: 50,
  },
  {
    id: 'accessory-gold-medal', name: '골드 메달', price: 380,
    owned: false, equipped: false, slot: 'accessory', category: '기타',
    effect: '능력치 없음 · 외형 전용', effectDesc: '꾸준한 운동을 기념하는 골드 메달입니다.',
    asset: 'assets/avatar-items/accessory-gold-medal.png', fullCanvas: { male: 'assets/avatar-items/full-canvas/accessory-gold-medal-male.png', female: 'assets/avatar-items/full-canvas/accessory-gold-medal-female.png' }, placement: { x: 61, y: 58, w: 22, h: 31 }, z: 50,
  },
  {
    id: 'background-riverside-day', name: '배경 - 맑은 강변 산책로', price: 250,
    owned: false, equipped: false, slot: 'background', category: '배경',
    effect: '능력치 없음 · 외형 전용', effectDesc: '푸른 하늘과 강변 러닝 코스가 펼쳐진 밝은 낮 배경입니다.',
    asset: 'assets/avatar-items/background-riverside-day.png', z: 0,
  },
  {
    id: 'background-riverside-sunset', name: '배경 - 노을빛 강변', price: 300,
    owned: false, equipped: false, slot: 'background', category: '배경',
    effect: '능력치 없음 · 외형 전용', effectDesc: '주황빛 노을이 물든 강변 러닝 코스 배경입니다.',
    asset: 'assets/avatar-items/background-riverside-sunset.png', z: 0,
  },
  {
    id: 'background-autumn-lake', name: '배경 - 가을 호수 공원', price: 350,
    owned: false, equipped: false, slot: 'background', category: '배경',
    effect: '능력치 없음 · 외형 전용', effectDesc: '단풍과 호수가 어우러진 따뜻한 가을 공원 배경입니다.',
    asset: 'assets/avatar-items/background-autumn-lake.png', z: 0,
  },
  {
    id: 'background-rainy-park', name: '배경 - 비 오는 가로수길', price: 320,
    owned: false, equipped: false, slot: 'background', category: '배경',
    effect: '능력치 없음 · 외형 전용', effectDesc: '가로등 불빛이 비치는 차분한 빗속 공원 배경입니다.',
    asset: 'assets/avatar-items/background-rainy-park.png', z: 0,
  },
];
