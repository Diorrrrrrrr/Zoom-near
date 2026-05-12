import { NextRequest, NextResponse } from "next/server";
import { jwtVerify } from "jose";

/**
 * 미들웨어: 경로별 인증 및 권한 검사
 *
 * 정책:
 *  - 공개 경로(/login, /auth/*, /invite/*, /_next, /favicon) 통과
 *  - 그 외 모든 경로는 JWT 쿠키(`zoomnear_at`) 서명 + 만료 검증
 *  - /admin/* 는 role=ADMIN 필수
 *  - 검증 실패 시 쿠키 삭제 + /login?reason=expired (또는 unauthenticated) 리다이렉트
 *
 * 토큰 보관 위치:
 *  - 클라이언트 saveSession/saveUserSession 이 localStorage 와 동시에
 *    `zoomnear_at` (access token) + `zoomnear_role` 쿠키를 함께 세팅
 *  - 미들웨어는 cookie 만 본다 (localStorage 접근 불가)
 */

const PUBLIC_PATH_PREFIXES = ["/auth/", "/invite/", "/_next/", "/favicon"];
const PUBLIC_EXACT = new Set(["/login"]);

const JWT_SECRET_KEY = new TextEncoder().encode(
  process.env.ZOOMNEAR_JWT_SECRET ?? "",
);

function isPublic(pathname: string): boolean {
  if (PUBLIC_EXACT.has(pathname)) return true;
  return PUBLIC_PATH_PREFIXES.some((p) => pathname.startsWith(p));
}

function redirectToLogin(
  request: NextRequest,
  reason: "unauthenticated" | "expired" | "forbidden",
  pathname: string,
): NextResponse {
  const loginUrl = new URL("/login", request.url);
  loginUrl.searchParams.set("redirect", pathname);
  loginUrl.searchParams.set("reason", reason === "forbidden" ? "expired" : reason);
  const res = NextResponse.redirect(loginUrl);
  // 만료/위변조 쿠키는 깨끗이 정리
  res.cookies.delete("zoomnear_at");
  res.cookies.delete("zoomnear_role");
  res.cookies.delete("user_session");
  res.cookies.delete("admin_session");
  return res;
}

export async function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // 공개 경로 통과
  if (isPublic(pathname)) {
    return NextResponse.next();
  }

  const token = request.cookies.get("zoomnear_at")?.value;
  if (!token) {
    return redirectToLogin(request, "unauthenticated", pathname);
  }

  if (JWT_SECRET_KEY.length === 0) {
    // 시크릿 미설정 — 안전을 위해 차단 (배포 잘못 막기)
    return redirectToLogin(request, "expired", pathname);
  }

  let role: string | undefined;
  try {
    const { payload } = await jwtVerify(token, JWT_SECRET_KEY, {
      algorithms: ["HS256"],
    });
    role = typeof payload.role === "string" ? payload.role : undefined;
  } catch {
    return redirectToLogin(request, "expired", pathname);
  }

  // /admin/* 는 ADMIN 만 진입
  if (pathname.startsWith("/admin")) {
    if (role !== "ADMIN") {
      return redirectToLogin(request, "forbidden", pathname);
    }
    return NextResponse.next();
  }

  // 그 외 보호 경로 — 토큰 유효성만 OK 면 통과
  return NextResponse.next();
}

export const config = {
  matcher: [
    // 정적 자원과 공개 경로 제외 모두 매칭
    "/((?!_next/static|_next/image|favicon.ico|login|auth|invite).*)",
  ],
};
