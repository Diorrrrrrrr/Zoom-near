import { clearSession, getAccessToken } from "@/lib/auth/session";
import { isIdleExpired, touchActivity } from "@/lib/auth/session-activity";

const API_BASE =
  process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080";

type RequestOptions = Omit<RequestInit, "body"> & {
  body?: unknown;
};

/**
 * 백엔드 REST API 호출용 fetch 래퍼.
 *
 * - JSON 직렬화/역직렬화 자동 처리
 * - 4xx / 5xx 응답을 Error 로 throw
 * - Authorization 헤더 자동 부착 (localStorage JWT)
 * - 401 응답 시 세션 삭제 후 /login 으로 이동
 */
export async function apiClient<T>(
  path: string,
  options: RequestOptions = {}
): Promise<T> {
  const { body, headers, ...rest } = options;

  // /api/v1/auth/* (login, signup, refresh) 호출은 세션 유무와 무관 — idle/401 자동 리다이렉트 스킵
  const isAuthEndpoint = path.startsWith("/api/v1/auth/");

  // 30분 미활동이면 호출 전에 강제 로그아웃 (auth 엔드포인트는 예외)
  if (!isAuthEndpoint && isIdleExpired()) {
    clearSession();
    if (typeof window !== "undefined") {
      window.location.href = "/login";
    }
    throw new Error("세션이 만료되었습니다. 다시 로그인해 주세요.");
  }

  const token = getAccessToken();

  const response = await fetch(`${API_BASE}${path}`, {
    ...rest,
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...headers,
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (response.status === 401 && !isAuthEndpoint) {
    clearSession();
    if (typeof window !== "undefined") {
      window.location.href = "/login";
    }
    throw new Error("인증이 만료되었습니다. 다시 로그인해 주세요.");
  }

  if (!response.ok) {
    const text = await response.text().catch(() => response.statusText);
    throw new Error(`[${response.status}] ${path} — ${text}`);
  }

  // 정상 응답 수신 — 활동 시각 갱신
  touchActivity();

  // 204 No Content
  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

/** GET shorthand */
export const get = <T>(path: string, init?: RequestOptions) =>
  apiClient<T>(path, { method: "GET", ...init });

/** POST shorthand */
export const post = <T>(path: string, body: unknown, init?: RequestOptions) =>
  apiClient<T>(path, { method: "POST", body, ...init });

/** PUT shorthand */
export const put = <T>(path: string, body: unknown, init?: RequestOptions) =>
  apiClient<T>(path, { method: "PUT", body, ...init });

/** PATCH shorthand */
export const patch = <T>(path: string, body: unknown, init?: RequestOptions) =>
  apiClient<T>(path, { method: "PATCH", body, ...init });

/** DELETE shorthand */
export const del = <T>(path: string, init?: RequestOptions) =>
  apiClient<T>(path, { method: "DELETE", ...init });
