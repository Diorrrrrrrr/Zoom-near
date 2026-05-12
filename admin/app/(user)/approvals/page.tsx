"use client";

import React, { useState } from "react";
import { useApprovals, useApproveItem, useRejectItem } from "@/lib/hooks/use-approvals";
import { TopBar } from "@/components/user-ui/top-bar";
import { Button } from "@/components/user-ui/button";
import { Icon } from "@/components/user-ui/icon";
import { UserAuthGuard } from "@/components/user-ui/user-auth-guard";
import type { ApprovalItem } from "@/lib/types-user";

const TYPE_LABEL: Record<ApprovalItem["type"], string> = {
  EVENT_JOIN: "이벤트 참여 요청",
  EVENT_CANCEL: "이벤트 취소 요청",
  EVENT_CREATE: "이벤트 등록 요청",
  LINKAGE_CREATE: "연동 요청",
};

export default function ApprovalsPage() {
  const { data, isLoading, isError } = useApprovals({ status: "PENDING", limit: 20 });
  const approveMutation = useApproveItem();
  const rejectMutation = useRejectItem();

  const [confirmId, setConfirmId] = useState<string | null>(null);
  const [confirmAction, setConfirmAction] = useState<"approve" | "reject" | null>(null);
  const [toast, setToast] = useState<string | null>(null);

  function showToast(msg: string) {
    setToast(msg);
    setTimeout(() => setToast(null), 3000);
  }

  async function handleAction() {
    if (!confirmId || !confirmAction) return;
    try {
      if (confirmAction === "approve") {
        await approveMutation.mutateAsync(confirmId);
        showToast("승인됐어요!");
      } else {
        await rejectMutation.mutateAsync(confirmId);
        showToast("거절됐어요.");
      }
    } catch (err) {
      showToast(err instanceof Error ? err.message : "처리 중 오류가 발생했습니다.");
    } finally {
      setConfirmId(null);
      setConfirmAction(null);
    }
  }

  return (
    <UserAuthGuard allowedRoles={["TUNTUN", "DUNDUN"]}>
      <TopBar title="승인 관리" showBack />

      <div className="px-4 py-5 space-y-3">
        {isLoading && (
          <div className="flex justify-center py-12">
            <span className="text-base text-gray-400">불러오는 중…</span>
          </div>
        )}

        {isError && (
          <div className="rounded-xl border border-gray-200 bg-gray-50 p-4 text-base text-gray-900">
            승인 목록을 불러오지 못했습니다.
          </div>
        )}

        {data && data.items.length === 0 && !isLoading && (
          <div className="flex flex-col items-center gap-3 py-16 text-gray-400">
            <Icon name="check" className="h-10 w-10" strokeWidth={1.5} />
            <p className="text-base">대기 중인 승인 요청이 없어요</p>
          </div>
        )}

        {data?.items.map((item) => {
          const createdAt = new Date(item.createdAt).toLocaleString("ko-KR", {
            month: "short",
            day: "numeric",
            hour: "2-digit",
            minute: "2-digit",
          });
          const typeLabel = TYPE_LABEL[item.type] ?? item.type;

          return (
            <div
              key={item.id}
              className="rounded-2xl border border-gray-100 bg-white p-5 shadow-soft-sm space-y-3"
            >
              <div>
                <span className="inline-block rounded-full bg-forest-50 px-2.5 py-0.5 text-xs font-semibold text-forest-700">
                  {typeLabel}
                </span>
                <p className="mt-2 text-lg font-bold tracking-tight-1 text-gray-900">
                  {item.requesterName} 님의 요청
                </p>
                {item.requesterLoginId && (
                  <p className="mt-0.5 text-sm text-gray-500">
                    아이디 · {item.requesterLoginId}
                  </p>
                )}
                {item.payloadSummary && (
                  <p className="mt-1 text-base text-gray-700">
                    {item.payloadSummary}
                  </p>
                )}
                <p className="mt-1 text-sm text-gray-400">요청 시각 · {createdAt}</p>
              </div>
              <div className="flex gap-3">
                <Button
                  variant="danger"
                  className="h-11 flex-1 text-base"
                  onClick={() => {
                    setConfirmId(item.id);
                    setConfirmAction("reject");
                  }}
                >
                  거절
                </Button>
                <Button
                  variant="primary"
                  className="h-11 flex-1 text-base"
                  onClick={() => {
                    setConfirmId(item.id);
                    setConfirmAction("approve");
                  }}
                >
                  승인
                </Button>
              </div>
            </div>
          );
        })}
      </div>

      {/* 확인 다이얼로그 */}
      {confirmId !== null && confirmAction !== null && (
        <div
          className="fixed inset-0 z-[60] flex items-end bg-black/40 p-4"
          style={{ paddingBottom: "calc(1rem + env(safe-area-inset-bottom))" }}
        >
          <div className="w-full rounded-2xl bg-white p-6 space-y-4">
            <h3 className="text-xl font-bold text-gray-900">
              {confirmAction === "approve" ? "승인할까요?" : "거절할까요?"}
            </h3>
            <p className="text-base text-gray-600">
              {confirmAction === "approve"
                ? "승인하면 해당 요청이 즉시 처리됩니다."
                : "거절하면 해당 요청이 종료됩니다."}
            </p>
            <div className="flex gap-3">
              <Button
                variant="secondary"
                fullWidth
                onClick={() => {
                  setConfirmId(null);
                  setConfirmAction(null);
                }}
              >
                취소
              </Button>
              <Button
                variant={confirmAction === "approve" ? "primary" : "danger"}
                fullWidth
                onClick={handleAction}
                loading={approveMutation.isPending || rejectMutation.isPending}
              >
                {confirmAction === "approve" ? "승인하기" : "거절하기"}
              </Button>
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
