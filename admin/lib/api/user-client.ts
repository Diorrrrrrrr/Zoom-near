import { clearUserSession, getUserAccessToken } from "@/lib/auth/user-session";
import { isIdleExpired, touchActivity } from "@/lib/auth/session-activity";

const API_BASE =
  process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080";

type RequestOptions = Omit<RequestInit, "body"> & {
  body?: unknown;
};

/**
 * 사용자용 API 클라이언트.
 * user localStorage 토큰을 사용하며, 401 시 사용자 세션 삭제 후 /login 이동.
 */
export async function userApiClient<T>(
  path: string,
  options: RequestOptions = {}
): Promise<T> {
  const { body, headers, ...rest } = options;

  const isAuthEndpoint = path.startsWith("/api/v1/auth/");

  // 30분 미활동이면 호출 전에 강제 로그아웃 (auth 엔드포인트는 예외)
  if (!isAuthEndpoint && isIdleExpired()) {
    clearUserSession();
    if (typeof window !== "undefined") {
      window.location.href = "/login";
    }
    throw new Error("세션이 만료되었습니다. 다시 로그인해 주세요.");
  }

  const token = getUserAccessToken();

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
    clearUserSession();
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

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

export const uGet = <T>(path: string, init?: RequestOptions) =>
  userApiClient<T>(path, { method: "GET", ...init });

export const uPost = <T>(path: string, body: unknown, init?: RequestOptions) =>
  userApiClient<T>(path, { method: "POST", body, ...init });

export const uPut = <T>(path: string, body: unknown, init?: RequestOptions) =>
  userApiClient<T>(path, { method: "PUT", body, ...init });

export const uDel = <T>(path: string, init?: RequestOptions) =>
  userApiClient<T>(path, { method: "DELETE", ...init });

export const uPatch = <T>(path: string, body: unknown, init?: RequestOptions) =>
  userApiClient<T>(path, { method: "PATCH", body, ...init });

export const uDelWithBody = <T>(path: string, body: unknown, init?: RequestOptions) =>
  userApiClient<T>(path, { method: "DELETE", body, ...init });
