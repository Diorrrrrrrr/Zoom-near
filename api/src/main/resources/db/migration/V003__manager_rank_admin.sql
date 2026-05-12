-- 목적: 매니저 신청 워크플로우, 사용자 랭크 컬럼, 감사 로그 상태 컬럼 추가
-- 수반 변경: V001/V002 이후. users.rank_code 기본값 'PPOJJAK', audit_logs.status 기본값 'SUCCESS'.
-- 롤백: ALTER TABLE audit_logs DROP COLUMN status;
--        ALTER TABLE users DROP COLUMN rank_code;
--        DROP TABLE manager_applications;

-- ============================================================
-- manager_applications
-- ============================================================
CREATE TABLE manager_applications (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    applicant_id UUID        NOT NULL REFERENCES users(id),
    status       VARCHAR(16) NOT NULL DEFAULT 'PENDING'
                             CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    reason       TEXT        NOT NULL DEFAULT '',
    decided_by   UUID        REFERENCES users(id),
    decided_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_manager_apps_status_created ON manager_applications(status, created_at DESC);
CREATE INDEX idx_manager_apps_applicant      ON manager_applications(applicant_id, created_at DESC);

-- 한 사용자가 동시에 PENDING 신청을 여러 건 갖지 못하도록
CREATE UNIQUE INDEX uk_manager_apps_active_per_user
    ON manager_applications(applicant_id)
    WHERE status = 'PENDING';

-- ============================================================
-- users.rank_code 컬럼 추가
-- ============================================================
ALTER TABLE users
    ADD COLUMN rank_code VARCHAR(16) NOT NULL DEFAULT 'PPOJJAK'
    CHECK (rank_code IN ('PPOJJAK','GWIYOMI','KKAMJJIK','HWALHWAL'));

-- ============================================================
-- audit_logs.status 컬럼 추가 (SUCCESS / FAILED)
-- ============================================================
ALTER TABLE audit_logs
    ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'SUCCESS'
    CHECK (status IN ('SUCCESS','FAILED'));

CREATE INDEX idx_audit_logs_actor_created ON audit_logs(actor_id, created_at DESC);
CREATE INDEX idx_audit_logs_action_created ON audit_logs(action, created_at DESC);
