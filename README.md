# hometraining-api

우리동네홈트챌린지(`smhrd-hc-prototype`) 프론트엔드 목업을 실제 서버·DB와 연결하기 위한 백엔드입니다.
Java 17 + Spring Boot 3 + Spring Data JPA + Spring Security(JWT) + MySQL 조합으로 구성했습니다.

프론트엔드(`../smhrd-hc-prototype`)는 건드리지 않았습니다 — 이 폴더는 완전히 별도 프로젝트이며,
나중에 `script.js`의 `state` 목업 값을 이 서버가 내려주는 API 응답으로 바꿔치기하면 연동됩니다.

## 0. 시작 전에 설치할 것

실행하려면 **JDK 17 이상**이 필요하고, 실제 데이터를 보존하려면 **MySQL 8.x**가 필요합니다.
처음 동작을 확인할 때는 MySQL 없이 H2 메모리 DB를 사용할 수 있습니다.

1. **JDK 17 이상** — [Eclipse Temurin](https://adoptium.net/) 에서 17 LTS Windows x64 `.msi` 설치.
   설치 후 새 터미널에서 `java -version` 이 떠야 합니다.
2. **MySQL Community Server 8.x** — [MySQL 공식 다운로드](https://dev.mysql.com/downloads/installer/) 에서
   "MySQL Installer for Windows" → **Developer Default** 구성으로 설치.
   설치 중 지정하는 **root 비밀번호를 꼭 기억**해두세요.

Maven은 따로 설치할 필요 없습니다. 이 프로젝트에 포함된 Maven Wrapper(`mvnw.cmd`)가
첫 실행 시 필요한 Maven을 내려받습니다.

## 1. DB 준비 (MySQL)

MySQL 설치 후, 아무 SQL 클라이언트(MySQL Workbench, 또는 `mysql -u root -p`)에서 한 번만 실행:

```sql
CREATE DATABASE hometraining CHARACTER SET utf8mb4;
CREATE USER 'hometraining'@'localhost' IDENTIFIED BY 'hometraining1234';
GRANT ALL PRIVILEGES ON hometraining.* TO 'hometraining'@'localhost';
FLUSH PRIVILEGES;
```

테이블은 직접 만들 필요 없습니다 — `ddl-auto: update` 설정으로 Spring Boot가 엔티티를 보고 자동 생성합니다.

계정/비밀번호를 다르게 쓰고 싶다면 테이블을 만드는 대신 환경변수로 덮어쓰면 됩니다 (`DB_USER`, `DB_PASSWORD`, `DB_NAME` — 아래 표 참고).

## 2. 실행하기

### 방법 A — MySQL로 바로 실행 (기본값)

```
cd smhrd-hc-backend
.\mvnw.cmd clean package
java -jar .\target\hometraining-api-0.1.0.jar
```

### 방법 B — MySQL 설치 전에 우선 동작만 확인 (메모리 DB, 설정 불필요)

```
.\mvnw.cmd clean package
java -jar .\target\hometraining-api-0.1.0.jar --spring.profiles.active=h2
```

서버를 껐다 켜면 데이터가 사라지는 임시 DB입니다. 코드가 도는지 빨리 확인할 때만 쓰세요.

기동되면 `http://localhost:8080` 에서 API가 뜹니다.

8080 포트를 다른 프로그램이 사용 중이면 마지막 명령에 `--server.port=18080`을 추가하세요.

## 3. 빠른 동작 확인 (curl)

```
# 회원가입 → JWT 토큰 발급
curl -X POST http://localhost:8080/api/auth/signup -H "Content-Type: application/json" -d "{\"loginId\":\"tester01\",\"password\":\"testpass123\",\"email\":\"tester01@test.com\",\"nickname\":\"써니핏\",\"gender\":\"male\",\"regionCity\":\"서울시\",\"regionGu\":\"강남구\",\"regionDong\":\"역삼동\"}"

# 위 응답의 accessToken을 넣어서 내 정보 조회
curl http://localhost:8080/api/users/me -H "Authorization: Bearer <accessToken>"
```

## 4. 환경변수

기본값은 로컬 개발용으로 이미 채워져 있어 아무것도 설정하지 않아도 됩니다. 바꾸고 싶을 때만 설정하세요.

| 변수 | 기본값 | 설명 |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | localhost / 3306 / hometraining | MySQL 접속 정보 |
| `DB_USER` / `DB_PASSWORD` | hometraining / hometraining1234 | 위 1단계에서 만든 계정 |
| `JWT_SECRET` | (개발용 기본값) | 운영 배포 전에는 반드시 바꿔야 함 |
| `CORS_ORIGINS` | `http://localhost:5500,http://127.0.0.1:5500` | 프론트를 다른 주소로 띄우면 여기에 추가 |

## 5. 프론트엔드(`smhrd-hc-prototype`)와 연결하려면

`index.html`을 `file://`로 직접 열면 CORS 때문에 요청이 막힙니다. VSCode의 **Live Server** 확장 등으로
`http://localhost:5500` 같은 주소로 띄운 뒤 `script.js`에서 `state` 목업 대신 `fetch('http://localhost:8080/api/...')`
호출로 바꿔가는 방식으로 연동하면 됩니다. (이 단계는 아직 진행 전 — 필요하시면 이어서 요청해주세요.)

## 6. 패키지 구조

```
src/main/java/com/smhrd/hometraining/
  auth/       회원가입·로그인·소셜로그인·아이디/비번찾기 (JWT 발급)
  user/       내 정보 조회·수정, 캘리브레이션 저장, 연속출석 보상, 회원탈퇴
  exercise/   종목 카탈로그, 운동기록 저장/조회 (포인트·경험치 지급 트리거)
  mission/    일간 미션 생성·진행도·보상 수령
  shop/       상점 아이템 카탈로그·구매·장착
  crew/       크루 생성/가입신청/승인/강퇴/탈퇴, 공지, 실시간 채팅(WebSocket)
  ranking/    지역별·종목별·크루 랭킹 조회
  support/    고객센터 문의 등록/조회/답변
  security/   JWT 발급·검증, Spring Security 연동
  config/     보안·CORS·WebSocket 설정, 초기 데이터 시딩
  common/     공통 응답 포맷, 예외 처리
```

## 7. 이번에 단순화한 부분 (다음 단계에서 다듬으면 좋은 것)

- **자세 판정(AI) 자체는 여전히 프론트엔드(MediaPipe, 브라우저) 몫입니다.** 서버는 프론트가 계산한
  최종 결과(reps·정확도·점수·판정 분포)만 받아서 저장·보상 지급을 합니다 — script.js 주석의 설계와 동일합니다.
- 아이디/비밀번호 찾기: 실제 이메일 발송 연동이 없어 응답에 임시 비밀번호를 바로 내려줍니다.
  운영 전에는 반드시 이메일 발송으로 바꾸고 응답에서 제거해야 합니다.
- 소셜 로그인: 카카오/네이버/구글의 실제 OAuth 토큰 교환 없이, 클라이언트가 보낸 값으로 바로 계정을
  만들거나 조회하는 목업입니다.
- 5vs5 크루대전의 실시간 스코어링(초 단위 랠리)은 아직 없습니다 — 크루 그룹미션(오늘 스쿼트 누적 횟수)
  진행도만 구현했습니다.
- 크루 채팅(WebSocket)은 CONNECT 프레임에 `Authorization: Bearer <JWT>` 네이티브 헤더를 실어 보내야
  인증됩니다 — 프론트에서 STOMP 클라이언트 연결 시 이 부분을 챙겨야 합니다.
