"use client";

import React, { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useLinkages } from "@/lib/hooks/use-linkages";
import { useTopupProxy } from "@/lib/hooks/use-points";
import { TopBar } from "@/components/user-ui/top-bar";
import { Input } from "@/components/user-ui/input";
import { Button } from "@/components/user-ui/button";
import { Card } from "@/components/user-ui/card";
import { UserAuthGuard } from "@/components/user-ui/user-auth-guard";

const PRESETS = [1000, 5000, 10000, 30000];

export default function ProxyChargePage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { data: linkages, isLoading } = useLinkages();
  const topupProxy = useTopupProxy();

  const [selectedId, setSelectedId] = useState<string | null>(null);

  // URL ?tuntunId=... 로 진입했고 해당 연동이 존재하면 자동 선택
  useEffect(() => {
    const tid = searchParams.get("tuntunId");
    if (!tid || !linkages) return;
    if (linkages.some((l) => l.otherUserId === tid)) {
      setSelectedId(tid);
    }
  }, [searchParams, linkages]);
  const [amount, setAmount] = useState("");
  const [reason, setReason] = useState("");
  const [showConfirm, setShowConfirm] = useState(false);
  const [toast, setToast] = useState<string | null>(null);

  function showToast(msg: string) {
    setToast(msg);
    setTimeout(() => setToast(null), 3000);
  }

  const selectedLinkage = linkages?.find((l) => l.otherUserId === selectedId);

  async function handleCharge() {
    if (!selectedId || !amount) return;
    try {
      const result = await topupProxy.mutateAsync({
        tuntunId: selectedId,
        amount: Number(amount),
        reasonText: reason || undefined,
      });
      setShowConfirm(false);
      showToast(`충전 완료! ${selectedLinkage?.otherUserName}님의 새 잔액: ${result.newBalance.toLocaleString()}P`);
      setTimeout(() => router.back(), 2000);
    } catch (err) {
      setShowConfirm(false);
      showToast(err instanceof Error ? err.message : "충전 중 오류가 발생했습니다.");
    }
  }

  return (
    <UserAuthGuard allowedRoles={["DUNDUN"]}>
      <TopBar title="대리 충전" showBack />

      <div className="px-4 py-5 space-y-5">
        {/* 튼튼이 선택 */}
        <div>
          <p className="mb-3 text-lg font-bold text-gray-900">튼튼이 선택</p>
          {isLoading && <p className="text-base text-gray-400">불러오는 중…</p>}
          {!isLoading && (!linkages || linkages.length === 0) && (
            <p className="text-base text-gray-500">연동된 튼튼이가 없어요. 먼저 연동해 주세요.</p>
          )}
          <div className="space-y-2">
            {linkages?.map((item) => (
              <button
                key={item.id}
                onClick={() => setSelectedId(item.otherUserId)}
                className={`flex w-full items-center justify-between rounded-2xl border px-5 py-4 transition-colors ${
                  selectedId === item.otherUserId
                    ? "border-forest-700 bg-forest-50"
                    : "border-gray-200 bg-white hover:bg-gray-50"
                }`}
              >
                <div className="text-left">
                  <p className="text-lg font-semibold text-gray-900">{item.otherUserName}</p>
                  <p className="text-base text-gray-500">코드: {item.otherUserUniqueCode}</p>
                </div>
                {selectedId === item.otherUserId && (
                  <span className="text-forest-700">
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="h-6 w-6">
                      <path fillRule="evenodd" d="M2.25 12c0-5.385 4.365-9.75 9.75-9.75s9.75 4.365 9.75 9.75-4.365 9.75-9.75 9.75S2.25 17.385 2.25 12zm13.36-1.814a.75.75 0 10-1.22-.872l-3.236 4.53L9.53 12.22a.75.75 0 00-1.06 1.06l2.25 2.25a.75.75 0 001.14-.094l3.75-5.25z" clipRule="evenodd" />
                    </svg>
                  </span>
                )}
              </button>
            ))}
          </div>
        </div>

        {/* 금액 */}
        {selectedId && (
          <>
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
              placeholder="충전 사유"
            />

            <Button
              variant="primary"
              fullWidth
              disabled={!amount || Number(amount) <= 0}
              onClick={() => setShowConfirm(true)}
            >
              {amount
                ? `${selectedLinkage?.otherUserName}님께 ${Number(amount).toLocaleString()}P 충전`
                : "충전하기"}
            </Button>
          </>
        )}
      </div>

      {showConfirm && (
        <div
          className="fixed inset-0 z-[60] flex items-end bg-black/40 p-4"
          style={{ paddingBottom: "calc(1rem + env(safe-area-inset-bottom))" }}
        >
          <div className="w-full rounded-2xl bg-white p-6 space-y-4">
            <h3 className="text-xl font-bold text-gray-900">대리 충전할까요?</h3>
            <p className="text-base text-gray-600">
              <span className="font-bold">{selectedLinkage?.otherUserName}</span>님께{" "}
              <span className="font-bold text-forest-700">{Number(amount).toLocaleString()}P</span>를 충전합니다.
            </p>
            <div className="flex gap-3">
              <Button variant="secondary" fullWidth onClick={() => setShowConfirm(false)}>취소</Button>
              <Button variant="primary" fullWidth onClick={handleCharge} loading={topupProxy.isPending}>충전하기</Button>
            </div>
          </div>
        </div>
      )}

      {toast && (
        <div className="fixed bottom-24 left-1/2 z-50 -translate-x-1/2 rounded-xl bg-gray-900 px-5 py-3 text-base text-white shadow-lg">
          {toast}
        </div>
      )}
    </UserAuthGuard>
  );
}
