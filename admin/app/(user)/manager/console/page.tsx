"use client";

import React from "react";
import Link from "next/link";
import { useEvents } from "@/lib/hooks/use-events-user";
import { useMe } from "@/lib/hooks/use-me";
import { TopBar } from "@/components/user-ui/top-bar";
import { Card } from "@/components/user-ui/card";
import { Icon } from "@/components/user-ui/icon";
import { UserAuthGuard } from "@/components/user-ui/user-auth-guard";

export default function ManagerConsolePage() {
  const { data: me } = useMe();
  const { data: events, isLoading } = useEvents({ size: 10 });

  return (
    <UserAuthGuard allowedRoles={["MANAGER"]}>
      <TopBar
        title="매니저 콘솔"
        showBack
        actions={
          <Link
            href="/events/create"
            className="rounded-xl bg-forest-700 px-4 py-2 text-base font-semibold text-white"
          >
            이벤트 등록
          </Link>
        }
      />

      <div className="px-4 py-5 space-y-5">
        {/* 매니저 정보 */}
        {me && (
          <Card>
            <p className="text-lg font-bold text-gray-900">{me.name} 매니저</p>
            <p className="text-base text-gray-500">{me.rankDisplayName}</p>
          </Card>
        )}

        {/* 내 이벤트 */}
        <div>
          <h2 className="mb-3 text-lg font-bold text-gray-900">내 이벤트</h2>
          {isLoading && <p className="text-base text-gray-400">불러오는 중…</p>}
          {events && events.content.length === 0 && !isLoading && (
            <div className="flex flex-col items-center gap-3 py-10 text-gray-400">
              <Icon name="inbox" className="h-10 w-10" strokeWidth={1.5} />
              <p className="text-base">등록한 이벤트가 없어요</p>
              <Link
                href="/events/create"
                className="mt-2 rounded-xl bg-forest-700 px-5 py-2.5 text-base font-semibold text-white"
              >
                첫 이벤트 등록하기
              </Link>
            </div>
          )}
          <div className="space-y-3">
            {events?.content.map((event) => {
              const startsAt = new Date(event.startsAt).toLocaleDateString("ko-KR", {
                month: "short", day: "numeric",
              });
              return (
                <Link
                  key={event.id}
                  href={`/events/${event.id}`}
                  className="block rounded-2xl border border-gray-200 bg-white p-4 shadow-sm hover:shadow-md"
                >
                  <div className="flex items-center justify-between gap-2">
                    <div className="flex-1 min-w-0">
                      <p className="truncate text-lg font-semibold text-gray-900">{event.title}</p>
                      <p className="text-base text-gray-500">{startsAt} · {event.regionText}</p>
                    </div>
                    <div className="text-right">
                      <span className={`rounded-full px-2.5 py-0.5 text-sm font-medium ${
                        event.status === "OPEN" ? "bg-forest-50 text-forest-800" : "bg-gray-100 text-gray-600"
                      }`}>
                        {event.status === "OPEN" ? "모집중" : event.status === "CLOSED" ? "마감" : "취소됨"}
                      </span>
                      <p className="mt-1 text-sm text-gray-500">{event.joinedCount}/{event.capacity}명</p>
                    </div>
                  </div>
                </Link>
              );
            })}
          </div>
        </div>
      </div>
    </UserAuthGuard>
  );
}
