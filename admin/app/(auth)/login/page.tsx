"use client";

import React, { useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { login } from "@/lib/api/auth";
import { saveSession } from "@/lib/auth/session";
import { saveUserSession } from "@/lib/auth/user-session";

export default function LoginPage() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const [loginId, setLoginId] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    if (!loginId.trim() || !password.trim()) {
      setError("아이디와 비밀번호를 모두 입력해 주세요.");
      return;
    }

    setLoading(true);
    try {
      const data = await login({ loginId: loginId.trim(), password });

      if (data.role === "ADMIN") {
        // 어드민: 기존 admin_session 쿠키 + localStorage
        saveSession(data);
        const redirectTo = searchParams.get("redirect") ?? "/admin/dashboard";
        router.replace(redirectTo);
      } else if (data.role === "MANAGER") {
        // 매니저: admin_session 쿠키 + localStorage (웹 대시보드)
        saveSession(data);
        const redirectTo = searchParams.get("redirect") ?? "/manager/dashboard";
        router.replace(redirectTo);
      } else {
        // 일반 사용자(TUNTUN/DUNDUN): user_session
        saveUserSession(data);
        const redirectTo = searchParams.get("redirect") ?? "/home";
        router.replace(redirectTo);
      }
    } catch (err) {
      const message =
        err instanceof Error ? err.message : "로그인 중 오류가 발생했습니다.";
      if (
        message.includes("401") ||
        message.includes("403") ||
        message.includes("404") ||
        /UNAUTHORIZED|FORBIDDEN|NOT_FOUND/i.test(message)
      ) {
        setError("아이디/비밀번호가 일치하지 않거나 회원이 존재하지 않습니다.");
      } else if (/Failed to fetch|NetworkError/i.test(message)) {
        setError("서버에 연결할 수 없어요. 잠시 후 다시 시도해 주세요.");
      } else {
        setError(`로그인에 실패했습니다. (${message})`);
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="flex min-h-screen flex-col items-center justify-center bg-gray-50 p-6">
      <div className="w-full max-w-md rounded-2xl bg-white p-10 shadow-lg">
        <div className="mb-8 text-center">
          <h1 className="text-3xl font-bold text-gray-900">ZOOM NEAR</h1>
          <p className="mt-1 text-base text-gray-500">주니어와 함께하는 이벤트</p>
        </div>

        <form onSubmit={handleSubmit} noValidate className="space-y-5">
          <div>
            <label
              htmlFor="loginId"
              className="mb-1.5 block text-base font-medium text-gray-700"
            >
              아이디
            </label>
            <input
              id="loginId"
              type="text"
              autoComplete="username"
              value={loginId}
              onChange={(e) => setLoginId(e.target.value)}
              placeholder="아이디를 입력하세요"
              className="w-full rounded-xl border border-gray-300 px-4 py-3.5 text-lg text-gray-900 placeholder-gray-400 focus:border-forest-700 focus:outline-none focus:ring-2 focus:ring-forest-700/20"
              disabled={loading}
            />
          </div>

          <div>
            <label
              htmlFor="password"
              className="mb-1.5 block text-base font-medium text-gray-700"
            >
              비밀번호
            </label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="비밀번호를 입력하세요"
              className="w-full rounded-xl border border-gray-300 px-4 py-3.5 text-lg text-gray-900 placeholder-gray-400 focus:border-forest-700 focus:outline-none focus:ring-2 focus:ring-forest-700/20"
              disabled={loading}
            />
          </div>

          {error && (
            <div
              role="alert"
              className="flex items-start gap-2 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-base font-medium text-red-700"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth={2}
                className="h-5 w-5 shrink-0 mt-0.5"
                aria-hidden="true"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z"
                />
              </svg>
              <span>{error}</span>
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className="mt-2 h-14 w-full rounded-xl bg-forest-700 text-lg font-semibold text-white hover:bg-forest-800 focus:outline-none focus:ring-2 focus:ring-forest-700/50 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {loading ? "로그인 중…" : "로그인"}
          </button>
        </form>

        <div className="mt-6 text-center">
          <span className="text-base text-gray-500">아직 계정이 없으신가요? </span>
          <Link
            href="/auth/signup"
            className="text-base font-semibold text-forest-700 hover:underline"
          >
            회원가입
          </Link>
        </div>
      </div>
    </main>
  );
}
