# Spring Boot 백엔드 적용 패치

첨부 파일에 백엔드 프로젝트가 포함되지 않아, 아래 파일은 기존 Spring Boot 프로젝트에 옮겨 적용하는 기준 코드입니다. `com.ounhome.crew` 패키지와 엔티티명은 실제 프로젝트에 맞게 변경하세요.

- `db/V20260916__crew_bugfix.sql`: 컨셉 배열과 가입 상태 스키마
- `CrewJoinRequestStatus.java`: 가입 요청 상태 enum
- `CrewDtos.java`: 멤버·컨셉·가입 요청 응답 계약
- `CrewQueryServicePatch.java`: 크루장을 포함한 멤버 조회, PENDING 대기열, 컨셉 변환
- `CrewCommandServicePatch.java`: PENDING 생성과 중복 방지
- `CrewControllerPatch.java`: 프론트엔드가 호출하는 API 예시

가입 요청 생성·승인·거절 트랜잭션이 성공한 뒤 `/topic/crews/{crewId}/join-requests`에 이벤트를 발행해야 프론트 목록이 즉시 갱신됩니다.
