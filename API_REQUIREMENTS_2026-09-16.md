# 오운홈 프론트엔드 개편용 API 요구사항

프론트는 서버 필드가 있으면 서버 값을 우선 사용하고, 없는 항목은 `requirements-v2.js`의 목데이터로 표시합니다.

## 사용자/레벨

- `GET /api/users/me`: `gender`, `avatar`, `gradeIndex`, `level(1~500)`, `currentExp`, `nextLevelExp`, `points`, `calibrated`, `freeWorkoutsUsed`, `freeWorkoutDate(KST)`, `retakeTickets`
- 레벨 500 초과 처리와 초과 경험치 이월은 서버에서 계산한 결과를 반환합니다.

## 운동

- `POST /api/exercise-records` 요청 추가 필드: `sessionType(FREE|TICKET)`, `durationSeconds`, 등급별 횟수
- 응답: `score`, `experienceAwarded`, `pointsAwarded`, `highScoreUpdated`, `freeWorkoutsUsed`, `freeWorkoutsRemaining`, `retakeTickets`
- 카메라 연결 전에는 무료 횟수/티켓을 차감하지 않고, 연결 실패 응답 또는 클라이언트 오류 시 `consumed=false`를 보장해야 합니다.
- 티켓 운동은 경험치 0이며 최고점일 때만 랭킹 최고점이 갱신됩니다.

## 미션/출석

- `GET /api/missions/today`: 정확히 3개, `target/current/achieved/claimed/pointsReward/expReward/sessionEligibility`
- 티켓 운동은 경험치 보상 미션 진행도에서 제외합니다.
- `POST /api/attendance/claim`: `streakDays`, `basePoints=5`, `bonusPoints=10(3일 이상)`, `totalPoints`, `claimed`

## 크루

- 목록: `currentMembers`, `maxMembers=5`, `joinEnabled` 포함. 비활성 크루도 검색 결과에 포함합니다.
- 생성: 비용 1,000P를 서버 트랜잭션으로 차감합니다.
- `PATCH /api/crews/me/join-status`, `POST /api/crews/me/transfer-leader`, `POST /api/crews/me/leave`, `DELETE /api/crews/me`
- 주간 미션: 목표 300, 멤버별 `weeklyContribution`(적용 40~80), 완료 보상 크루 EXP 500
- 대전: 자동매칭 요청/상태/진행/결과 API. 2~5인 동일 인원 매칭, 제한 120초, 주간 획득 EXP 상한 500

## 랭킹

- 크루: `ORDER BY crew_level DESC, current_exp DESC, reached_at ASC`
- 지역/종목: 해당 운동 최고점 기준. 티켓 기록은 기존 최고점 초과 시에만 반영합니다.

## 관리자 미션

- 개인/크루 미션 CRUD 및 활성화 API가 필요합니다.
- 필드: `type`, `exerciseType`, `metric`, `target`, `minTarget`, `maxTarget`, `pointsReward`, `expReward`, `active`.
# 2026-09-16 버그 수정 추가 계약

- 회원가입 `POST /api/auth/signup`: `gender`(`male|female`)와 선택형 `referrerId`를 저장하고 응답에도 `gender`를 반환해야 합니다.
- 사용자 프로필 `GET /api/users/me`: 재로그인/새로고침 복원을 위해 `gender`, `currentExp`, `nextLevelExp`를 반환해야 합니다.
- 크루 생성/수정: `concepts: string[]`(최대 3개)을 저장하고 응답에도 배열로 반환해야 합니다. `concept`은 이전 서버 호환 필드입니다.
- 크루 가입 요청: 서버에서도 `joinEnabled=false`, `recruiting=false`, 정원 초과를 검사해 4xx로 거절해야 합니다.
- `GET /api/crews/me`의 `members`에는 크루장도 `role: "LEADER"`로 반드시 포함해야 합니다. 별도 `leader` 필드를 함께 내려도 프론트에서 중복 제거합니다.
- `POST /api/crews/{crewId}/join-requests`는 항상 최초 상태를 `PENDING`으로 저장하고, 중복 PENDING 요청을 거절해야 합니다.
- `GET /api/crews/me/join-requests?status=PENDING`은 크루장 권한을 확인하고 PENDING 요청만 최신순으로 반환해야 합니다.
- 가입 신청 생성·승인·거절 시 `/topic/crews/{crewId}/join-requests`로 변경 이벤트를 발행하면 크루장 화면이 즉시 갱신됩니다.
