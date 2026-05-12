"use client";

import React, { Suspense, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { post } from "@/lib/api/client";
import { saveUserSession } from "@/lib/auth/user-session";
import type { TokenResponse } from "@/lib/types";

type Role = "TUNTUN" | "DUNDUN";

const ROLE_INFO: Record<Role, { label: string; desc: string }> = {
  TUNTUN: {
    label: "튼튼이",
    desc: "이벤트에 직접 참여하는 회원입니다. 포인트를 충전하고 이벤트를 신청할 수 있어요.",
  },
  DUNDUN: {
    label: "든든이",
    desc: "튼튼이를 도와주는 보호자 회원입니다. 연동 후 대리 충전·참여가 가능해요.",
  },
};

/// useSearchParams 가 정적 prerender 를 막기 때문에 Suspense 경계 안에서만 사용한다.
function SignupForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const inviteToken = searchParams.get("inviteToken") ?? "";

  const [step, setStep] = useState<1 | 2>(1);
  const [role, setRole] = useState<Role>("TUNTUN");
  const [form, setForm] = useState({
    loginId: "",
    password: "",
    confirmPassword: "",
    name: "",
    phone: "",
    email: "",
  });
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }));
  }

  function validateStep1(): string | null {
    if (!role) return "역할을 선택해 주세요.";
    return null;
  }

  function validateStep2(): string | null {
    if (!form.loginId.trim()) return "아이디를 입력해 주세요.";
    if (!form.password) return "비밀번호를 입력해 주세요.";
    if (form.password.length < 6) return "비밀번호는 6자 이상이어야 합니다.";
    if (form.password !== form.confirmPassword) return "비밀번호가 일치하지 않습니다.";
    if (!form.name.trim()) return "이름을 입력해 주세요.";
    if (!form.phone.trim()) return "휴대폰 번호를 입력해 주세요.";
    return null;
  }

  function handleNext() {
    const err = validateStep1();
    if (err) { setError(err); return; }
    setError(null);
    setStep(2);
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const err = validateStep2();
    if (err) { setError(err); return; }
    setError(null);
    setLoading(true);

    try {
      const data = await post<TokenResponse>("/api/v1/auth/signup", {
        loginId: form.loginId.trim(),
        password: form.password,
        phone: form.phone.trim(),
        email: form.email.trim() || undefined,
        name: form.name.trim(),
        role,
        inviteToken: inviteToken || undefined,
      });
      saveUserSession(data);
      router.replace("/home");
    } catch (err) {
      setError(err instanceof Error ? err.message : "가입 중 오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="flex min-h-screen flex-col items-center justify-center bg-gray-50 p-6">
      <div className="w-full max-w-md rounded-2xl bg-white p-8 shadow-lg">
        {/* 헤더 */}
        <div className="mb-6 text-center">
          <h1 className="text-2xl font-bold text-gray-900">ZOOM NEAR 가입</h1>
          <p className="mt-1 text-base text-gray-500">
            {step === 1 ? "어떤 역할로 가입하시나요?" : "기본 정보를 입력해 주세요"}
          </p>
          {/* 스텝 인디케이터 */}
          <div className="mt-4 flex items-center justify-center gap-2">
            <div className={`h-2 w-8 rounded-full ${step >= 1 ? "bg-orange-700" : "bg-gray-200"}`} />
            <div className={`h-2 w-8 rounded-full ${step >= 2 ? "bg-orange-700" : "bg-gray-200"}`} />
          </div>
        </div>

        {step === 1 ? (
          <div className="space-y-4">
            {(Object.entries(ROLE_INFO) as [Role, { label: string; desc: string }][]).map(([r, info]) => (
              <button
                key={r}
                type="button"
                onClick={() => setRole(r)}
                className={`w-full rounded-2xl border p-5 text-left transition-colors ${
                  role === r
                    ? "border-orange-700 bg-orange-50"
                    : "border-gray-200 bg-white hover:border-gray-300"
                }`}
              >
                <p className="text-lg font-bold text-gray-900">{info.label}</p>
                <p className="mt-1 text-base text-gray-600">{info.desc}</p>
              </button>
            ))}

            {error && (
              <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-base text-red-700" role="alert">
                {error}
              </div>
            )}

            <button
              type="button"
              onClick={handleNext}
              className="mt-2 h-14 w-full rounded-xl bg-orange-700 text-lg font-semibold text-white hover:bg-orange-800 focus:outline-none focus:ring-2 focus:ring-orange-700/50"
            >
              다음
            </button>
          </div>
        ) : (
          <form onSubmit={handleSubmit} noValidate className="space-y-4">
            <div>
              <label className="mb-1.5 block text-base font-medium text-gray-700">아이디 *</label>
              <input
                name="loginId"
                type="text"
                autoComplete="username"
                value={form.loginId}
                onChange={handleChange}
                placeholder="로그인 아이디"
                className="w-full rounded-xl border border-gray-300 px-4 py-3.5 text-lg text-gray-900 placeholder-gray-400 focus:border-orange-700 focus:outline-none focus:ring-2 focus:ring-orange-700/20"
              />
            </div>

            <div>
              <label className="mb-1.5 block text-base font-medium text-gray-700">비밀번호 *</label>
              <input
                name="password"
                type="password"
                autoComplete="new-password"
                value={form.password}
                onChange={handleChange}
                placeholder="6자 이상"
                className="w-full rounded-xl border border-gray-300 px-4 py-3.5 text-lg text-gray-900 placeholder-gray-400 focus:border-orange-700 focus:outline-none focus:ring-2 focus:ring-orange-700/20"
              />
            </div>

            <div>
              <label className="mb-1.5 block text-base font-medium text-gray-700">비밀번호 확인 *</label>
              <input
                name="confirmPassword"
                type="password"
                autoComplete="new-password"
                value={form.confirmPassword}
                onChange={handleChange}
                placeholder="비밀번호 재입력"
                className="w-full rounded-xl border border-gray-300 px-4 py-3.5 text-lg text-gray-900 placeholder-gray-400 focus:border-orange-700 focus:outline-none focus:ring-2 focus:ring-orange-700/20"
              />
            </div>

            <div>
              <label className="mb-1.5 block text-base font-medium text-gray-700">이름 *</label>
              <input
                name="name"
                type="text"
                autoComplete="name"
                value={form.name}
                onChange={handleChange}
                placeholder="실명을 입력하세요"
                className="w-full rounded-xl border border-gray-300 px-4 py-3.5 text-lg text-gray-900 placeholder-gray-400 focus:border-orange-700 focus:outline-none focus:ring-2 focus:ring-orange-700/20"
              />
            </div>

            <div>
              <label className="mb-1.5 block text-base font-medium text-gray-700">휴대폰 번호 *</label>
              <input
                name="phone"
                type="tel"
                autoComplete="tel"
                value={form.phone}
                onChange={handleChange}
                placeholder="010-0000-0000"
                className="w-full rounded-xl border border-gray-300 px-4 py-3.5 text-lg text-gray-900 placeholder-gray-400 focus:border-orange-700 focus:outline-none focus:ring-2 focus:ring-orange-700/20"
              />
            </div>

            <div>
              <label className="mb-1.5 block text-base font-medium text-gray-700">이메일 (선택)</label>
              <input
                name="email"
                type="email"
                autoComplete="email"
                value={form.email}
                onChange={handleChange}
                placeholder="example@email.com"
                className="w-full rounded-xl border border-gray-300 px-4 py-3.5 text-lg text-gray-900 placeholder-gray-400 focus:border-orange-700 focus:outline-none focus:ring-2 focus:ring-orange-700/20"
              />
            </div>

            {error && (
              <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-base text-red-700" role="alert">
                {error}
              </div>
            )}

            <div className="flex gap-3 pt-1">
              <button
                type="button"
                onClick={() => setStep(1)}
                className="h-14 flex-1 rounded-xl border border-gray-300 bg-white text-lg font-semibold text-gray-700 hover:bg-gray-50"
              >
                이전
              </button>
              <button
                type="submit"
                disabled={loading}
                className="h-14 flex-1 rounded-xl bg-orange-700 text-lg font-semibold text-white hover:bg-orange-800 disabled:opacity-60"
              >
                {loading ? "가입 중…" : "가입하기"}
              </button>
            </div>
          </form>
        )}

        <p className="mt-5 text-center text-base text-gray-500">
          이미 계정이 있으신가요?{" "}
          <Link href="/login" className="font-semibold text-orange-700 hover:underline">
            로그인
          </Link>
        </p>
      </div>
    </main>
  );
}

export default function SignupPage() {
  return (
    <Suspense
      fallback={
        <main className="flex min-h-screen flex-col items-center justify-center bg-gray-50 p-6">
          <p className="text-base text-gray-400">불러오는 중…</p>
        </main>
      }
    >
      <SignupForm />
    </Suspense>
  );
}
