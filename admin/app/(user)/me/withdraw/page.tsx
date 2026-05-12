"use client";

import React, { useState } from "react";
import { useRouter } from "next/navigation";
import { TopBar } from "@/components/user-ui/top-bar";
import { Button } from "@/components/user-ui/button";
import { clearUserSession } from "@/lib/auth/user-session";
import { uPost } from "@/lib/api/user-client";

export default function WithdrawPage() {
  const router = useRouter();
  const [step, setStep] = useState<1 | 2>(1);
  const [confirm, setConfirm] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleWithdraw() {
    if (confirm !== "탈퇴") {
      setError('"탈퇴"를 입력해 주세요.');
      return;
    }
    setLoading(true);
    try {
      await uPost("/api/v1/me/withdraw", {});
      clearUserSession();
      router.replace("/login");
    } catch (err) {
      setError(err instanceof Error ? err.message : "탈퇴 처리 중 오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <>
      <TopBar title="회원 탈퇴" showBack />

      <div className="px-4 py-8 space-y-6">
        {step === 1 ? (
          <>
            <div className="rounded-2xl border border-gray-200 bg-gray-50 p-5 space-y-2">
              <p className="text-lg font-bold text-gray-900">탈퇴 전 꼭 확인해 주세요</p>
              <ul className="list-disc pl-5 space-y-1 text-base text-gray-800">
                <li>모든 이벤트 참여 이력이 삭제됩니다.</li>
                <li>보유 포인트는 환불되지 않습니다.</li>
                <li>연동된 계정 정보도 삭제됩니다.</li>
                <li>탈퇴 후 같은 아이디로 재가입할 수 없습니다.</li>
              </ul>
            </div>

            <div className="flex gap-3">
              <Button variant="secondary" fullWidth onClick={() => router.back()}>
                취소
              </Button>
              <Button variant="danger" fullWidth onClick={() => setStep(2)}>
                탈퇴 진행
              </Button>
            </div>
          </>
        ) : (
          <>
            <p className="text-base text-gray-600">
              탈퇴를 확인하려면 아래 입력창에{" "}
              <span className="font-bold text-gray-800">탈퇴</span>를 입력해 주세요.
            </p>

            <input
              type="text"
              value={confirm}
              onChange={(e) => setConfirm(e.target.value)}
              placeholder='여기에 "탈퇴" 입력'
              className="w-full rounded-xl border border-gray-300 px-4 py-3.5 text-lg text-gray-900 placeholder-gray-400 focus:border-gray-900 focus:outline-none focus:ring-2 focus:ring-gray-900/15"
            />

            {error && (
              <div className="rounded-xl border border-gray-200 bg-gray-50 p-4 text-base text-gray-900" role="alert">
                {error}
              </div>
            )}

            <div className="flex gap-3">
              <Button variant="secondary" fullWidth onClick={() => setStep(1)}>
                이전
              </Button>
              <Button
                variant="danger"
                fullWidth
                loading={loading}
                onClick={handleWithdraw}
              >
                최종 탈퇴
              </Button>
            </div>
          </>
        )}
      </div>
    </>
  );
}
