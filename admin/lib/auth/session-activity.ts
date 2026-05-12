/**
 * 클라이언트 미활동 세션 만료 공용 헬퍼.
 * - 30분(IDLE_TIMEOUT_MS) 동안 어떠한 사용자 입력·API 호출도 없으면 강제 로그아웃
 * - 백엔드 access token TTL(15분)이 끝나면 API가 401을 반환 → API 클라이언트에서 처리
 * - 본 파일은 "마지막 활동 시각"을 localStorage 에 기록·조회한다 (탭 간 공유)
 */

export const IDLE_TIMEOUT_MS = 30 * 60 * 1000; // 30분
export const ACTIVITY_KEY = "session_last_activity_at";

function isBrowser(): boolean {
  return typeof window !== "undefined";
}

/// 사용자 활동(클릭/스크롤/네비/API 응답) 발생 시 호출. 마지막 활동 시각을 now로 갱신.
export function touchActivity(): void {
  if (!isBrowser()) return;
  try {
    localStorage.setItem(ACTIVITY_KEY, String(Date.now()));
  } catch {
    /* QuotaExceeded 등은 무시 */
  }
}

/// 마지막 활동 시각을 반환. 한 번도 없으면 0.
export function getLastActivity(): number {
  if (!isBrowser()) return 0;
  const v = localStorage.getItem(ACTIVITY_KEY);
  if (!v) return 0;
  const n = Number(v);
  return Number.isFinite(n) ? n : 0;
}

/// 30분 이상 미활동이면 true.
export function isIdleExpired(): boolean {
  const last = getLastActivity();
  if (last === 0) return false; // 활동 기록이 없으면 강제 로그아웃 안 함(로그인 직후 등)
  return Date.now() - last > IDLE_TIMEOUT_MS;
}

/// 활동 시각 기록 삭제 (로그아웃 시 호출).
export function clearActivity(): void {
  if (!isBrowser()) return;
  localStorage.removeItem(ACTIVITY_KEY);
}
