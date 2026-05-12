-- 목적: 연동 생성을 승인 워크플로우(approvals)로 일원화하기 위해 type 에 LINKAGE_CREATE 추가
-- 수반 변경: ApprovalType enum, ApprovalFacade 분기, LinkageFacade 리팩토링 (코드 변경은 별개)
-- 롤백:
--   ALTER TABLE approvals DROP CONSTRAINT approvals_type_check;
--   ALTER TABLE approvals ADD CONSTRAINT approvals_type_check
--       CHECK (type IN ('EVENT_JOIN','EVENT_CANCEL','EVENT_CREATE'));

ALTER TABLE approvals DROP CONSTRAINT IF EXISTS approvals_type_check;
ALTER TABLE approvals
    ADD CONSTRAINT approvals_type_check
    CHECK (type IN ('EVENT_JOIN','EVENT_CANCEL','EVENT_CREATE','LINKAGE_CREATE'));
