// avatar-items.js — SD 캐릭터 위에 겹쳐 그릴 수 있는 투명 PNG 아이템 목록.
// placement 값은 profile.js의 144x176 논리 캔버스를 기준으로 하며,
// 기본 SD 캐릭터의 실제 머리·몸통·다리·손목 실루엣에 맞춘 좌표입니다.

const AVATAR_WEARABLE_SLOTS = ['top', 'bottom', 'shoes', 'head', 'accessory'];

const AVATAR_ITEM_CATALOG = [
  {
    id: 'head-headband', name: '오렌지 헤드밴드', price: 120,
    owned: true, equipped: false, slot: 'head', category: '헤어', levelReq: 1,
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
    owned: true, equipped: false, slot: 'top', category: '상의', levelReq: 1,
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
    owned: true, equipped: false, slot: 'bottom', category: '하의', levelReq: 1,
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
    owned: true, equipped: false, slot: 'accessory', category: '기타', levelReq: 1,
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
