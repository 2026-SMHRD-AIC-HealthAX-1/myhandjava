# 오운홈(OunHome) — 백엔드

AI 자세 분석 홈트레이닝 + 동네 크루 커뮤니티 플랫폼의 백엔드입니다.
Java 17 + Spring Boot 3 + Spring Data JPA + Spring Security(JWT) + MySQL 조합으로 구성했습니다.

프론트엔드는 이 저장소가 아니라 **별도 폴더/브랜치(front)** 로 관리됩니다 — 순수 자바스크립트 SPA이며,
이 서버가 내려주는 REST API + WebSocket(STOMP)으로 연동됩니다.

## 0. 시작 전에 설치할 것

실행하려면 **JDK 17 이상**만 있으면 됩니다. DB는 로컬에 새로 설치할 필요 없이, 팀에서 같이 쓰는
**공유 MySQL(project-db-campus.smhrd.com)** 에 접속합니다 — 접속 계정(DB_USER/DB_PASSWORD)은
팀 내부적으로 공유된 값을 쓰세요(이 파일에는 올리지 않습니다).

1. **JDK 17 이상** — [Eclipse Temurin](https://adoptium.net/) 에서 17 LTS 설치 후 `java -version` 확인.
2. Maven은 따로 설치할 필요 없습니다 — 이 프로젝트에 포함된 Maven Wrapper(`mvnw`/`mvnw.cmd`)가
   첫 실행 시 필요한 Maven을 내려받습니다.

## 1. DB

⚠️ **이 프로젝트는 `ddl-auto: none`입니다** — Spring Boot가 테이블을 자동으로 만들거나 바꾸지
않습니다. 엔티티(`@Entity`)만 고치고 실제 DB 컬럼/테이블을 안 맞추면 애플리케이션은 뜨지만 그
기능을 쓸 때 SQL 에러가 납니다. 스키마를 바꿔야 하면 팀 내에서 공지하고 공유 DB에 직접
`ALTER TABLE`/`CREATE TABLE`을 실행해야 합니다.

로컬 개인 DB로 테스트하고 싶다면 `src/main/resources/application-mysql.yml`의 데이터소스
설정을 참고해서 아래 환경변수로 자기 MySQL을 가리키면 됩니다(테이블은 직접 만들어야 함).

## 2. 실행하기

```bash
./mvnw -o package -DskipTests
DB_HOST=project-db-campus.smhrd.com DB_PORT=3312 DB_NAME=cd_26K_HI1_p2_3 \
DB_USER=<팀 공유 계정> DB_PASSWORD=<팀 공유 비밀번호> \
java -jar target/hometraining-api-0.1.0.jar --spring.profiles.active=mysql
```

Windows PowerShell에서는:

```powershell
$env:DB_HOST="project-db-campus.smhrd.com"; $env:DB_PORT="3312"; $env:DB_NAME="cd_26K_HI1_p2_3"
$env:DB_USER="<팀 공유 계정>"; $env:DB_PASSWORD="<팀 공유 비밀번호>"
java -jar target\hometraining-api-0.1.0.jar --spring.profiles.active=mysql
```

기동되면 `http://localhost:8086` 에서 API가 뜹니다(포트는 `application.yml`의 `server.port`).
다른 프로그램이 8086을 쓰고 있으면 마지막 명령에 `--server.port=18086` 을 추가하세요.

카카오/구글 소셜 로그인을 실제로 테스트하려면 `src/main/resources/application-secrets.yml`
(gitignore됨, 로컬에만 존재)에 팀에서 공유한 OAuth 키를 넣어야 합니다 — 없으면 소셜 로그인만
비활성 상태로 서버는 정상 기동됩니다.

## 3. 빠른 동작 확인 (curl)

프론트는 SNS(카카오/구글) 로그인만 쓰지만, 아이디/비밀번호 회원가입·로그인 API는 백엔드에
그대로 남아있어서 curl로 빠르게 찔러볼 수 있습니다.

```bash
# 회원가입 → JWT 토큰 발급
curl -X POST http://localhost:8086/api/auth/signup -H "Content-Type: application/json" \
  -d "{\"loginId\":\"tester01\",\"password\":\"testpass123\",\"email\":\"tester01@test.com\",\"nickname\":\"써니핏\",\"gender\":\"male\",\"regionCity\":\"서울시\",\"regionGu\":\"강남구\",\"regionDong\":\"역삼동\"}"

# 위 응답의 accessToken을 넣어서 내 정보 조회
curl http://localhost:8086/api/users/me -H "Authorization: Bearer <accessToken>"
```

## 4. 환경변수

| 변수 | 기본값 | 설명 |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | project-db-campus.smhrd.com / 3312 / cd_26K_HI1_p2_3 | 공유 MySQL 접속 정보 |
| `DB_USER` / `DB_PASSWORD` | (없음, 필수) | 팀 내부에서 공유하는 계정 — 이 파일에는 안 올림 |
| `JWT_SECRET` | (개발용 기본값) | 운영 배포 전에는 반드시 바꿔야 함 |
| `CORS_ORIGINS` | `http://localhost:5500,http://127.0.0.1:5500,...` | 프론트를 다른 주소로 띄우면 여기에 추가 |
| `KAKAO_REST_API_KEY` / `KAKAO_REDIRECT_URI` | (없음) | 카카오 로그인용 — 보통 `application-secrets.yml`로 관리 |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` / `GOOGLE_REDIRECT_URI` | (없음) | 구글 로그인용 |

## 5. 패키지 구조

```
src/main/java/com/smhrd/hometraining/
  auth/       로그인(SNS 전용, 아이디/비번 API는 유지)·소셜로그인·JWT 발급
  user/       내 정보 조회·수정, 소셜 온보딩, 캘리브레이션 저장, 연속출석 보상, 회원탈퇴
  exercise/   종목 카탈로그, 운동 세션(exercise.session)·운동기록 저장/조회 (포인트·경험치 지급)
  mission/    일간 미션(개인)·미션 원본(관리자 등록) 관리
  crew/       크루 생성/가입/탈퇴/공지/채팅(WebSocket)/주간미션, 크루대전(crew.battle) 실시간 매칭·진행
  shop/       상점 아이템 카탈로그·구매·장착
  ranking/    지역별·종목별·크루 랭킹 조회 (별도 랭킹 테이블 없이 매 요청 집계)
  support/    고객센터 문의 등록/조회/답변
  admin/      관리자모드 대시보드 집계 API
  security/   JWT 발급·검증, Spring Security 연동
  config/     보안·CORS·WebSocket 설정, 초기 데이터 시딩(DataSeeder)
  common/     공통 응답 포맷, 예외 처리
```

각 컨트롤러/리포지토리 파일 상단에 **[담당]/[프론트 연동]/[DB]/[주의]** 형식 주석을 달아뒀으니,
특정 기능이 프론트 어느 파일과 이어지는지·DB 어느 테이블까지 가는지는 해당 파일을 열어서 확인하세요.

## 6. 알아두면 좋은 것

- **자세 판정(AI) 자체는 프론트엔드(MediaPipe, 브라우저) 몫입니다.** 서버는 프론트가 계산한
  최종 결과(reps·정확도·점수·판정 분포)만 받아서 저장·보상 지급을 합니다.
- **소셜 로그인은 실제 OAuth 연동입니다** — 카카오/구글 인가 코드를 서버가 각 사 API로 교환해서
  진짜 사용자 정보를 받아온 뒤 계정을 생성/조회합니다(목업 아님).
- **크루대전은 실시간으로 동작합니다** — WebSocket(STOMP)으로 렙 판정 결과를 즉시 반영하고
  양팀에 브로드캐스트합니다. 매칭 시 동시 요청 경합을 막기 위해 비관적 락을 씁니다.
- **관리자 권한 부여는 화면에서 할 수 없습니다** — 특정 계정을 관리자로 만들려면 DB에서 직접
  `UPDATE users SET role='ADMIN' WHERE id=?` 를 실행해야 합니다.
- **정지(SUSPENDED) 계정은 다음 로그인부터 거부됩니다** — 이미 발급된 토큰을 즉시 무효화하지는
  않습니다(토큰 블랙리스트 없음).
- 크루 채팅(WebSocket)은 CONNECT 프레임에 `Authorization: Bearer <JWT>` 네이티브 헤더를 실어
  보내야 인증됩니다.
