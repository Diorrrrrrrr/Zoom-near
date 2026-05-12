"use client";

import React, { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useNotifications, useMarkRead } from "@/lib/hooks/use-notifications";
import { useApproveItem, useRejectItem } from "@/lib/hooks/use-approvals";
import { TopBar } from "@/components/user-ui/top-bar";
import { Icon } from "@/components/user-ui/icon";
import type { NotificationItem } from "@/lib/types-user";

/// 알림 payload 에서 approvalId 안전 추출.
function getApprovalId(item: NotificationItem): string | null {
  const p = item.payload as Record<string, unknown> | null | undefined;
  if (!p) return null;
  const id = p.approvalId;
  return typeof id === "string" ? id : null;
}

/// 인라인 승인/거절 버튼을 표시할 알림 타입 (요청 수신형).
const ACTIONABLE_TYPES = new Set<string>([
  "LINKAGE_REQUEST_RECEIVED",
  "EVENT_JOIN_REQUEST",
  "EVENT_CANCEL_REQUEST",
  "EVENT_CREATE_REQUEST",
]);

export default function NotificationsPage() {
  const queryClient = useQueryClient();
  const { data, isLoading, isError } = useNotifications({ limit: 50 });
  const markRead = useMarkRead();
  const approveMutation = useApproveItem();
  const rejectMutation = useRejectItem();
  const [busyId, setBusyId] = useState<string | null>(null);
  const [toast, setToast] = useState<string | null>(null);

  function showToast(msg: string) {
    setToast(msg);
    setTimeout(() => setToast(null), 2500);
  }

  async function handleRead(id: string) {
    await markRead.mutateAsync(id);
  }

  async function handleApprove(notif: NotificationItem) {
    const approvalId = getApprovalId(notif);
    if (!approvalId) return;
    setBusyId(notif.id);
    try {
      await approveMutation.mutateAsync(approvalId);
      await markRead.mutateAsync(notif.id);
      queryClient.invalidateQueries({ queryKey: ["linkages"] });
      queryClient.invalidateQueries({ queryKey: ["approvals"] });
      showToast("승인했어요.");
    } catch (err) {
      showToast(err instanceof Error ? err.message : "처리 중 오류");
    } finally {
      setBusyId(null);
    }
  }

  async function handleReject(notif: NotificationItem) {
    const approvalId = getApprovalId(notif);
    if (!approvalId) return;
    setBusyId(notif.id);
    try {
      await rejectMutation.mutateAsync(approvalId);
      await markRead.mutateAsync(notif.id);
      queryClient.invalidateQueries({ queryKey: ["approvals"] });
      showToast("거절했어요.");
    } catch (err) {
      showToast(err instanceof Error ? err.message : "처리 중 오류");
    } finally {
      setBusyId(null);
    }
  }

  return (
    <>
      <TopBar title="알림" />

      <div className="px-4 py-5 space-y-3">
        {isLoading && (
          <div className="flex justify-center py-12">
            <span className="text-base text-gray-400">불러오는 중…</span>
          </div>
        )}

        {isError && (
          <div className="rounded-xl border border-gray-200 bg-gray-50 p-4 text-base text-gray-900">
            알림을 불러오지 못했습니다.
          </div>
        )}

        {data && data.items.length === 0 && !isLoading && (
          <div className="flex flex-col items-center gap-3 py-16 text-gray-400">
            <Icon name="bell" className="h-10 w-10" strokeWidth={1.5} />
            <p className="text-base">새로운 알림이 없어요</p>
          </div>
        )}

        {data?.items.map((item) => {
          const unread = !item.readAt;
          const createdAt = new Date(item.createdAt).toLocaleString("ko-KR", {
            month: "short",
            day: "numeric",
            hour: "2-digit",
            minute: "2-digit",
          });
          const actionable = ACTIONABLE_TYPES.has(item.type) && !!getApprovalId(item);
          const busy = busyId === item.id;

          return (
            <div
              key={item.id}
              className={`rounded-2xl border p-5 transition-colors ${
                unread
                  ? "border-forest-100 bg-forest-50"
                  : "border-gray-100 bg-white opacity-70"
              }`}
            >
              <div
                className="flex items-start justify-between gap-3"
                onClick={() => !actionable && unread && handleRead(item.id)}
              >
                <div className="min-w-0 flex-1">
                  {unread && (
                    <span className="mb-1 inline-block h-2 w-2 rounded-full bg-forest-700" />
                  )}
                  <p className="text-base font-semibold tracking-tight-1 text-gray-900">
                    {item.title}
                  </p>
                  <p className="mt-0.5 text-sm leading-snug text-gray-700">
                    {item.body}
                  </p>
                  <p className="mt-1 text-xs text-gray-500">{createdAt}</p>
                </div>
                {!actionable && unread && (
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      handleRead(item.id);
                    }}
                    className="shrink-0 rounded-lg px-3 py-1 text-sm font-medium text-forest-700 hover:bg-forest-100"
                  >
                    읽음
                  </button>
                )}
              </div>

              {/* 인라인 승인/거절 (요청 수신형 알림 한정) */}
              {actionable && unread && (
                <div className="mt-3 flex gap-2">
                  <button
                    disabled={busy}
                    onClick={() => handleReject(item)}
                    className="flex-1 rounded-xl border border-gray-900 bg-white px-3 py-2.5 text-sm font-semibold text-gray-900 transition-colors hover:bg-gray-50 disabled:opacity-50"
                  >
                    거절
                  </button>
                  <button
                    disabled={busy}
                    onClick={() => handleApprove(item)}
                    className="flex-1 rounded-xl bg-forest-700 px-3 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-forest-800 disabled:opacity-50"
                  >
                    {busy ? "처리 중…" : "승인"}
                  </button>
                </div>
              )}
            </div>
          );
        })}
      </div>

      {toast && (
        <div className="fixed bottom-24 left-1/2 z-50 -translate-x-1/2 rounded-xl bg-gray-900 px-5 py-3 text-base text-white shadow-lg">
          {toast}
        </div>
      )}
    </>
  );
}
