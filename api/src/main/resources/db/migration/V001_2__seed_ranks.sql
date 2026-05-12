-- 목적: ranks 테이블 초기 데이터 삽입 (연결 수 기반 등급 정의)
-- 수반 변경: V001__init_schema.sql 이후에 실행되어야 함
-- 롤백 메모: DELETE FROM ranks WHERE code IN ('PPOJJAK','GWIYOMI','KKAMJJIK','HWALHWAL');

INSERT INTO ranks(code, min_count, max_count, display_name) VALUES
    ('PPOJJAK',  0,   4,    '뽀짝이'),
    ('GWIYOMI',  5,   9,    '귀요미'),
    ('KKAMJJIK', 10,  29,   '깜찍이'),
    ('HWALHWAL', 30,  NULL, '활활이');
