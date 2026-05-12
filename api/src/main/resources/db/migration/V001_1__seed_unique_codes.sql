-- 목적: unique_codes 테이블에 100000~999999 전체(90만 건) INSERT (미할당 풀 초기화)
-- 수반 변경: V001__init_schema.sql 이후에 실행되어야 함
-- 롤백 메모: DELETE FROM unique_codes WHERE assigned_to IS NULL;  (또는 TRUNCATE unique_codes;)

-- 약 90만 row INSERT — 로컬 환경 기준 5~10초 소요
-- 욕설 블랙리스트는 어드민 기능에서 관리 (V001_1에서는 전체 범위 INSERT)
INSERT INTO unique_codes(code)
SELECT lpad(g.n::text, 6, '0')
FROM generate_series(100000, 999999) AS g(n);
