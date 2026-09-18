# SD 캐릭터 아이템 적용 안내

## 포함된 파일

- `assets/avatar-male-sd.png`: 남자 기본 캐릭터
- `assets/avatar-female-sd.png`: 여자 기본 캐릭터
- `assets/avatar-items/*.png`: 아이템별 투명 PNG
- `js/avatar-items.js`: 상품 정보, 이미지 경로, 캔버스 착용 좌표

## 적용 흐름

1. `index.html`에서 `avatar-items.js`를 `state.js`보다 먼저 불러옵니다.
2. `state.js`가 `AVATAR_ITEM_CATALOG`을 상점 상품 목록에 추가합니다.
3. `utils.js`가 기본 캐릭터와 아이템 이미지를 미리 불러옵니다.
4. `profile.js`의 `getEquipState()`가 부위별 착용 아이템을 선택합니다.
5. `drawPixelCharacter()`가 기본 캐릭터를 그린 뒤 `drawAvatarWearables()`로 아이템을 합성합니다.
6. `shop.js`의 미리보기에서도 같은 합성 함수를 사용합니다.

## 착용 부위

- `head`: 헤드밴드, 모자, 왕관
- `top`: 티셔츠, 재킷, 후디
- `bottom`: 반바지, 트랙 팬츠, 조거 팬츠
- `shoes`: 러닝화, 운동화, 하이탑
- `accessory`: 손목밴드, 스마트워치, 메달

같은 `slot`에 속하는 아이템은 한 번에 하나만 착용됩니다.

## 위치 조정

`js/avatar-items.js`에서 각 아이템의 `placement` 값을 수정합니다.

```js
placement: { x: 35, y: 58, w: 74, h: 49 }
```

- `x`, `y`: 144×176 캐릭터 캔버스에서 아이템의 왼쪽 위 좌표
- `w`, `h`: 아이템을 그릴 크기
- `z`: 아이템끼리 겹칠 때의 순서

손목밴드처럼 한 상품을 여러 위치에 그려야 할 때는 `layers` 배열을 사용합니다.

## 현재 범위

프론트엔드에서 구매, 미리보기, 착용·해제와 화면 반영까지 동작합니다. 서버와 데이터베이스에 신규 15종 상품을 영구 저장하려면 백엔드 상품 초기 데이터와 API 응답에도 동일한 `slot` 및 이미지 식별자를 추가해야 합니다.
