-- MySQL 8 기준. 운영 반영 전 백업 및 실제 제약조건명이 아래와 같은지 먼저 확인하세요
-- (SHOW CREATE TABLE crew_weekly_contributions; 로 FK 이름을 재확인).
--
-- 증상: 크루 해체(DELETE /api/crews/me) 및 크루장 계정의 회원탈퇴(DELETE /api/users/me)가
-- 아래 에러로 실패함.
--   Cannot delete or update a parent row: a foreign key constraint fails
--   (`crew_weekly_contributions`, CONSTRAINT `FKkw1lnfds2aw24g1rxe30weybr`
--   FOREIGN KEY (`weekly_mission_id`) REFERENCES `crew_weekly_missions` (`id`))
--   [delete from crew_weekly_missions where id=?]
--
-- 원인: 크루 해체 시 crew_weekly_missions 행을 지우는데, 그 미션에 달린
-- crew_weekly_contributions(크루원별 주간미션 기여도) 자식 행이 먼저 삭제되지 않아 FK 위반.
-- crew_weekly_missions가 삭제될 때 딸린 기여도 기록도 함께 삭제되도록 FK에
-- ON DELETE CASCADE를 건다.
ALTER TABLE crew_weekly_contributions
  DROP FOREIGN KEY FKkw1lnfds2aw24g1rxe30weybr;

ALTER TABLE crew_weekly_contributions
  ADD CONSTRAINT FKkw1lnfds2aw24g1rxe30weybr
    FOREIGN KEY (weekly_mission_id) REFERENCES crew_weekly_missions (id)
    ON DELETE CASCADE;
