"use client";

import React, { useState } from "react";
import { useLinkages } from "@/lib/hooks/use-linkages";
import { useEvents, useJoinEvent } from "@/lib/hooks/use-events-user";
import { TopBar } from "@/components/user-ui/top-bar";
import { Button } from "@/components/user-ui/button";
import { EventCard } from "@/components/user-ui/event-card";
import { UserAuthGuard } from "@/components/user-ui/user-auth-guard";

export default function ProxyEventsPage() {
  const { data: linkages, isLoading: linkagesLoading } = useLinkages();
  const [selectedTuntunId, setSelectedTuntunId] = useState<string | null>(null);
  const [selectedEventId, setSelectedEventId] = useState<string | null>(null);
  const [toast, setToast] = useState<string | null>(null);

  const { data: events, isLoading: eventsLoading } = useEvents({ status: "OPEN", size: 20 });
  const joinMutation = useJoinEvent(selectedEventId ?? "");

  function showToast(msg: string) {
    setToast(msg);
    setTimeout(() => setToast(null), 3000);
  }

  const selectedLinkage = linkages?.find((l) => l.otherUserId === selectedTuntunId);

  async function handleJoin() {
    if (!selectedTuntunId || !selectedEventId) return;
    try {
      await joinMutation.mutateAsync(selectedTuntunId);
      setSelectedEventId(null);
      showToast(`${selectedLinkage?.otherUserName}님 대신 이벤트에 참여했어요!`);
    } catch (err) {
      setSelectedEventId(null);
      showToast(err instanceof Error ? err.message : "참여 중 오류가 발생했습니다.");
    }
  }

  return (
    <UserAuthGuard allowedRoles={["DUNDUN"]}>
      <TopBar title="대리 이벤트 참여" showBack />

      <div className="px-4 py-5 space-y-5">
        {/* 튼튼이 선택 */}
        <div>
          <p className="mb-3 text-lg font-bold text-gray-900">1단계: 튼튼이 선택</p>
          {linkagesLoading && <p className="text-base text-gray-400">불러오는 중…</p>}
          {!linkagesLoading && (!linkages || linkages.length === 0) && (
            <p className="text-base text-gray-500">연동된 튼튼이가 없어요.</p>
          )}
          <div className="flex flex-wrap gap-2">
            {linkages?.map((item) => (
              <button
                key={item.id}
                onClick={() => setSelectedTuntunId(item.otherUserId)}
                className={`rounded-xl border px-4 py-2.5 text-base font-semibold transition-colors ${
                  selectedTuntunId === item.otherUserId
                    ? "border-forest-700 bg-forest-700 text-white"
                    : "border-gray-300 bg-white text-gray-700 hover:border-forest-700 hover:text-forest-700"
                }`}
              >
                {item.otherUserName}
              </button>
            ))}
          </div>
        </div>

        {/* 이벤트 목록 */}
        {selectedTuntunId && (
          <div>
            <p className="mb-3 text-lg font-bold text-gray-900">2단계: 참여할 이벤트 선택</p>
            {eventsLoading && <p className="text-base text-gray-400">불러오는 중…</p>}
            <div className="space-y-3">
              {events?.content.map((event) => (
                <div key={event.id} className="relative">
                  <EventCard event={event} />
                  <button
                    onClick={() => setSelectedEventId(event.id)}
                    className="absolute right-4 top-4 rounded-xl bg-forest-700 px-4 py-2 text-sm font-semibold text-white shadow"
                  >
                    대리 참여
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* 확인 다이얼로그 */}
      {selectedEventId !== null && (
        <div
          className="fixed inset-0 z-[60] flex items-end bg-black/40 p-4"
          style={{ paddingBottom: "calc(1rem + env(safe-area-inset-bottom))" }}
        >
          <div className="w-full rounded-2xl bg-white p-6 space-y-4">
            <h3 className="text-xl font-bold text-gray-900">대리 참여할까요?</h3>
            <p className="text-base text-gray-600">
              <span className="font-bold">{selectedLinkage?.otherUserName}</span>님을 대신해 이벤트에 참여합니다.
            </p>
            <div className="flex gap-3">
              <Button variant="secondary" fullWidth onClick={() => setSelectedEventId(null)}>취소</Button>
              <Button variant="primary" fullWidth onClick={handleJoin} loading={joinMutation.isPending}>참여하기</Button>
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
