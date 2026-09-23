# 오운홈(OunHome) — 프론트엔드

AI 자세 분석 홈트레이닝 + 동네 크루 커뮤니티 플랫폼의 프론트엔드입니다. 번들러 없는 순수
자바스크립트 SPA로, 백엔드(Java/Spring Boot, 별도 폴더/브랜치 `back`)가 내려주는 REST API +
WebSocket(STOMP)으로 실시간 동작합니다 — 더 이상 목업 데이터가 아니라 실제 서버와 연동됩니다.

번들러(Webpack/Vite 등)는 쓰지 않습니다 — 일반 `<script>` 태그 여러 개를 순서대로 로드하는
방식이라, 정적 파일 서버로 열면 바로 동작합니다.

## 실행 방법

번들러나 별도 설치 없이, 정적 파일 서버로만 열면 됩니다.

- VSCode "Live Server" 확장 → `index.html` 우클릭 → Open with Live Server
- 또는 Node가 있다면: `npx serve .`

`file://`로 직접 열면 카메라 권한과 CORS(백엔드 연동) 때문에 기능이 막히니, 위 방법 중 하나로
`http://localhost:...` 주소로 띄워서 확인하세요. 백엔드(`back` 브랜치)를 먼저 실행해서
`js/data.js` 등의 `API_BASE`가 가리키는 주소로 떠 있어야 로그인·운동 저장 등이 동작합니다.

## 폴더 구조

```
index.html
style.css
Bodyweight_Squats.gif      스쿼트 레퍼런스 영상(운동 튜토리얼)
squat-bottom-ref.png       스쿼트 최저점 실루엣 레퍼런스(캘리브레이션 가이드용)
assets/                    아바타 아이템·랭킹 등급 아이콘·상점 아이콘 이미지
js/
  data.js             정적 데이터(운동 종목, 지역 드롭다운 등)
  avatar-items.js     캐릭터 꾸미기 아이템 카탈로그
  state.js            전역 상태 객체 — 모든 화면이 공유
  utils.js            공용 유틸(토스트, 확인모달, 아바타 색상, 등급 색상 등)
  router.js           화면 라우팅(render) + 앱 셸(사이드바/탑바)
  landing.js          로그인 전 랜딩 페이지 + 비회원 체험 진입
  auth.js             로그인(SNS 전용)/소셜로그인 콜백/최초 로그인 온보딩
  calibration.js      카메라 체형 캘리브레이션(MediaPipe Pose)
  main.js             로그인 후 메인 대시보드
  exercise.js         운동(카메라 실시간 자세 판정) — 가장 큰 파일
  mission.js          오늘의 미션
  profile.js          마이페이지(캐릭터 꾸미기/미션현황/히스토리/계정관리)
  shop.js             포인트 상점
  crew.js             홈크루(생성/가입/채팅/크루대전 파티맺기 등)
  ranking.js          랭킹(지역별/종목별/크루)
  support.js          고객센터
  admin.js            관리자모드(대시보드/회원관리/신고관리/문의관리/미션관리)
  requirements-v2.js  기존 함수를 나중에 덮어쓰는 화면/정책 개편 모음 — 아래 "주의" 참고
  bootstrap.js         세션 복원 + render() 최초 호출 (항상 마지막 로드)
```

각 `js/*.js` 파일 맨 위에 **[담당]/[백엔드 연동]/[주의]** 형식 주석을 달아뒀습니다 — 이 화면이
백엔드 어느 엔드포인트를 부르는지, 뭘 조심해야 하는지는 해당 파일을 열어서 먼저 확인하세요.

로드 순서는 `index.html`의 `<script>` 태그 순서를 그대로 따릅니다(위 목록과 동일한 순서).
실제로 순서를 지켜야 하는 건 `data.js → state.js`, `bootstrap.js를 항상 마지막에 두는 것`,
그리고 **`requirements-v2.js`는 반드시 자신이 덮어쓰는 원본 파일들보다 나중에 로드돼야 하는 것**
세 가지입니다.

## ⚠️ `requirements-v2.js`를 고칠 때 주의

이 파일은 다른 파일에 이미 정의된 함수를 나중에 통째로 덮어쓰는 "몽키패치" 모음입니다.

```js
const _renderFooV1 = renderFoo;
renderFoo = function(){ ...; return _renderFooV1(); };
```

이런 패턴으로 `renderFoo`를 교체하므로, 원본 파일(`main.js`/`crew.js` 등)에서 `renderFoo`를
고쳐도 `requirements-v2.js`가 같은 이름을 다시 덮어쓰고 있으면 그 수정이 조용히 무시됩니다.
원본 함수를 고칠 땐 `requirements-v2.js`에 같은 이름이 있는지 먼저 검색해보세요.

## 백엔드 연동 현황

- **회원가입/로그인은 SNS(카카오/구글) 전용입니다.** 아이디/비밀번호 화면은 없습니다(백엔드
  API 자체는 남아있음).
- 운동 결과 저장, 미션, 상점, 크루, 랭킹, 고객센터, 관리자모드까지 **전부 실제 백엔드 API와
  연동되어 있습니다** — 목업으로 남은 큰 기능은 없습니다.
- 크루채팅·크루대전 실시간 갱신·크루대전 파티 초대는 WebSocket(SockJS + STOMP, `/ws`)으로
  동작합니다. 연결 시 STOMP CONNECT 프레임에 `Authorization: Bearer <JWT>` 헤더가 실려야
  인증되니, 백엔드가 꺼져 있거나 토큰이 없으면 크루 관련 실시간 기능이 안 됩니다.
- 관리자모드(`admin.js`)는 로그인 계정의 `role`이 `'ADMIN'`이어야 사이드바에 진입 버튼이
  보입니다 — 권한 부여는 화면에 없고 DB에서 직접 바꿔야 합니다(백엔드 README 참고).

## 최근 변경사항 (2026-09-23)

- **마이프로필 등급 배지 3D 회전 미리보기 추가** — `js/tier3d.js`(새 파일, ES 모듈)가
  three.js로 `assets/models/tiers/*.glb`(아이언~챌린저 10개 등급 모델)를 불러와 마이프로필
  화면의 등급 배지를 평면 아이콘 대신 계속 회전하는 3D 모델로 보여준다. `index.html`에
  three.js importmap + `tier3d.js` 모듈 스크립트 추가, `router.js`가 마이프로필 진입 시
  `window.renderTier3D()`를 호출하도록 훅 추가, `profile.js`의 배지 자리를 `<canvas>`로 교체.
- **랜딩페이지 로고/소개 카드 영상 교체** — `js/landing.js`: 상단 로고 영상 파일 교체
  (`assets/landing-logo-new.mp4`), 하단 4개 소개 카드의 정적 이미지를 짧은 영상으로 교체하고
  마우스를 올렸을 때만 재생되도록 변경(평소엔 첫 프레임에서 정지, 모바일은 터치 시 재생).
