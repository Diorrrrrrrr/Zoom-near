/**
 * 토큰 저장/조회/삭제 헬퍼 (localStorage + 세션 쿠키 플래그)
 *
 * - localStorage: accessToken, refreshToken, userId, role 저장
 * - 쿠키: admin_session 플래그 (middleware redirect 용, httpOnly 아님)
 */

import { clearActivity, touchActivity } from "./session-activity";

const ACCESS_TOKEN_KEY = "admin_access_token";
const REFRESH_TOKEN_KEY = "admin_refresh_token";
const USER_ID_KEY = "admin_user_id";
const ROLE_KEY = "admin_role";
const SESSION_COOKIE = "admin_session";

const AT_COOKIE = "zoomnear_at";
const ROLE_COOKIE = "zoomnear_role";
const AT_COOKIE_MAX_AGE_SEC = 30 * 60;

function isBrowser(): boolean {
  return typeof window !== "undefined";
}

export interface SessionData {
  accessToken: string;
  refreshToken: string;
  userId: string;
  role: string;
}

export function saveSession(data: SessionData): void {
  if (!isBrowser()) return;
  localStorage.setItem(ACCESS_TOKEN_KEY, data.accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken);
  localStorage.setItem(USER_ID_KEY, String(data.userId));
  localStorage.setItem(ROLE_KEY, data.role);
  // 미들웨어용 쿠키 — JWT 서명 검증 + role 클레임 확인을 위해 토큰 자체를 함께 저장
  document.cookie = `${SESSION_COOKIE}=1; path=/; SameSite=Lax; Max-Age=${AT_COOKIE_MAX_AGE_SEC}`;
  document.cookie = `${AT_COOKIE}=${data.accessToken}; path=/; SameSite=Lax; Max-Age=${AT_COOKIE_MAX_AGE_SEC}`;
  document.cookie = `${ROLE_COOKIE}=${data.role}; path=/; SameSite=Lax; Max-Age=${AT_COOKIE_MAX_AGE_SEC}`;
  touchActivity();
}

export function getAccessToken(): string | null {
  if (!isBrowser()) return null;
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function getRefreshToken(): string | null {
  if (!isBrowser()) return null;
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function getRole(): string | null {
  if (!isBrowser()) return null;
  return localStorage.getItem(ROLE_KEY);
}

export function getUserId(): string | null {
  if (!isBrowser()) return null;
  return localStorage.getItem(USER_ID_KEY);
}

export function clearSession(): void {
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

export function isAuthenticated(): boolean {
  return !!getAccessToken();
}

export function isAdmin(): boolean {
  return getRole() === "ADMIN";
}
