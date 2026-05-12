"use client";

import React, { useState } from "react";
import { useRouter } from "next/navigation";
import { useTopup } from "@/lib/hooks/use-points";
import { useMe } from "@/lib/hooks/use-me";
import { TopBar } from "@/components/user-ui/top-bar";
import { Input } from "@/components/user-ui/input";
import { Button } from "@/components/user-ui/button";
import { Card } from "@/components/user-ui/card";

const PRESETS = [1000, 5000, 10000, 30000, 50000];

export default function ChargePage() {
  const router = useRouter();
  const { data: me } = useMe();
  const topupMutation = useTopup();

  const [amount, setAmount] = useState("");
  const [reason, setReason] = useState("");
  const [toast, setToast] = useState<string | null>(null);
  const [showConfirm, setShowConfirm] = useState(false);

  function showToast(msg: string) {
    setToast(msg);
    setTimeout(() => setToast(null), 3000);
  }

  async function handleCharge() {
    const parsed = Number(amount);
    if (!parsed || parsed <= 0) {
      showToast("충전할 포인트를 입력해 주세요.");
      return;
    }
    try {
      const result = await topupMutation.mutateAsync({ amount: parsed, reasonText: reason || undefined });
      setShowConfirm(false);
      setAmount("");
      setReason("");
      showToast(`충전 완료! 현재 잔액: ${result.newBalance.toLocaleString()}P`);
      setTimeout(() => router.back(), 1500);
    } catch (err) {
      setShowConfirm(false);
      showToast(err instanceof Error ? err.message : "충전 중 오류가 발생했습니다.");
    }
  }

  return (
    <>
      <TopBar title="포인트 충전" showBack />

      <div className="px-4 py-5 space-y-5">
        {/* 현재 잔액 */}
        {me && (
          <Card className="bg-forest-700 border-forest-700 text-white">
            <p className="text-base opacity-80">현재 잔액</p>
            <p className="text-3xl font-bold">{me.balance.toLocaleString()}<span className="ml-1 text-lg opacity-80">P</span></p>
          </Card>
        )}

        {/* 빠른 선택 */}
        <div>
          <p className="mb-3 text-base font-medium text-gray-700">빠른 선택</p>
          <div className="flex flex-wrap gap-2">
            {PRESETS.map((p) => (
              <button
                key={p}
                onClick={() => setAmount(String(p))}
                className={`rounded-xl border px-4 py-2.5 text-base font-semibold transition-colors ${
                  amount === String(p)
                    ? "border-forest-700 bg-forest-700 text-white"
                    : "border-gray-300 bg-white text-gray-700 hover:border-forest-700 hover:text-forest-700"
                }`}
              >
                {p.toLocaleString()}P
              </button>
            ))}
          </div>
        </div>

        {/* 직접 입력 */}
        <Input
          label="직접 입력"
          type="number"
          min="1"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          placeholder="충전할 포인트"
        />

        <Input
          label="메모 (선택)"
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          placeholder="충전 사유를 입력하세요"
        />

        <Button
          variant="primary"
          fullWidth
          onClick={() => setShowConfirm(true)}
          disabled={!amount || Number(amount) <= 0}
        >
          {amount ? `${Number(amount).toLocaleString()}P 충전하기` : "충전하기"}
        </Button>
      </div>

      {/* 확인 다이얼로그 */}
      {showConfirm && (
        <div
          className="fixed inset-0 z-[60] flex items-end bg-black/40 p-4"
          style={{ paddingBottom: "calc(1rem + env(safe-area-inset-bottom))" }}
        >
          <div className="w-full rounded-2xl bg-white p-6 space-y-4">
            <h3 className="text-xl font-bold text-gray-900">포인트를 충전할까요?</h3>
            <p className="text-base text-gray-600">
              <span className="font-bold text-forest-700">{Number(amount).toLocaleString()}P</span>가 충전됩니다.
            </p>
            <div className="flex gap-3">
              <Button variant="secondary" fullWidth onClick={() => setShowConfirm(false)}>취소</Button>
              <Button variant="primary" fullWidth onClick={handleCharge} loading={topupMutation.isPending}>충전하기</Button>
            </div>
          </div>
        </div>
      )}

      {toast && (
        <div className="fixed bottom-24 left-1/2 z-50 -translate-x-1/2 rounded-xl bg-gray-900 px-5 py-3 text-base text-white shadow-lg">
          {toast}
        </div>
      )}
    </>
  );
}
