-- 목적: approvals 검증 트리거를 type 별로 분기.
--       LINKAGE_CREATE 는 (1) 아직 linkage 미존재가 정상이고 (2) 요청자/승인자 역할이 양방향 가능.
-- 수반 변경: V002 의 check_approval_constraints 함수 본문 REPLACE.
-- 롤백:
--   기존 V002 정의로 다시 CREATE OR REPLACE FUNCTION 하면 됨.

CREATE OR REPLACE FUNCTION check_approval_constraints()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    v_req_role  VARCHAR(16);
    v_app_role  VARCHAR(16);
    v_linked    BOOLEAN;
BEGIN
    SELECT role INTO v_req_role FROM users WHERE id = NEW.requester_id;
    SELECT role INTO v_app_role FROM users WHERE id = NEW.approver_id;

    IF NEW.type = 'LINKAGE_CREATE' THEN
        -- 연동 요청: requester/approver 한 쪽은 TUNTUN, 다른 한 쪽은 DUNDUN. 순서 무관.
        IF NOT (
            (v_req_role = 'TUNTUN' AND v_app_role = 'DUNDUN')
            OR (v_req_role = 'DUNDUN' AND v_app_role = 'TUNTUN')
        ) THEN
            RAISE EXCEPTION 'invalid_linkage_approval_roles: req=% app=%',
                v_req_role, v_app_role;
        END IF;
        -- linkage 존재 여부는 검사하지 않음 (생성 자체가 목적)
        RETURN NEW;
    END IF;

    -- EVENT_JOIN / EVENT_CANCEL / EVENT_CREATE: 기존 규칙 유지
    IF v_req_role <> 'DUNDUN' THEN
        RAISE EXCEPTION 'invalid_approval_requester_role: user % has role %', NEW.requester_id, v_req_role;
    END IF;

    IF v_app_role <> 'TUNTUN' THEN
        RAISE EXCEPTION 'invalid_approval_approver_role: user % has role %', NEW.approver_id, v_app_role;
    END IF;

    SELECT EXISTS(
        SELECT 1 FROM linkages
         WHERE dundun_id  = NEW.requester_id
           AND tunchun_id = NEW.approver_id
           AND status = 'ACTIVE'
    ) INTO v_linked;

    IF NOT v_linked THEN
        RAISE EXCEPTION 'approval_not_linked: requester % and approver % are not linked',
            NEW.requester_id, NEW.approver_id;
    END IF;

    RETURN NEW;
END;
$$;
