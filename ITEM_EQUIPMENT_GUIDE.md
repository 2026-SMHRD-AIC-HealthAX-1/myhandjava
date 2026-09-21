# 아이템 실제 착용 기능 적용본

이 버전은 제공된 남/여 캐릭터 PNG를 기본 캐릭터로 사용하고, 상의/하의/신발/왕관 등의 투명 PNG를 캐릭터 위에 레이어로 겹쳐 실제 착용처럼 표시합니다.

## 적용된 기능

- `getEquipState()`가 `true` 대신 실제 아이템 객체를 전달
- `drawPixelCharacter()`가 기본 캐릭터 → 아이템 레이어 순서로 렌더링
- 아이템별 `image`, `layer`, `fit` 속성 지원
- `fit.x/y/w/h`는 0~1 비율이라 캐릭터가 작아지거나 커져도 아이템이 함께 크기 조절됨
- 상점 `착용해보기`에서 선택한 아이템이 실제로 캐릭터에 표시됨
- 상의/하의/신발/왕관 샘플 아이템 및 투명 PNG 포함
- 제공된 캐릭터 이미지로 `assets/avatar-male.png`, `assets/avatar-female.png` 교체

## 아이템 추가 방법

`assets/items/male/`, `assets/items/female/`에 투명 PNG를 넣고 `js/state.js`의 아이템에 다음 속성을 추가합니다.

```js
{
  name: '새 운동복',
  price: 300,
  owned: false,
  equipped: false,
  slot: 'outfit',
  category: '상의',
  layer: 40,
  image: {
    male: 'assets/items/male/new-outfit.png',
    female: 'assets/items/female/new-outfit.png'
  },
  fit: {
    male: { x: 0, y: 0, w: 1, h: 1 },
    female: { x: 0, y: 0, w: 1, h: 1 }
  },
  levelReq: 1,
  effect: '능력치 없음 · 외형 전용',
  effectDesc: '캐릭터가 새 운동복을 착용합니다.'
}
```

### 권장 슬롯/레이어

- 하의: `bottom`, layer 45
- 상의: `outfit`, layer 40
- 신발: `shoes`, layer 50
- 머리 장식: `crown` 또는 `hat`, layer 80
- 액세서리: `accessory`, layer 90

실제 서비스에서 더 다양한 옷을 넣을 경우에는 **캐릭터와 동일한 캔버스 비율로 정렬된 투명 PNG**를 사용하는 것이 가장 정확합니다.

## 실행

기존 프로젝트와 동일하게 `index.html`을 실행하면 됩니다.

1. 포인트 상점 → 상의/하의/신발/헤어
2. `착용해보기` 클릭
3. 캐릭터에 실제 아이템이 겹쳐서 표시되는지 확인
4. 구매 후 `착용하기`를 누르면 마이페이지/메인/상단바 캐릭터에도 반영
