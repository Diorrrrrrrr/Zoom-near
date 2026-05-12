"use client";

import React, { useEffect } from "react";
import { useParams, useRouter } from "next/navigation";

/**
 * 초대 토큰 진입점 (공개 경로).
 * 토큰을 가지고 회원가입 페이지로 자동 이동합니다.
 */
export default function InviteTokenPage() {
  const { token } = useParams<{ token: string }>();
  const router = useRouter();

  useEffect(() => {
    if (token) {
      router.replace(`/auth/signup?inviteToken=${encodeURIComponent(token)}`);
    }
  }, [token, router]);

  return (
    <main className="flex min-h-screen items-center justify-center bg-gray-50">
      <div className="text-center space-y-2">
        <p className="text-xl font-semibold text-gray-700">초대 링크를 처리 중이에요</p>
        <p className="text-base text-gray-400">잠시만 기다려 주세요…</p>
      </div>
    </main>
  );
}
