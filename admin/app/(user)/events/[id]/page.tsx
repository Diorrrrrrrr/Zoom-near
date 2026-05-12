"use client";

import React, { useState } from "react";
import { useParams } from "next/navigation";
import { useEvent, useJoinEvent, useCancelEvent } from "@/lib/hooks/use-events-user";
import { useMe } from "@/lib/hooks/use-me";
import { TopBar } from "@/components/user-ui/top-bar";
import { Button } from "@/components/user-ui/button";
import { Card } from "@/components/user-ui/card";
import { getStatusBadge } from "@/components/user-ui/event-card";
import {
  formatCategory,
  formatKoreanDate,
  formatKoreanTimeRange,
} from "@/lib/labels";
import Link from "next/link";

export default function EventDetailPage() {
  const { id } = useParams<{ id: string }>();
  const eventId = id;

  const { data: event, isLoading, isError } = useEvent(eventId);
  const { data: me } = useMe();
  const joinMutation = useJoinEvent(eventId);
  const cancelMutation = useCancelEvent(eventId);

  const [showConfirm, setShowConfirm] = useState(false);
  const [showCancelConfirm, setShowCancelConfirm] = useState(false);
  const [toast, setToast] = useState<string | null>(null);

  function showToast(msg: string) {
    setToast(msg);
    setTimeout(() => setToast(null), 3000);
  }

  async function handleJoin() {
    try {
      await joinMutation.mutateAsync(undefined);
      setShowConfirm(false);
      showToast("이벤트 참여 신청이 완료됐어요!");
    } catch (err) {
      setShowConfirm(false);
      showToast(err instanceof Error ? err.message : "오류가 발생했습니다.");
    }
  }

  async function handleCancel() {
    try {
      await cancelMutation.mutateAsync();
      setShowCancelConfirm(false);
      showToast("이벤트 참여가 취소됐어요.");
    } catch (err) {
      setShowCancelConfirm(false);
      showToast(err instanceof Error ? err.message : "오류가 발생했습니다.");
    }
  }

  if (isLoading) {
    return (
      <>
        <TopBar title="이벤트 상세" showBack />
        <div className="flex justify-center py-16">
          <span className="text-base text-gray-400">불러오는 중…</span>
        </div>
      </>
    );
  }

  if (isError || !event) {
    return (
      <>
        <TopBar title="이벤트 상세" showBack />
        <div className="px-4 py-8">
          <div className="rounded-xl border border-gray-200 bg-gray-50 p-4 text-base text-gray-900">
            이벤트 정보를 불러오지 못했습니다.
          </div>
        </div>
      </>
    );
  }

  const dateLabel = formatKoreanDate(event.startsAt);
  const timeLabel = formatKoreanTimeRange(event.startsAt, event.endsAt);

  const hasJoined = event.myParticipationStatus === "JOINED";
  const isClosed = event.status !== "OPEN";
  const balance = me?.balance ?? 0;
  const canAfford = balance >= event.pointCost;

  return (
    <>
      <TopBar title="이벤트 상세" showBack />

      <div className="px-4 py-5 space-y-5">
        {/* 상태 + 카테고리 */}
        {(() => {
          const badge = getStatusBadge(event);
          return (
            <div className="flex items-center gap-3">
              <span className={`inline-flex items-center gap-1.5 text-sm font-medium ${badge.textColor}`}>
                <span
                  aria-hidden="true"
                  className="inline-block h-2 w-2 rounded-full"
                  style={{ backgroundColor: badge.dotColor }}
                />
                {badge.label}
              </span>
              <span className="rounded-full border border-gray-200 px-3 py-1 text-sm text-gray-600">
                {formatCategory(event.category)}
              </span>
            </div>
          );
        })()}

        {/* 제목 */}
        <h2 className="text-2xl font-bold text-gray-900">{event.title}</h2>

        {/* 기본 정보 카드 */}
        <Card>
          <dl className="space-y-3">
            <div className="flex justify-between">
              <dt className="text-base text-gray-500">날짜</dt>
              <dd className="text-base font-medium text-gray-900 text-right">
                {dateLabel}
              </dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-base text-gray-500">시간</dt>
              <dd className="text-base font-medium text-gray-900 text-right">
                {timeLabel}
              </dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-base text-gray-500">장소</dt>
              <dd className="text-base font-medium text-gray-900">{event.regionText}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-base text-gray-500">참여 인원</dt>
              <dd className="text-base font-medium text-gray-900">
                {event.currentJoinedCount ?? event.joinedCount ?? 0} / {event.capacity}명
              </dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-base text-gray-500">참여 비용</dt>
              <dd className="text-lg font-bold text-forest-700">
                {event.pointCost > 0 ? `${event.pointCost.toLocaleString()}P` : "무료"}
              </dd>
            </div>
          </dl>
        </Card>

        {/* 내 잔액 */}
        {me && (
          <Card className="bg-forest-50 border-forest-100">
            <div className="flex justify-between items-center">
              <span className="text-base text-gray-600">내 잔액</span>
              <span className={`text-lg font-bold ${canAfford ? "text-forest-700" : "text-gray-800"}`}>
                {balance.toLocaleString()}P
                {!canAfford && event.pointCost > 0 && (
                  <span className="ml-2 text-sm font-normal text-gray-600">(부족)</span>
                )}
              </span>
            </div>
          </Card>
        )}

        {/* 설명 */}
        <Card>
          <h3 className="mb-2 text-base font-semibold text-gray-900">상세 내용</h3>
          <p className="whitespace-pre-wrap text-base leading-relaxed text-gray-700">
            {event.description}
          </p>
        </Card>

        {/* CTA 버튼 */}
        {!isClosed && (
          <div className="pt-2">
            {hasJoined ? (
              <Button
                variant="danger"
                fullWidth
                onClick={() => setShowCancelConfirm(true)}
                loading={cancelMutation.isPending}
              >
                참여 취소하기
              </Button>
            ) : !canAfford && event.pointCost > 0 ? (
              <div className="flex gap-3">
                <Button
                  variant="secondary"
                  disabled
                  className="flex-[4]"
                  onClick={() => {}}
                >
                  포인트가 부족해요
                </Button>
                <Link
                  href="/me/charge"
                  className="flex flex-1 h-[3.25rem] items-center justify-center rounded-xl bg-forest-700 px-3 text-base font-semibold tracking-tight-1 text-white shadow-soft-sm transition-colors hover:bg-forest-800"
                >
                  충전하기
                </Link>
              </div>
            ) : (
              <Button
                variant="primary"
                fullWidth
                onClick={() => setShowConfirm(true)}
                loading={joinMutation.isPending}
              >
                참여 신청하기
              </Button>
            )}
          </div>
        )}
      </div>

      {/* 참여 확인 다이얼로그 */}
      {showConfirm && (
        <div
          className="fixed inset-0 z-[60] flex items-end bg-black/40 p-4"
          style={{ paddingBottom: "calc(1rem + env(safe-area-inset-bottom))" }}
        >
          <div className="w-full rounded-2xl bg-white p-6 space-y-4">
            <h3 className="text-xl font-bold text-gray-900">이벤트에 참여할까요?</h3>
            <p className="text-base text-gray-600">
              {event.pointCost > 0
                ? `${event.pointCost.toLocaleString()}P가 차감됩니다.`
                : "무료로 참여할 수 있어요."}
            </p>
            <div className="flex gap-3">
              <Button variant="secondary" fullWidth onClick={() => setShowConfirm(false)}>
                취소
              </Button>
              <Button variant="primary" fullWidth onClick={handleJoin} loading={joinMutation.isPending}>
                참여하기
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* 취소 확인 다이얼로그 */}
      {showCancelConfirm && (
        <div
          className="fixed inset-0 z-[60] flex items-end bg-black/40 p-4"
          style={{ paddingBottom: "calc(1rem + env(safe-area-inset-bottom))" }}
        >
          <div className="w-full rounded-2xl bg-white p-6 space-y-4">
            <h3 className="text-xl font-bold text-gray-900">참여를 취소할까요?</h3>
            <p className="text-base text-gray-600">취소 후에는 다시 신청해야 합니다.</p>
            <div className="flex gap-3">
              <Button variant="secondary" fullWidth onClick={() => setShowCancelConfirm(false)}>
                돌아가기
              </Button>
              <Button variant="danger" fullWidth onClick={handleCancel} loading={cancelMutation.isPending}>
                취소하기
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Toast */}
      {toast && (
        <div className="fixed bottom-24 left-1/2 z-50 -translate-x-1/2 rounded-xl bg-gray-900 px-5 py-3 text-base text-white shadow-lg">
          {toast}
        </div>
      )}
    </>
  );
}
