-- 목적: 기존에 TUNTUN 사용자가 만들었지만 "주니어 자체 프로그램" 성격으로 운영하기로 한
--       수영모임/탁구모임 이벤트를 매니저 이벤트로 재분류.
-- 수반 변경: 프론트엔드 이벤트 카드/목록에서 녹색 강조 표시 노출.
-- 롤백 메모: UPDATE social_events SET manager_program = false WHERE title IN ('수영모임', '탁구모임');

UPDATE social_events
SET is_manager_program = true,
    updated_at = now()
WHERE title IN ('수영모임', '탁구모임');
