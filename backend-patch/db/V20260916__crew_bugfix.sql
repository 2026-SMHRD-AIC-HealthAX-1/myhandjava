-- MySQL 8 기준. 운영 반영 전 백업 및 중복 컬럼 여부를 확인하세요.
ALTER TABLE crew ADD COLUMN concepts_json JSON NULL;
UPDATE crew
SET concepts_json = JSON_ARRAY(concept)
WHERE concepts_json IS NULL AND concept IS NOT NULL AND concept <> '';

ALTER TABLE crew_join_request
  ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  ADD COLUMN requested_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  ADD COLUMN processed_at DATETIME(6) NULL;

CREATE INDEX idx_crew_join_request_pending
  ON crew_join_request (crew_id, status, requested_at);

-- 같은 사용자가 같은 크루에 PENDING 요청을 여러 개 만들지 못하게 하는 생성 컬럼 방식.
ALTER TABLE crew_join_request
  ADD COLUMN pending_request_key VARCHAR(100)
    GENERATED ALWAYS AS (
      CASE WHEN status = 'PENDING' THEN CONCAT(crew_id, ':', requester_id) ELSE NULL END
    ) STORED,
  ADD UNIQUE INDEX uk_crew_join_request_pending (pending_request_key);
