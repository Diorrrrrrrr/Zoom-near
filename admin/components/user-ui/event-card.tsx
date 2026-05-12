import React from "react";
import Link from "next/link";
import type { EventDetail, EventSummary } from "@/lib/types-user";
import { formatShortKoreanDate, formatKoreanTime } from "@/lib/labels";

interface EventCardProps {
  event: EventSummary;
}

interface StatusBadge {
  label: string;
  /** 상태 점 색상 (CSS 변수 값) */
  dotColor: string;
  textColor: string;
}

/// 이벤트 상태 + 잔여 인원 기반 표시 배지.
/// 색상은 기능적 상태 지표(traffic-light)로 3색 팔레트 외 예외로 허용.
export function getStatusBadge(
  event: EventSummary | EventDetail
): StatusBadge {
  const joined =
    (event as EventDetail).currentJoinedCount ?? event.joinedCount ?? 0;
  const spotsLeft = event.capacity - joined;

  if (event.status === "CANCELLED") {
    return { label: "취소됨", dotColor: "#9ca3af", textColor: "text-gray-500" };
  }
  if (event.status === "CLOSED" || spotsLeft <= 0) {
    return { label: "마감", dotColor: "#dc2626", textColor: "text-gray-700" };
  }
  if (spotsLeft <= event.capacity / 2) {
    return { label: "마감 예정", dotColor: "#d97706", textColor: "text-gray-700" };
  }
  return { label: "모집중", dotColor: "#16a34a", textColor: "text-gray-700" };
}

export function EventCard({ event }: EventCardProps) {
  const dateLabel = formatShortKoreanDate(event.startsAt);
  const timeLabel = formatKoreanTime(event.startsAt);

  const joined = event.joinedCount ?? 0;
  const spotsLeft = Math.max(0, event.capacity - joined);
  const badge = getStatusBadge(event);

  return (
    <Link
      href={`/events/${event.id}`}
      className={`block rounded-2xl border p-5 shadow-soft-sm transition-all hover:-translate-y-0.5 active:translate-y-0 hover:shadow-soft-md ${
        event.managerProgram
          ? "border-forest-300 bg-forest-50/70 hover:border-forest-400"
          : "border-gray-100 bg-white hover:border-forest-200"
      }`}
    >
      <div className="mb-2 flex items-center justify-between gap-2">
        <div className="flex items-center gap-2 min-w-0">
          <span
            className={`inline-flex items-center gap-1.5 text-sm font-medium shrink-0 ${badge.textColor}`}
          >
            <span
              aria-hidden="true"
              className="inline-block h-2 w-2 rounded-full"
              style={{ backgroundColor: badge.dotColor }}
            />
            {badge.label}
          </span>
          {event.managerProgram && (
            <span className="inline-flex items-center rounded-full bg-forest-700 px-2 py-0.5 text-[11px] font-semibold tracking-tight-1 text-white whitespace-nowrap">
              주니어 자체 프로그램
            </span>
          )}
        </div>
        <span className="text-sm text-gray-500 shrink-0">{event.regionText}</span>
      </div>

      <h3 className="mb-1 text-lg font-bold tracking-tight-1 text-gray-900 line-clamp-2">
        {event.title}
      </h3>

      <p className="mb-3 text-sm text-gray-500">
        {dateLabel} · {timeLabel}
      </p>

      <div className="flex items-center justify-between">
        <span className="text-base font-bold tracking-tight-1 text-forest-700">
          {event.pointCost > 0 ? `${event.pointCost.toLocaleString()}P` : "무료"}
        </span>
        <span className="text-sm text-gray-500">
          {event.status === "OPEN"
            ? spotsLeft > 0
              ? `잔여 ${spotsLeft}자리`
              : "마감"
            : `${joined}명 참여`}
        </span>
      </div>
    </Link>
  );
}
