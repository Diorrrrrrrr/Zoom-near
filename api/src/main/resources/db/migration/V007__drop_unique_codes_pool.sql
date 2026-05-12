-- 목적: unique_codes 사전 채번 풀과 claim_unique_code RPC 제거.
-- 수반 변경: AuthFacade/AdminSeedRunner 가 Crockford Base32 (32^6 ≈ 1.07B) 로 in-memory 채번.
-- 롤백 메모: V001__init_schema.sql 의 unique_codes 블록 + V002 의 claim_unique_code 함수 정의 재실행.

DROP FUNCTION IF EXISTS claim_unique_code(uuid);
DROP TABLE IF EXISTS unique_codes;
