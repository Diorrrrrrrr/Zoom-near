-- 목적: ZOOM NEAR 서비스 초기 스키마 생성 (users, point, linkages, notifications, audit 등)
-- 수반 변경: 없음 (최초 마이그레이션)
-- 롤백 메모: DROP TABLE 역순(device_tokens, ranks, audit_logs, notifications, invite_tokens, linkages, unique_codes, point_ledger, point_wallets, users); DROP EXTENSION pgcrypto;

-- ============================================================
-- 확장
-- ============================================================
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================
-- users
-- ============================================================
CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    login_id      VARCHAR(20)  UNIQUE NOT NULL
                               CHECK (login_id ~ '^[a-zA-Z0-9_]{4,20}$'),
    password_hash VARCHAR(72)  NOT NULL,
    phone         VARCHAR(20)  NOT NULL,
    email         VARCHAR(254),
    name          VARCHAR(50)  NOT NULL,
    role          VARCHAR(16)  NOT NULL
                               CHECK (role IN ('TUNTUN','DUNDUN','MANAGER','ADMIN')),
    unique_code   CHAR(6)      UNIQUE NOT NULL,
    birth_date    DATE,
    status        VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE'
                               CHECK (status IN ('ACTIVE','SUSPENDED','DELETED')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at    TIMESTAMPTZ
);

-- ============================================================
-- point_wallets
-- ============================================================
CREATE TABLE point_wallets (
    user_id    UUID         PRIMARY KEY REFERENCES users(id),
    balance    NUMERIC(12,0) NOT NULL DEFAULT 0
                             CHECK (balance >= 0),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ============================================================
-- point_ledger
-- ============================================================
CREATE TABLE point_ledger (
    id             BIGSERIAL    PRIMARY KEY,
    user_id        UUID         NOT NULL REFERENCES users(id),
    delta          NUMERIC(12,0) NOT NULL,
    reason         VARCHAR(24)  NOT NULL
                                CHECK (reason IN ('MOCK_TOPUP','EVENT_JOIN','EVENT_REFUND','ADMIN_ADJUST','EXPIRE')),
    reference_type VARCHAR(32)  NOT NULL,
    reference_id   VARCHAR(64)  NOT NULL,
    balance_after  NUMERIC(12,0) NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),

    UNIQUE (reason, reference_type, reference_id, user_id)
);

CREATE INDEX idx_point_ledger_user_created ON point_ledger(user_id, created_at DESC);

-- ============================================================
-- unique_codes
-- ============================================================
CREATE TABLE unique_codes (
    code        CHAR(6)     PRIMARY KEY,
    assigned_to UUID        REFERENCES users(id),
    assigned_at TIMESTAMPTZ
);

-- 미할당 코드 빠른 조회를 위한 부분 인덱스
CREATE INDEX idx_unique_codes_unassigned ON unique_codes(assigned_to) WHERE assigned_to IS NULL;

-- ============================================================
-- linkages
-- ============================================================
CREATE TABLE linkages (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    dundun_id   UUID        NOT NULL REFERENCES users(id),
    tunchun_id  UUID        NOT NULL REFERENCES users(id),
    is_primary  BOOLEAN     NOT NULL DEFAULT false,
    status      VARCHAR(16) NOT NULL DEFAULT 'ACTIVE'
                            CHECK (status IN ('ACTIVE','REVOKED')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    UNIQUE (dundun_id, tunchun_id),
    CHECK (dundun_id <> tunchun_id)
);

-- dundun 1인당 ACTIVE primary linkage 1개만 허용
CREATE UNIQUE INDEX idx_dundun_primary
    ON linkages(dundun_id)
    WHERE is_primary = true AND status = 'ACTIVE';

-- ============================================================
-- invite_tokens
-- ============================================================
CREATE TABLE invite_tokens (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    token             UUID        UNIQUE NOT NULL DEFAULT gen_random_uuid(),
    inviter_dundun_id UUID        NOT NULL REFERENCES users(id),
    expires_at        TIMESTAMPTZ NOT NULL,
    status            VARCHAR(16) NOT NULL DEFAULT 'PENDING'
                                  CHECK (status IN ('PENDING','CONSUMED','EXPIRED','REVOKED')),
    consumed_by       UUID        REFERENCES users(id),
    consumed_at       TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_invite_tokens_inviter_status ON invite_tokens(inviter_dundun_id, status);

-- ============================================================
-- notifications
-- ============================================================
CREATE TABLE notifications (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users(id),
    type       VARCHAR(32) NOT NULL,
    title      VARCHAR(200) NOT NULL,
    body       TEXT        NOT NULL,
    payload    JSONB       NOT NULL DEFAULT '{}'::jsonb,
    read_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_user_created ON notifications(user_id, created_at DESC);

-- ============================================================
-- audit_logs
-- ============================================================
CREATE TABLE audit_logs (
    id          BIGSERIAL   PRIMARY KEY,
    actor_id    UUID        REFERENCES users(id),
    action      VARCHAR(64) NOT NULL,
    target_type VARCHAR(32),
    target_id   VARCHAR(64),
    payload     JSONB       NOT NULL DEFAULT '{}'::jsonb,
    ip          VARCHAR(45),
    user_agent  TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_created ON audit_logs(created_at DESC);

-- ============================================================
-- device_tokens
-- ============================================================
CREATE TABLE device_tokens (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID        NOT NULL REFERENCES users(id),
    platform     VARCHAR(8)  NOT NULL
                             CHECK (platform IN ('FCM','APNS')),
    token        VARCHAR(500) NOT NULL,
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    UNIQUE (user_id, platform, token)
);

-- ============================================================
-- ranks
-- ============================================================
CREATE TABLE ranks (
    code         VARCHAR(16) PRIMARY KEY,
    min_count    INT         NOT NULL,
    max_count    INT,                   -- NULL = 상한 없음
    display_name VARCHAR(40) NOT NULL
);

-- ============================================================
-- 트리거 함수 1: point_wallets.updated_at 자동 갱신
-- ============================================================
CREATE OR REPLACE FUNCTION trg_point_wallets_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_point_wallets_updated_at
    BEFORE UPDATE ON point_wallets
    FOR EACH ROW EXECUTE FUNCTION trg_point_wallets_updated_at();

-- ============================================================
-- 트리거 함수 2: linkages 역할 및 최대 연결 수 검증
-- ============================================================
CREATE OR REPLACE FUNCTION check_linkage_constraints()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    v_dundun_role  VARCHAR(16);
    v_tuntun_role  VARCHAR(16);
    v_active_count INT;
BEGIN
    -- dundun_id 역할 검증
    SELECT role INTO v_dundun_role FROM users WHERE id = NEW.dundun_id;
    IF v_dundun_role <> 'DUNDUN' THEN
        RAISE EXCEPTION 'invalid_dundun_role: user % has role %', NEW.dundun_id, v_dundun_role;
    END IF;

    -- tunchun_id(tuntun) 역할 검증
    SELECT role INTO v_tuntun_role FROM users WHERE id = NEW.tunchun_id;
    IF v_tuntun_role <> 'TUNTUN' THEN
        RAISE EXCEPTION 'invalid_tuntun_role: user % has role %', NEW.tunchun_id, v_tuntun_role;
    END IF;

    -- 동시성 보호: dundun 단위 advisory lock
    PERFORM pg_advisory_xact_lock(hashtext(NEW.dundun_id::text));

    -- INSERT 또는 REVOKED→ACTIVE 전환 시에만 카운트 검증
    IF TG_OP = 'INSERT' OR (TG_OP = 'UPDATE' AND OLD.status = 'REVOKED' AND NEW.status = 'ACTIVE') THEN
        SELECT COUNT(*) INTO v_active_count
          FROM linkages
         WHERE dundun_id = NEW.dundun_id
           AND status = 'ACTIVE'
           AND id IS DISTINCT FROM NEW.id; -- UPDATE 시 자기 자신 제외

        IF v_active_count >= 4 THEN
            RAISE EXCEPTION 'dundun_active_linkage_limit_exceeded: dundun % already has % active linkages',
                NEW.dundun_id, v_active_count;
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_check_linkage_constraints
    BEFORE INSERT OR UPDATE ON linkages
    FOR EACH ROW EXECUTE FUNCTION check_linkage_constraints();
