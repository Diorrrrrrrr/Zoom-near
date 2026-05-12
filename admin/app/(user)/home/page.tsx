"use client";

import React, { useMemo } from "react";
import Link from "next/link";
import { useMe } from "@/lib/hooks/use-me";
import { useBalance } from "@/lib/hooks/use-points";
import { useEvents } from "@/lib/hooks/use-events-user";
import { PointDisplay } from "@/components/user-ui/point-display";
import { Card } from "@/components/user-ui/card";
import { Icon, type IconName } from "@/components/user-ui/icon";
import { getUserRole } from "@/lib/auth/user-session";
import {
  formatCategory,
  formatShortKoreanDate,
  formatKoreanTime,
} from "@/lib/labels";

type ActionItem = { href: string; label: string; icon: IconName };

const QUICK_ACTIONS: ActionItem[] = [
  { href: "/events", label: "이벤트 보기", icon: "calendar" },
  { href: "/notifications", label: "알림 확인", icon: "bell" },
  { href: "/me", label: "내 정보", icon: "user" },
];

const DUNDUN_ACTIONS: ActionItem[] = [
  { href: "/linkage", label: "연동 관리", icon: "link" },
  { href: "/linkage/proxy-charge", label: "대리 충전", icon: "card" },
  { href: "/linkage/proxy-events", label: "대리 참여", icon: "target" },
];

export default function HomePage() {
  const { data: me, isLoading: meLoading } = useMe();
  const { data: balanceData } = useBalance();
  const { data: eventsData } = useEvents({ status: "OPEN", size: 12 });
  const role = getUserRole();

  const balance = balanceData?.balance ?? me?.balance ?? 0;

  /// 모집중 이벤트에서 3개를 무작위로 골라 픽업. 새로고침마다 셔플.
  const featured = useMemo(() => {
    const pool = eventsData?.content ?? [];
    return [...pool].sort(() => Math.random() - 0.5).slice(0, 3);
  }, [eventsData]);

  return (
    <div className="space-y-7 px-5 py-7">
      {/* 환영 메시지 */}
      <div className="space-y-1">
        <p className="text-sm font-medium uppercase tracking-widest text-gray-400">
          ZOOM NEAR
        </p>
        <h2 className="text-2xl font-bold tracking-tight-2 text-gray-900">
          {meLoading ? "안녕하세요" : `${me?.name ?? "회원"}님,`}
        </h2>
        <p className="text-base text-gray-500">오늘도 가까이 들여다보아요.</p>
        {me && (
          <p className="pt-1 text-sm text-gray-400">
            {me.rankDisplayName} · 고유 코드 {me.uniqueCode}
          </p>
        )}
      </div>

      {/* 잔액 카드 + 충전 버튼 */}
      <PointDisplay
        balance={balance}
        size="lg"
        action={{ href: "/me/charge", label: "충전하기" }}
      />

      {/* 추천 이벤트 — 모집중 3개 랜덤 */}
      {featured.length > 0 && (
        <section>
          <div className="mb-3 flex items-baseline justify-between">
            <h3 className="text-base font-semibold tracking-tight-1 text-gray-700">
              모집중 이벤트
            </h3>
            <Link
              href="/events"
              className="text-sm font-medium text-forest-700 hover:underline"
            >
              전체 보기
            </Link>
          </div>
          <div className="grid grid-cols-3 gap-3">
            {featured.map((event) => (
              <Link
                key={event.id}
                href={`/events/${event.id}`}
                className="group relative flex aspect-[3/4] flex-col justify-between overflow-hidden rounded-2xl p-4 text-white shadow-soft-md transition-all hover:-translate-y-0.5 hover:shadow-soft-md"
                style={{
                  background:
                    "linear-gradient(155deg, #1f4332 0%, #143126 60%, #0c1f18 100%)",
                }}
              >
                <span
                  aria-hidden="true"
                  className="pointer-events-none absolute -right-6 -top-6 h-20 w-20 rounded-full"
                  style={{
                    background:
                      "radial-gradient(circle, rgba(255,255,255,0.12) 0%, transparent 70%)",
                  }}
                />
                <div className="relative space-y-2">
                  <span className="inline-block rounded-full border border-white/30 bg-white/10 px-2.5 py-0.5 text-xs font-semibold tracking-tight-1 text-white backdrop-blur-sm">
                    {formatCategory(event.category)}
                  </span>
                  <p className="line-clamp-2 text-base font-bold leading-snug tracking-tight-1 text-white">
                    {event.title}
                  </p>
                </div>
                <div className="relative space-y-1">
                  <p className="text-sm font-medium text-white/85">
                    {formatShortKoreanDate(event.startsAt)}
                  </p>
                  <p className="text-sm text-white/75">
                    {formatKoreanTime(event.startsAt)}
                  </p>
                  <p className="truncate text-xs text-white/55">
                    {event.regionText}
                  </p>
                  <p className="pt-1 text-base font-bold tracking-tight-1 text-white">
                    {event.pointCost > 0
                      ? `${event.pointCost.toLocaleString()}P`
                      : "무료"}
                  </p>
                </div>
              </Link>
            ))}
          </div>
        </section>
      )}

      {/* 빠른 액션 */}
      <section>
        <h3 className="mb-3 text-base font-semibold tracking-tight-1 text-gray-700">
          빠른 메뉴
        </h3>
        <div className="grid grid-cols-3 gap-3">
          {QUICK_ACTIONS.map((action) => (
            <Link
              key={action.href}
              href={action.href}
              className="group flex flex-col items-center gap-2 rounded-2xl border border-gray-100 bg-white p-5 shadow-soft-sm transition-all hover:-translate-y-0.5 hover:border-forest-200 hover:shadow-soft-md active:translate-y-0"
            >
              <Icon name={action.icon} className="h-6 w-6 text-forest-700" />
              <span className="text-sm font-semibold tracking-tight-1 text-gray-900 group-hover:text-forest-700">
                {action.label}
              </span>
            </Link>
          ))}
        </div>
      </section>

      {/* 튼튼이 전용: 승인 */}
      {role === "TUNTUN" && (
        <Card>
          <div className="flex items-center justify-between gap-4">
            <div className="min-w-0">
              <p className="text-base font-semibold tracking-tight-1 text-gray-900">
                승인 대기
              </p>
              <p className="mt-0.5 text-sm text-gray-500">
                자녀 든든이의 이벤트 신청을 확인해 주세요
              </p>
            </div>
            <Link
              href="/approvals"
              className="shrink-0 rounded-xl bg-forest-700 px-4 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-forest-800"
            >
              확인
            </Link>
          </div>
        </Card>
      )}

      {/* 든든이 전용 */}
      {role === "DUNDUN" && (
        <section>
          <h3 className="mb-3 text-base font-semibold tracking-tight-1 text-gray-700">
            연동 메뉴
          </h3>
          <div className="grid grid-cols-3 gap-3">
            {DUNDUN_ACTIONS.map((action) => (
              <Link
                key={action.href}
                href={action.href}
                className="flex flex-col items-center gap-2 rounded-2xl border border-gray-100 bg-white p-4 shadow-soft-sm transition-all hover:-translate-y-0.5 hover:border-forest-200 hover:shadow-soft-md"
              >
                <Icon name={action.icon} className="h-6 w-6 text-forest-700" />
                <span className="text-center text-xs font-semibold tracking-tight-1 text-gray-800">
                  {action.label}
                </span>
              </Link>
            ))}
          </div>
        </section>
      )}

      {/* 매니저 전용 */}
      {role === "MANAGER" && (
        <Card>
          <div className="flex items-center justify-between gap-4">
            <div className="min-w-0">
              <p className="text-base font-semibold tracking-tight-1 text-gray-900">
                매니저 콘솔
              </p>
              <p className="mt-0.5 text-sm text-gray-500">
                이벤트 등록 및 관리
              </p>
            </div>
            <Link
              href="/manager/console"
              className="shrink-0 rounded-xl bg-forest-700 px-4 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-forest-800"
            >
              이동
            </Link>
          </div>
        </Card>
      )}
    </div>
  );
}
