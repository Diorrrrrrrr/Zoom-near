"use client";

import React, { useState } from "react";
import { useRouter } from "next/navigation";
import { applyForManager } from "@/lib/api/manager";
import { TopBar } from "@/components/user-ui/top-bar";
import { Button } from "@/components/user-ui/button";
import { UserAuthGuard } from "@/components/user-ui/user-auth-guard";

export default function ManagerApplyPage() {
  const router = useRouter();
  const [reason, setReason] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!reason.trim()) {
      setError("신청 사유를 입력해 주세요.");
      return;
    }
    setError(null);
    setLoading(true);
    try {
      await applyForManager({ reason: reason.trim() });
      setSuccess(true);
      setTimeout(() => router.replace("/me"), 2000);
    } catch (err) {
      setError(err instanceof Error ? err.message : "신청 중 오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <UserAuthGuard allowedRoles={["DUNDUN"]}>
      <TopBar title="매니저 신청" showBack />

      <div className="px-4 py-6 space-y-5">
        <div className="rounded-2xl border border-forest-100 bg-forest-50 p-5 space-y-2">
          <p className="text-lg font-bold text-forest-800">매니저란?</p>
          <p className="text-base text-forest-700">
            매니저는 이벤트를 직접 등록하고 운영할 수 있어요. 승인 후 매니저 역할이 부여됩니다.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="mb-1.5 block text-base font-medium text-gray-700">
              신청 사유 *
            </label>
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              rows={5}
              placeholder="매니저 신청 사유를 자세히 적어주세요 (활동 계획, 운영할 이벤트 종류 등)"
              className="w-full rounded-xl border border-gray-300 px-4 py-3.5 text-lg text-gray-900 placeholder-gray-400 focus:border-forest-700 focus:outline-none focus:ring-2 focus:ring-forest-700/20"
            />
          </div>

          {error && (
            <div className="rounded-xl border border-gray-200 bg-gray-50 p-4 text-base text-gray-900" role="alert">
              {error}
            </div>
          )}

          {success && (
            <div className="rounded-xl border border-forest-200 bg-forest-50 p-4 text-base text-forest-700" role="status">
              신청이 완료됐어요! 검토 후 연락드릴게요.
            </div>
          )}

          <Button
            type="submit"
            variant="primary"
            fullWidth
            loading={loading}
            disabled={success}
          >
            신청하기
          </Button>
        </form>
      </div>
    </UserAuthGuard>
  );
}
