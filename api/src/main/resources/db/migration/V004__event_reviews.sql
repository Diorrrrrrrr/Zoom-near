-- 목적: 이벤트 후기(reviews) 테이블 신설 — 추후 후기순 정렬·통계용
-- 수반 변경: 없음. 기능 구현 전이므로 INSERT/SELECT만 미리 보장.
-- 롤백:
--   DROP INDEX IF EXISTS idx_event_reviews_event_created;
--   DROP TABLE IF EXISTS event_reviews;
--   DROP FUNCTION IF EXISTS trg_event_reviews_updated_at();

-- ============================================================
-- event_reviews
-- ============================================================
-- 이벤트 종료 후 참가자(또는 매니저 관점 평가자)가 작성하는 후기.
-- (event_id, author_id) 1쌍당 최대 1건(고유 인덱스). 본문은 1~2000자.
-- rating 은 1~5 정수. status 는 PUBLISHED/HIDDEN. 작성자만 자기 후기 수정/삭제 가능.
-- 후기순 정렬은 event_id 별 카운트 또는 평균 평점 기반.

CREATE TABLE event_reviews (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id    UUID         NOT NULL REFERENCES social_events(id) ON DELETE CASCADE,
    author_id   UUID         NOT NULL REFERENCES users(id),
    rating      SMALLINT     NOT NULL CHECK (rating BETWEEN 1 AND 5),
    body        VARCHAR(2000) NOT NULL CHECK (char_length(body) >= 1),
    status      VARCHAR(16)  NOT NULL DEFAULT 'PUBLISHED'
                             CHECK (status IN ('PUBLISHED','HIDDEN')),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    -- 1 이벤트 × 1 작성자 = 최대 1건. 재제출이 아니라 UPDATE 로 다루도록 강제.
    UNIQUE (event_id, author_id)
);

-- 최근 후기 / 이벤트별 카운트 조회 가속용 복합 인덱스.
CREATE INDEX idx_event_reviews_event_created
    ON event_reviews (event_id, created_at DESC);

-- updated_at 자동 갱신 트리거.
CREATE OR REPLACE FUNCTION trg_event_reviews_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$;

CREATE TRIGGER event_reviews_updated_at
    BEFORE UPDATE ON event_reviews
    FOR EACH ROW EXECUTE FUNCTION trg_event_reviews_updated_at();
