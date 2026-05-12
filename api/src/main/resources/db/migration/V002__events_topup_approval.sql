-- 목적: social_events, event_participations, mock_topups, approvals 테이블 추가
-- 수반 변경: linkages 테이블 사용은 V001에 이미 정의됨, FK 참조만 추가
-- 롤백: DROP TABLE approvals, mock_topups, event_participations, social_events;
--        DROP FUNCTION claim_unique_code, debit_points, credit_points,
--                      trg_social_events_updated_at, check_approval_constraints;

-- ============================================================
-- social_events
-- ============================================================
CREATE TABLE social_events (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_id          UUID         NOT NULL REFERENCES users(id),
    region_text         VARCHAR(100),
    category            VARCHAR(24)  NOT NULL
                                     CHECK (category IN ('SPORTS','STUDY','LEISURE','HEALTH','MANAGER_PROGRAM','ETC')),
    title               VARCHAR(120) NOT NULL,
    description         TEXT         NOT NULL DEFAULT '',
    starts_at           TIMESTAMPTZ  NOT NULL,
    ends_at             TIMESTAMPTZ  NOT NULL,
    capacity            INT          NOT NULL CHECK (capacity > 0),
    point_cost          NUMERIC(12,0) NOT NULL DEFAULT 0 CHECK (point_cost >= 0),
    status              VARCHAR(16)  NOT NULL DEFAULT 'OPEN'
                                     CHECK (status IN ('OPEN','CLOSED','CANCELED','DONE')),
    visibility          VARCHAR(16)  NOT NULL DEFAULT 'REGION_ONLY'
                                     CHECK (visibility IN ('REGION_ONLY','ADJACENT')),
    is_manager_program  BOOLEAN      NOT NULL DEFAULT false,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CHECK (ends_at > starts_at)
);

CREATE INDEX idx_social_events_status_starts ON social_events(status, starts_at);
CREATE INDEX idx_social_events_region        ON social_events(region_text);

-- updated_at 자동 갱신 트리거
CREATE OR REPLACE FUNCTION trg_social_events_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_social_events_updated_at
    BEFORE UPDATE ON social_events
    FOR EACH ROW EXECUTE FUNCTION trg_social_events_updated_at();

-- ============================================================
-- event_participations
-- ============================================================
CREATE TABLE event_participations (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id        UUID         NOT NULL REFERENCES social_events(id),
    participant_id  UUID         NOT NULL REFERENCES users(id),
    proxied_by      UUID         REFERENCES users(id),
    status          VARCHAR(24)  NOT NULL
                                 CHECK (status IN ('PENDING_APPROVAL','CONFIRMED','CANCELED','COMPLETED','NO_SHOW')),
    approval_id     UUID,
    joined_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    canceled_at     TIMESTAMPTZ
);

-- 동일 이벤트에 대해 활성(PENDING/CONFIRMED) 참여는 1건만
CREATE UNIQUE INDEX uk_event_part_active
    ON event_participations(event_id, participant_id)
    WHERE status IN ('PENDING_APPROVAL','CONFIRMED');

CREATE INDEX idx_event_part_participant ON event_participations(participant_id, joined_at DESC);

-- ============================================================
-- mock_topups
-- ============================================================
CREATE TABLE mock_topups (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      UUID         NOT NULL REFERENCES users(id),
    charged_by   UUID         NOT NULL REFERENCES users(id),
    amount       NUMERIC(12,0) NOT NULL CHECK (amount > 0),
    reason_text  VARCHAR(200) NOT NULL DEFAULT '본인 충전',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_mock_topups_user_created    ON mock_topups(user_id, created_at DESC);
CREATE INDEX idx_mock_topups_charged_created ON mock_topups(charged_by, created_at DESC);

-- ============================================================
-- approvals
-- ============================================================
CREATE TABLE approvals (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    type          VARCHAR(24) NOT NULL
                              CHECK (type IN ('EVENT_JOIN','EVENT_CANCEL','EVENT_CREATE')),
    requester_id  UUID        NOT NULL REFERENCES users(id),
    approver_id   UUID        NOT NULL REFERENCES users(id),
    payload       JSONB       NOT NULL DEFAULT '{}'::jsonb,
    status        VARCHAR(16) NOT NULL DEFAULT 'PENDING'
                              CHECK (status IN ('PENDING','APPROVED','REJECTED','EXPIRED')),
    expires_at    TIMESTAMPTZ NOT NULL,
    decided_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_approvals_approver_status   ON approvals(approver_id, status, expires_at);
CREATE INDEX idx_approvals_requester_status  ON approvals(requester_id, status, created_at DESC);

-- approvals 검증 트리거: requester=DUNDUN, approver=TUNTUN, linked 검증
CREATE OR REPLACE FUNCTION check_approval_constraints()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    v_req_role  VARCHAR(16);
    v_app_role  VARCHAR(16);
    v_linked    BOOLEAN;
BEGIN
    SELECT role INTO v_req_role FROM users WHERE id = NEW.requester_id;
    IF v_req_role <> 'DUNDUN' THEN
        RAISE EXCEPTION 'invalid_approval_requester_role: user % has role %', NEW.requester_id, v_req_role;
    END IF;

    SELECT role INTO v_app_role FROM users WHERE id = NEW.approver_id;
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

CREATE TRIGGER trg_check_approval_constraints
    BEFORE INSERT OR UPDATE ON approvals
    FOR EACH ROW EXECUTE FUNCTION check_approval_constraints();

-- event_participations.approval_id FK는 approvals 정의 후 추가
ALTER TABLE event_participations
    ADD CONSTRAINT fk_part_approval
    FOREIGN KEY (approval_id) REFERENCES approvals(id);

-- ============================================================
-- RPC: claim_unique_code
-- ============================================================
-- 미할당 unique_code 한 건을 잠금/할당하고 반환한다.
-- pg_advisory_xact_lock으로 동시 채번 충돌을 차단한다.
CREATE OR REPLACE FUNCTION claim_unique_code(_user_id UUID) RETURNS CHAR(6)
LANGUAGE plpgsql AS $$
DECLARE
    v_code CHAR(6);
BEGIN
    PERFORM pg_advisory_xact_lock(12345);

    UPDATE unique_codes
       SET assigned_to = _user_id,
           assigned_at = now()
     WHERE code = (
            SELECT code FROM unique_codes
             WHERE assigned_to IS NULL
             ORDER BY random()
             LIMIT 1
           )
     RETURNING code INTO v_code;

    IF v_code IS NULL THEN
        RAISE EXCEPTION 'unique_code_pool_exhausted';
    END IF;

    RETURN v_code;
END;
$$;

-- ============================================================
-- RPC: debit_points
-- ============================================================
-- 사용자 포인트 잔액에서 _amount만큼 차감한 뒤 새 잔액을 반환한다.
-- 잔액 부족 시 RAISE EXCEPTION 'insufficient_points'.
-- point_ledger에 INSERT까지 한 트랜잭션에서 수행 (UNIQUE로 idempotent 보호).
CREATE OR REPLACE FUNCTION debit_points(
    _user_id   UUID,
    _amount    NUMERIC,
    _reason    VARCHAR,
    _ref_type  VARCHAR,
    _ref_id    VARCHAR
) RETURNS NUMERIC
LANGUAGE plpgsql AS $$
DECLARE
    v_new_balance NUMERIC(12,0);
BEGIN
    IF _amount <= 0 THEN
        RAISE EXCEPTION 'invalid_amount: % must be positive', _amount;
    END IF;

    UPDATE point_wallets
       SET balance = balance - _amount
     WHERE user_id = _user_id
       AND balance >= _amount
     RETURNING balance INTO v_new_balance;

    IF v_new_balance IS NULL THEN
        RAISE EXCEPTION 'insufficient_points';
    END IF;

    INSERT INTO point_ledger(user_id, delta, reason, reference_type, reference_id, balance_after)
    VALUES (_user_id, -_amount, _reason, _ref_type, _ref_id, v_new_balance);

    RETURN v_new_balance;
END;
$$;

-- ============================================================
-- RPC: credit_points
-- ============================================================
-- 사용자 포인트 지갑에 _amount만큼 적립한 뒤 새 잔액을 반환한다.
-- point_ledger UNIQUE(reason, reference_type, reference_id, user_id)로 idempotent.
CREATE OR REPLACE FUNCTION credit_points(
    _user_id   UUID,
    _amount    NUMERIC,
    _reason    VARCHAR,
    _ref_type  VARCHAR,
    _ref_id    VARCHAR
) RETURNS NUMERIC
LANGUAGE plpgsql AS $$
DECLARE
    v_new_balance NUMERIC(12,0);
BEGIN
    IF _amount <= 0 THEN
        RAISE EXCEPTION 'invalid_amount: % must be positive', _amount;
    END IF;

    INSERT INTO point_wallets(user_id, balance, updated_at)
    VALUES (_user_id, _amount, now())
    ON CONFLICT (user_id) DO UPDATE
       SET balance = point_wallets.balance + EXCLUDED.balance
    RETURNING balance INTO v_new_balance;

    INSERT INTO point_ledger(user_id, delta, reason, reference_type, reference_id, balance_after)
    VALUES (_user_id, _amount, _reason, _ref_type, _ref_id, v_new_balance);

    RETURN v_new_balance;
END;
$$;
