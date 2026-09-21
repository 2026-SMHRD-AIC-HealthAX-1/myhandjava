-- server/sql/avatar_items.sql — 상품 마스터 + 사용자 보유/착용 (MySQL 8 / PostgreSQL 호환 문법)
-- 프론트 js/avatar-items.js 의 id · slot · asset 과 반드시 동일해야 합니다.

CREATE TABLE IF NOT EXISTS avatar_items (
  id           VARCHAR(64)  PRIMARY KEY,
  name         VARCHAR(100) NOT NULL,
  price        INT          NOT NULL,
  slot         VARCHAR(20)  NOT NULL,   -- socks|top|bottom|legwear|outer|shoes|hair|head|eyewear|bag|wrist|neck|hands|background
  legacy_slot  VARCHAR(20)  NULL,       -- 구 데이터 호환용 (accessory)
  category     VARCHAR(30)  NOT NULL,
  level_req    INT          NOT NULL DEFAULT 1,
  effect       VARCHAR(100) NOT NULL,
  effect_desc  VARCHAR(255) NOT NULL,
  asset        VARCHAR(255) NOT NULL,
  layer_assets TEXT         NULL,       -- JSON 배열 문자열 (양손 아이템 등)
  genders      VARCHAR(20)  NOT NULL DEFAULT 'male,female',
  default_owned BOOLEAN     NOT NULL DEFAULT FALSE,
  active       BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS user_avatar_items (
  user_id   VARCHAR(64) NOT NULL,
  item_id   VARCHAR(64) NOT NULL REFERENCES avatar_items(id),
  owned_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, item_id)
);

-- 슬롯당 1개만 착용: (user_id, slot) 유니크
CREATE TABLE IF NOT EXISTS user_avatar_equipped (
  user_id  VARCHAR(64) NOT NULL,
  slot     VARCHAR(20) NOT NULL,
  item_id  VARCHAR(64) NOT NULL REFERENCES avatar_items(id),
  PRIMARY KEY (user_id, slot)
);

-- 구 데이터 마이그레이션: accessory 슬롯으로 저장된 착용 정보를 카탈로그의 실제 슬롯으로 이동
-- UPDATE user_avatar_equipped e JOIN avatar_items i ON i.id = e.item_id SET e.slot = i.slot WHERE e.slot = 'accessory';

INSERT INTO avatar_items (id, name, price, slot, legacy_slot, category, level_req, effect, effect_desc, asset, layer_assets, genders, default_owned, active) VALUES
  ('head-headband', '오렌지 헤드밴드', 120, 'head', NULL, '헤어', 1, '능력치 없음 · 외형 전용', '운동할 때 포인트가 되는 오렌지 헤드밴드입니다.', 'assets/avatar-items/head-headband.png', NULL, 'male,female', TRUE, TRUE),
  ('head-cap', '네이비 스포츠 캡', 220, 'head', NULL, '헤어', 2, '능력치 없음 · 외형 전용', '차분한 네이비 컬러의 스포츠 캡입니다.', 'assets/avatar-items/head-cap.png', NULL, 'male,female', FALSE, TRUE),
  ('head-crown', '골드 챔피언 왕관', 450, 'head', NULL, '헤어', 4, '능력치 없음 · 외형 전용', '챌린지 우승자를 위한 골드 왕관입니다.', 'assets/avatar-items/head-crown.png', NULL, 'male,female', FALSE, TRUE),
  ('top-mint-shirt', '민트 트레이닝 티셔츠', 180, 'top', NULL, '상의', 1, '능력치 없음 · 외형 전용', '가볍게 입기 좋은 민트색 운동 티셔츠입니다.', 'assets/avatar-items/top-mint-shirt.png', NULL, 'male,female', TRUE, TRUE),
  ('top-orange-jacket', '오렌지 트랙 재킷', 320, 'top', NULL, '상의', 3, '능력치 없음 · 외형 전용', '활기찬 오렌지 컬러의 집업 트랙 재킷입니다.', 'assets/avatar-items/top-orange-jacket.png', NULL, 'male,female', FALSE, TRUE),
  ('top-lavender-hoodie', '라벤더 후디', 340, 'top', NULL, '상의', 3, '능력치 없음 · 외형 전용', '부드러운 라벤더색 후드 운동복입니다.', 'assets/avatar-items/top-lavender-hoodie.png', NULL, 'male,female', FALSE, TRUE),
  ('bottom-mint-shorts', '민트 스포츠 반바지', 160, 'bottom', NULL, '하의', 1, '능력치 없음 · 외형 전용', '산뜻한 민트색 스포츠 반바지입니다.', 'assets/avatar-items/bottom-mint-shorts.png', NULL, 'male,female', TRUE, TRUE),
  ('bottom-charcoal-pants', '차콜 트랙 팬츠', 280, 'bottom', NULL, '하의', 2, '능력치 없음 · 외형 전용', '오렌지 라인이 들어간 차콜 트레이닝 팬츠입니다.', 'assets/avatar-items/bottom-charcoal-pants.png', NULL, 'male,female', FALSE, TRUE),
  ('bottom-lavender-joggers', '라벤더 조거 팬츠', 300, 'bottom', NULL, '하의', 3, '능력치 없음 · 외형 전용', '편안한 핏의 라벤더 조거 팬츠입니다.', 'assets/avatar-items/bottom-lavender-joggers.png', NULL, 'male,female', FALSE, TRUE),
  ('shoes-orange-runners', '오렌지 러닝화', 200, 'shoes', NULL, '신발', 1, '능력치 없음 · 외형 전용', '기본 컬러와 잘 어울리는 오렌지 러닝화입니다.', 'assets/avatar-items/shoes-orange-runners.png', NULL, 'male,female', TRUE, TRUE),
  ('shoes-mint-sneakers', '민트 운동화', 260, 'shoes', NULL, '신발', 2, '능력치 없음 · 외형 전용', '민트 포인트가 들어간 산뜻한 운동화입니다.', 'assets/avatar-items/shoes-mint-sneakers.png', NULL, 'male,female', FALSE, TRUE),
  ('shoes-lavender-hightops', '라벤더 하이탑', 310, 'shoes', NULL, '신발', 3, '능력치 없음 · 외형 전용', '발목까지 올라오는 라벤더 하이탑 운동화입니다.', 'assets/avatar-items/shoes-lavender-hightops.png', NULL, 'male,female', FALSE, TRUE),
  ('accessory-wristbands', '오렌지 손목밴드', 100, 'wrist', 'accessory', '기타', 1, '능력치 없음 · 외형 전용', '양쪽 손목에 함께 착용하는 오렌지 밴드입니다.', 'assets/avatar-items/accessory-wristbands.png', '["assets/avatar-items/accessory-wristband-left-layer.png", "assets/avatar-items/accessory-wristband-right-layer.png"]', 'male,female', TRUE, TRUE),
  ('accessory-smartwatch', '네이비 스마트워치', 240, 'wrist', 'accessory', '기타', 2, '능력치 없음 · 외형 전용', '운동 기록을 확인하는 네이비 스마트워치입니다.', 'assets/avatar-items/accessory-smartwatch.png', NULL, 'male,female', FALSE, TRUE),
  ('accessory-gold-medal', '골드 메달', 380, 'neck', 'accessory', '기타', 4, '능력치 없음 · 외형 전용', '꾸준한 운동을 기념하는 골드 메달입니다.', 'assets/avatar-items/accessory-gold-medal.png', NULL, 'male,female', FALSE, TRUE),
  ('top-white-tank', '화이트 러닝 탱크톱', 170, 'top', NULL, '상의', 1, '능력치 없음 · 외형 전용', '더운 날 가볍게 입는 민소매 러닝 탱크톱입니다.', 'assets/avatar-items/top-white-tank.png', NULL, 'male,female', FALSE, TRUE),
  ('top-navy-longsleeve', '네이비 긴팔 기능성 티', 260, 'top', NULL, '상의', 2, '능력치 없음 · 외형 전용', '손목까지 내려오는 흡습속건 긴팔 티셔츠입니다.', 'assets/avatar-items/top-navy-longsleeve.png', NULL, 'male,female', FALSE, TRUE),
  ('outer-orange-vest', '오렌지 바람막이 베스트', 330, 'outer', NULL, '아우터', 3, '능력치 없음 · 외형 전용', '티셔츠 위에 겹쳐 입는 경량 바람막이 베스트입니다.', 'assets/avatar-items/outer-orange-vest.png', NULL, 'male,female', FALSE, TRUE),
  ('bottom-black-leggings', '블랙 러닝 레깅스', 280, 'bottom', NULL, '하의', 2, '능력치 없음 · 외형 전용', '다리에 밀착되는 블랙 러닝 레깅스입니다.', 'assets/avatar-items/bottom-black-leggings.png', NULL, 'male,female', FALSE, TRUE),
  ('bottom-grey-training-pants', '그레이 트레이닝 팬츠', 290, 'bottom', NULL, '하의', 2, '능력치 없음 · 외형 전용', '발목까지 내려오는 그레이 긴 트레이닝 팬츠입니다.', 'assets/avatar-items/bottom-grey-training-pants.png', NULL, 'male,female', FALSE, TRUE),
  ('socks-white-ankle', '화이트 발목 양말', 80, 'socks', NULL, '양말', 1, '능력치 없음 · 외형 전용', '운동화 위로 살짝 보이는 발목 양말입니다.', 'assets/avatar-items/socks-white-ankle.png', NULL, 'male,female', FALSE, TRUE),
  ('socks-orange-crew', '오렌지 크루 양말', 120, 'socks', NULL, '양말', 1, '능력치 없음 · 외형 전용', '종아리 중간까지 올라오는 오렌지 크루 양말입니다.', 'assets/avatar-items/socks-orange-crew.png', NULL, 'male,female', FALSE, TRUE),
  ('head-white-visor', '화이트 러닝 바이저', 200, 'head', NULL, '헤어', 2, '능력치 없음 · 외형 전용', '햇빛을 막아주는 챙만 있는 러닝 바이저입니다.', 'assets/avatar-items/head-white-visor.png', NULL, 'male,female', FALSE, TRUE),
  ('eyewear-sport-sunglasses', '스포츠 선글라스', 250, 'eyewear', NULL, '아이웨어', 2, '능력치 없음 · 외형 전용', '미러 렌즈가 들어간 러닝용 스포츠 선글라스입니다.', 'assets/avatar-items/eyewear-sport-sunglasses.png', NULL, 'male,female', FALSE, TRUE),
  ('hair-pink-scrunchie', '핑크 스크런치', 90, 'hair', NULL, '헤어', 1, '능력치 없음 · 외형 전용', '옆머리에 포인트를 주는 핑크 스크런치입니다.', 'assets/avatar-items/hair-pink-scrunchie.png', NULL, 'female', FALSE, TRUE),
  ('bag-black-hipsack', '블랙 러닝 힙색', 300, 'bag', NULL, '가방', 2, '능력치 없음 · 외형 전용', '허리에 두르는 슬림한 러닝 힙색입니다.', 'assets/avatar-items/bag-black-hipsack.png', NULL, 'male,female', FALSE, TRUE),
  ('hands-handheld-bottle', '핸드헬드 물병', 150, 'hands', NULL, '기타', 1, '능력치 없음 · 외형 전용', '손에 끼워 들고 달리는 핸드헬드 물병입니다.', 'assets/avatar-items/hands-handheld-bottle.png', NULL, 'male,female', FALSE, TRUE),
  ('legwear-black-kneepads', '블랙 무릎 보호대', 180, 'legwear', NULL, '보호대', 2, '능력치 없음 · 외형 전용', '양쪽 무릎을 감싸는 블랙 무릎 보호대입니다.', 'assets/avatar-items/legwear-black-kneepads.png', NULL, 'male,female', FALSE, TRUE),
  ('legwear-orange-calf-sleeves', '오렌지 종아리 슬리브', 190, 'legwear', NULL, '보호대', 2, '능력치 없음 · 외형 전용', '종아리를 압박해 주는 오렌지 슬리브입니다.', 'assets/avatar-items/legwear-orange-calf-sleeves.png', NULL, 'male,female', FALSE, TRUE),
  ('hands-running-gloves', '네이비 러닝 장갑', 160, 'hands', NULL, '기타', 2, '능력치 없음 · 외형 전용', '겨울 러닝용 네이비 터치 장갑입니다.', 'assets/avatar-items/hands-running-gloves.png', '["assets/avatar-items/hands-running-glove-left-layer.png", "assets/avatar-items/hands-running-glove-right-layer.png"]', 'male,female', FALSE, TRUE),
  ('neck-grey-neckwarmer', '그레이 넥워머', 170, 'neck', NULL, '기타', 2, '능력치 없음 · 외형 전용', '목을 따뜻하게 감싸는 그레이 넥워머입니다.', 'assets/avatar-items/neck-grey-neckwarmer.png', NULL, 'male,female', FALSE, TRUE),
  ('background-riverside-day', '배경 - 맑은 강변 산책로', 250, 'background', NULL, '배경', 1, '능력치 없음 · 외형 전용', '푸른 하늘과 강변 러닝 코스가 펼쳐진 밝은 낮 배경입니다.', 'assets/avatar-items/background-riverside-day.png', NULL, 'male,female', FALSE, TRUE),
  ('background-riverside-sunset', '배경 - 노을빛 강변', 300, 'background', NULL, '배경', 2, '능력치 없음 · 외형 전용', '주황빛 노을이 물든 강변 러닝 코스 배경입니다.', 'assets/avatar-items/background-riverside-sunset.png', NULL, 'male,female', FALSE, TRUE),
  ('background-autumn-lake', '배경 - 가을 호수 공원', 350, 'background', NULL, '배경', 3, '능력치 없음 · 외형 전용', '단풍과 호수가 어우러진 따뜻한 가을 공원 배경입니다.', 'assets/avatar-items/background-autumn-lake.png', NULL, 'male,female', FALSE, TRUE),
  ('background-rainy-park', '배경 - 비 오는 가로수길', 320, 'background', NULL, '배경', 3, '능력치 없음 · 외형 전용', '가로등 불빛이 비치는 차분한 빗속 공원 배경입니다.', 'assets/avatar-items/background-rainy-park.png', NULL, 'male,female', FALSE, TRUE);