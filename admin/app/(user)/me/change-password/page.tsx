"use client";

import React, { useState } from "react";
import { useRouter } from "next/navigation";
import { useChangePassword } from "@/lib/hooks/use-me";
import { TopBar } from "@/components/user-ui/top-bar";
import { Input } from "@/components/user-ui/input";
import { Button } from "@/components/user-ui/button";

export default function ChangePasswordPage() {
  const router = useRouter();
  const changeMutation = useChangePassword();

  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    if (!oldPassword || !newPassword || !confirmPassword) {
      setError("모든 항목을 입력해 주세요.");
      return;
    }
    if (newPassword !== confirmPassword) {
      setError("새 비밀번호가 일치하지 않습니다.");
      return;
    }
    if (newPassword.length < 6) {
      setError("비밀번호는 6자 이상이어야 합니다.");
      return;
    }

    try {
      await changeMutation.mutateAsync({ oldPassword, newPassword });
      setSuccess(true);
      setTimeout(() => router.back(), 2000);
    } catch (err) {
      setError(err instanceof Error ? err.message : "변경 중 오류가 발생했습니다.");
    }
  }

  return (
    <>
      <TopBar title="비밀번호 변경" showBack />

      <form onSubmit={handleSubmit} className="space-y-5 px-4 py-5">
        <Input
          label="현재 비밀번호"
          type="password"
          autoComplete="current-password"
          value={oldPassword}
          onChange={(e) => setOldPassword(e.target.value)}
          placeholder="현재 비밀번호 입력"
        />
        <Input
          label="새 비밀번호"
          type="password"
          autoComplete="new-password"
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
          placeholder="새 비밀번호 (6자 이상)"
        />
        <Input
          label="새 비밀번호 확인"
          type="password"
          autoComplete="new-password"
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
          placeholder="새 비밀번호를 다시 입력"
          error={confirmPassword && newPassword !== confirmPassword ? "비밀번호가 일치하지 않습니다." : undefined}
        />

        {error && (
          <div className="rounded-xl border border-gray-200 bg-gray-50 p-4 text-base text-gray-900" role="alert">
            {error}
          </div>
        )}

        {success && (
          <div className="rounded-xl border border-forest-200 bg-forest-50 p-4 text-base text-forest-700" role="status">
            비밀번호가 변경됐어요. 잠시 후 이동합니다.
          </div>
        )}

        <Button
          type="submit"
          variant="primary"
          fullWidth
          loading={changeMutation.isPending}
          disabled={success}
        >
          변경하기
        </Button>
      </form>
    </>
  );
}
