/**
 * 사용자(TUNTUN/DUNDUN/MANAGER) 세션 저장/조회/삭제 헬퍼
 *
 * - localStorage: accessToken, refreshToken, userId, role 저장
 * - 쿠키: user_session 플래그 (middleware redirect 용)
 */

import { clearActivity, touchActivity } from "./session-activity";

const ACCESS_TOKEN_KEY = "user_access_token";
const REFRESH_TOKEN_KEY = "user_refresh_token";
const USER_ID_KEY = "user_id";
const ROLE_KEY = "user_role";
const SESSION_COOKIE = "user_session";

// 미들웨어가 서명 검증할 access token 쿠키 (어드민/일반 공용)
const AT_COOKIE = "zoomnear_at";
const ROLE_COOKIE = "zoomnear_role";
// access token TTL(30분) 과 동일. 만료되면 브라우저가 쿠키를 자동 폐기.
const AT_COOKIE_MAX_AGE_SEC = 30 * 60;

function isBrowser(): boolean {
  return typeof window !== "undefined";
}

export interface UserSessionData {
  accessToken: string;
  refreshToken: string;
  userId: string;
  role: string;
}

export function saveUserSession(data: UserSessionData): void {
  if (!isBrowser()) return;
  localStorage.setItem(ACCESS_TOKEN_KEY, data.accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken);
  localStorage.setItem(USER_ID_KEY, String(data.userId));
  localStorage.setItem(ROLE_KEY, data.role);
  document.cookie = `${SESSION_COOKIE}=1; path=/; SameSite=Lax; Max-Age=${AT_COOKIE_MAX_AGE_SEC}`;
  document.cookie = `${AT_COOKIE}=${data.accessToken}; path=/; SameSite=Lax; Max-Age=${AT_COOKIE_MAX_AGE_SEC}`;
  document.cookie = `${ROLE_COOKIE}=${data.role}; path=/; SameSite=Lax; Max-Age=${AT_COOKIE_MAX_AGE_SEC}`;
  touchActivity();
}

export function getUserAccessToken(): string | null {
  if (!isBrowser()) return null;
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function getUserRefreshToken(): string | null {
  if (!isBrowser()) return null;
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function getUserRole(): string | null {
  if (!isBrowser()) return null;
  return localStorage.getItem(ROLE_KEY);
}

export function getUserId(): string | null {
  if (!isBrowser()) return null;
  return localStorage.getItem(USER_ID_KEY);
}

export function clearUserSession(): void {
  if (!isBrowser()) return;
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem(USER_ID_KEY);
  localStorage.removeItem(ROLE_KEY);
  document.cookie = `${SESSION_COOKIE}=; path=/; Max-Age=0`;
  document.cookie = `${AT_COOKIE}=; path=/; Max-Age=0`;
  document.cookie = `${ROLE_COOKIE}=; path=/; Max-Age=0`;
  clearActivity();
}

export function isUserAuthenticated(): boolean {
  return !!getUserAccessToken();
}

export function isTuntun(): boolean {
  return getUserRole() === "TUNTUN";
}

export function isDundun(): boolean {
  return getUserRole() === "DUNDUN";
}

export function isManager(): boolean {
  return getUserRole() === "MANAGER";
}
