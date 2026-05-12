"use client";

import React, { useEffect, useRef, useState } from "react";
import { TopBar } from "@/components/user-ui/top-bar";
import { Button } from "@/components/user-ui/button";
import { UserAuthGuard } from "@/components/user-ui/user-auth-guard";
import { Input } from "@/components/user-ui/input";
import { useMyEvents, useUpdateEvent, useDeleteEvent } from "@/lib/hooks/use-events-user";
import type { EventSummary } from "@/lib/types-user";
import type { UpdateEventRequest } from "@/lib/api/user-events";

const CATEGORIES = ["문화", "스포츠", "봉사", "교육", "기타"];

const STATUS_LABELS: Record<string, string> = {
  OPEN: "모집중",
  CLOSED: "종료",
  CANCELLED: "취소",
};

const STATUS_COLORS: Record<string, string> = {
  OPEN: "bg-forest-50 text-forest-700 border border-forest-200",
  CLOSED: "bg-gray-100 text-gray-500 border border-gray-200",
  CANCELLED: "bg-red-50 text-red-600 border border-red-200",
};

/// 날짜(YYYY-MM-DD) + 시 + 분 → ISO 문자열. 잘못된 입력이면 null.
function composeIso(date: string, hh: string, mm: string): string | null {
  if (!date) return null;
  const h = Number(hh);
  const m = Number(mm);
  if (!Number.isFinite(h) || h < 0 || h > 23) return null;
  if (!Number.isFinite(m) || m < 0 || m > 59) return null;
  const local = new Date(
    `${date}T${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}:00`
  );
  if (Number.isNaN(local.getTime())) return null;
  return local.toISOString();
}

/// ISO 문자열에서 YYYY-MM-DD 부분 추출
function isoToDate(iso: string): string {
  if (!iso) return "";
  try {
    return iso.slice(0, 10);
  } catch {
    return "";
  }
}

/// ISO 문자열에서 시 추출
function isoToHour(iso: string): string {
  if (!iso) return "";
  try {
    return String(new Date(iso).getHours());
  } catch {
    return "";
  }
}

/// ISO 문자열에서 분 추출
function isoToMinute(iso: string): string {
  if (!iso) return "";
  try {
    return String(new Date(iso).getMinutes());
  } catch {
    return "";
  }
}

interface EditForm {
  title: string;
  description: string;
  regionText: string;
  category: string;
  startDate: string;
  startHour: string;
  startMinute: string;
  endDate: string;
  endHour: string;
  endMinute: string;
  sameDay: boolean;
  capacity: string;
  pointCost: string;
}

function initEditForm(ev: EventSummary & { description?: string }): EditForm {
  return {
    title: ev.title,
    description: (ev as { description?: string }).description ?? "",
    regionText: ev.regionText,
    category: ev.category,
    startDate: isoToDate(ev.startsAt),
    startHour: isoToHour(ev.startsAt),
    startMinute: isoToMinute(ev.startsAt),
    endDate: isoToDate(ev.endsAt),
    endHour: isoToHour(ev.endsAt),
    endMinute: isoToMinute(ev.endsAt),
    sameDay: false,
    capacity: String(ev.capacity),
    pointCost: String(ev.pointCost),
  };
}

interface EditModalProps {
  event: EventSummary;
  onClose: () => void;
  onSave: (data: UpdateEventRequest) => void;
  isPending: boolean;
}

function EditModal({ event, onClose, onSave, isPending }: EditModalProps) {
  const [form, setForm] = useState<EditForm>(() => initEditForm(event));
  const [error, setError] = useState<string | null>(null);

  function setField<K extends keyof EditForm>(key: K, value: EditForm[K]) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  function digitsOnly(setter: (v: string) => void, maxValue?: number) {
    return (e: React.ChangeEvent<HTMLInputElement>) => {
      const v = e.target.value;
      if (v === "") return setter("");
      if (!/^\d+$/.test(v)) return;
      if (maxValue !== undefined && Number(v) > maxValue) return;
      setter(v);
    };
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    if (
      !form.title ||
      !form.description ||
      !form.regionText ||
      !form.startDate ||
      form.startHour === "" ||
      form.startMinute === ""
    ) {
      setError("필수 항목을 모두 입력해 주세요.");
      return;
    }

    const startIso = composeIso(form.startDate, form.startHour, form.startMinute);
    if (!startIso) {
      setError("시작 일시가 올바르지 않습니다.");
      return;
    }

    let endIso: string | null;
    if (form.sameDay) {
      endIso = composeIso(form.startDate, "23", "59");
    } else {
      if (!form.endDate || form.endHour === "" || form.endMinute === "") {
        setError("종료 일시를 모두 입력해 주세요.");
        return;
      }
      endIso = composeIso(form.endDate, form.endHour, form.endMinute);
      if (!endIso) {
        setError("종료 일시가 올바르지 않습니다.");
        return;
      }
      if (new Date(endIso) <= new Date(startIso)) {
        setError("종료 일시는 시작 일시 이후여야 합니다.");
        return;
      }
    }

    const capacityNum = Math.max(1, Number(form.capacity) || 0);
    const pointCostNum = Math.max(0, Number(form.pointCost) || 0);

    onSave({
      title: form.title,
      description: form.description,
      regionText: form.regionText,
      category: form.category,
      startsAt: startIso,
      endsAt: endIso!,
      capacity: capacityNum,
      pointCost: pointCostNum,
    });
  }

  const endDisabled = form.sameDay;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="이벤트 수정"
      className="fixed inset-0 z-[60] flex items-end bg-black/40"
      style={{ paddingBottom: "calc(0px + env(safe-area-inset-bottom))" }}
    >
      <div className="w-full rounded-t-2xl bg-white overflow-y-auto max-h-[90vh]">
        <div className="sticky top-0 flex items-center justify-between border-b border-gray-100 bg-white px-5 py-4">
          <h2 className="text-lg font-bold text-gray-900">이벤트 수정</h2>
          <button
            type="button"
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600"
            aria-label="닫기"
          >
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} className="h-6 w-6">
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5 px-5 py-5">
          <Input
            label="이벤트 제목 *"
            name="title"
            value={form.title}
            onChange={(e) => setField("title", e.target.value)}
            placeholder="이벤트 이름을 입력하세요"
          />

          <div className="flex flex-col gap-1.5">
            <label className="text-base font-medium text-gray-700">상세 내용 *</label>
            <textarea
              value={form.description}
              onChange={(e) => setField("description", e.target.value)}
              rows={4}
              placeholder="이벤트 내용을 입력하세요"
              className="w-full rounded-xl border border-gray-300 px-4 py-3.5 text-lg text-gray-900 placeholder-gray-400 focus:border-forest-700 focus:outline-none focus:ring-2 focus:ring-forest-700/20"
            />
          </div>

          <Input
            label="지역 *"
            name="regionText"
            value={form.regionText}
            onChange={(e) => setField("regionText", e.target.value)}
            placeholder="예: 서울 강남구"
          />

          <div className="flex flex-col gap-1.5">
            <label className="text-base font-medium text-gray-700">카테고리</label>
            <select
              value={form.category}
              onChange={(e) => setField("category", e.target.value)}
              className="w-full rounded-xl border border-gray-300 px-4 py-3.5 text-lg text-gray-900 focus:border-forest-700 focus:outline-none focus:ring-2 focus:ring-forest-700/20"
            >
              {CATEGORIES.map((c) => (
                <option key={c} value={c}>{c}</option>
              ))}
            </select>
          </div>

          {/* 시작 일시 */}
          <div className="flex flex-col gap-1.5">
            <label className="text-base font-medium text-gray-700">시작 일시 *</label>
            <div className="grid grid-cols-[1fr_4rem_4rem] gap-2">
              <input
                type="date"
                value={form.startDate}
                onChange={(e) => setField("startDate", e.target.value)}
                className="w-full rounded-xl border border-gray-300 px-3 py-3 text-base text-gray-900 focus:border-forest-700 focus:outline-none focus:ring-2 focus:ring-forest-700/20"
              />
              <input
                type="text"
                inputMode="numeric"
                pattern="\d*"
                maxLength={2}
                placeholder="시"
                value={form.startHour}
                onChange={digitsOnly((v) => setField("startHour", v), 23)}
                className="w-full rounded-xl border border-gray-300 px-2 py-3 text-center text-base text-gray-900 placeholder-gray-400 focus:border-forest-700 focus:outline-none focus:ring-2 focus:ring-forest-700/20"
                aria-label="시작 시"
              />
              <input
                type="text"
                inputMode="numeric"
                pattern="\d*"
                maxLength={2}
                placeholder="분"
                value={form.startMinute}
                onChange={digitsOnly((v) => setField("startMinute", v), 59)}
                className="w-full rounded-xl border border-gray-300 px-2 py-3 text-center text-base text-gray-900 placeholder-gray-400 focus:border-forest-700 focus:outline-none focus:ring-2 focus:ring-forest-700/20"
                aria-label="시작 분"
              />
            </div>
            <label className="mt-1 flex items-center gap-2.5 text-sm text-gray-700">
              <input
                type="checkbox"
                checked={form.sameDay}
                onChange={(e) => setField("sameDay", e.target.checked)}
                className="h-4 w-4 rounded border-gray-300 accent-forest-700"
              />
              당일 일정 (종료 일시 자동 설정)
            </label>
          </div>

          {/* 종료 일시 */}
          <div className="flex flex-col gap-1.5">
            <label className={`text-base font-medium ${endDisabled ? "text-gray-400" : "text-gray-700"}`}>
              종료 일시 {endDisabled ? "" : "*"}
            </label>
            <div className="grid grid-cols-[1fr_4rem_4rem] gap-2">
              <input
                type="date"
                value={form.endDate}
                disabled={endDisabled}
                onChange={(e) => setField("endDate", e.target.value)}
                className="w-full rounded-xl border border-gray-300 px-3 py-3 text-base text-gray-900 focus:border-forest-700 focus:outline-none focus:ring-2 focus:ring-forest-700/20 disabled:bg-gray-50 disabled:text-gray-400"
              />
              <input
                type="text"
                inputMode="numeric"
                pattern="\d*"
                maxLength={2}
                placeholder="시"
                disabled={endDisabled}
                value={form.endHour}
                onChange={digitsOnly((v) => setField("endHour", v), 23)}
                className="w-full rounded-xl border border-gray-300 px-2 py-3 text-center text-base text-gray-900 placeholder-gray-400 focus:border-forest-700 focus:outline-none focus:ring-2 focus:ring-forest-700/20 disabled:bg-gray-50 disabled:text-gray-400"
                aria-label="종료 시"
              />
              <input
                type="text"
                inputMode="numeric"
                pattern="\d*"
                maxLength={2}
                placeholder="분"
                disabled={endDisabled}
                value={form.endMinute}
                onChange={digitsOnly((v) => setField("endMinute", v), 59)}
                className="w-full rounded-xl border border-gray-300 px-2 py-3 text-center text-base text-gray-900 placeholder-gray-400 focus:border-forest-700 focus:outline-none focus:ring-2 focus:ring-forest-700/20 disabled:bg-gray-50 disabled:text-gray-400"
                aria-label="종료 분"
              />
            </div>
          </div>

          <Input
            label="정원"
            name="capacity"
            type="text"
            inputMode="numeric"
            value={form.capacity}
            onChange={digitsOnly((v) => setField("capacity", v))}
            placeholder="10"
          />

          <Input
            label="참여 포인트"
            name="pointCost"
            type="text"
            inputMode="numeric"
            value={form.pointCost}
            onChange={digitsOnly((v) => setField("pointCost", v))}
            placeholder="0"
          />

          {error && (
            <div className="rounded-xl border border-gray-200 bg-gray-50 p-4 text-base text-gray-900" role="alert">
              {error}
            </div>
          )}

          <div
            className="pb-5"
            style={{ paddingBottom: "calc(1.25rem + env(safe-area-inset-bottom))" }}
          >
            <Button type="submit" variant="primary" fullWidth loading={isPending}>
              저장하기
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

interface DeleteDialogProps {
  event: EventSummary;
  onClose: () => void;
  onConfirm: (reason: string) => void;
  isPending: boolean;
}

function DeleteDialog({ event, onClose, onConfirm, isPending }: DeleteDialogProps) {
  const [reason, setReason] = useState("");

  function handleConfirm() {
    if (!reason.trim()) return;
    onConfirm(reason.trim());
  }

  return (
    <div
      className="fixed inset-0 z-[60] flex items-end bg-black/40"
      style={{ paddingBottom: "calc(0px + env(safe-area-inset-bottom))" }}
    >
      <div
        className="w-full space-y-4 rounded-t-2xl bg-white p-6"
        style={{ paddingBottom: "calc(1.5rem + env(safe-area-inset-bottom))" }}
      >
        <h3 className="text-xl font-bold text-gray-900">이벤트를 삭제할까요?</h3>
        <p className="text-base text-gray-600">
          <span className="font-semibold">{event.title}</span> 이벤트를 삭제합니다. 이 작업은 되돌릴 수 없습니다.
        </p>
        <div className="flex flex-col gap-1.5">
          <label className="text-sm font-medium text-gray-700">삭제 사유 *</label>
          <textarea
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            rows={3}
            placeholder="삭제 사유를 입력해 주세요."
            className="w-full rounded-xl border border-gray-300 px-4 py-3 text-base text-gray-900 placeholder-gray-400 focus:border-forest-700 focus:outline-none focus:ring-2 focus:ring-forest-700/20"
          />
        </div>
        <div className="flex gap-3">
          <Button variant="secondary" fullWidth onClick={onClose}>
            취소
          </Button>
          <Button
            variant="danger"
            fullWidth
            disabled={!reason.trim()}
            loading={isPending}
            onClick={handleConfirm}
          >
            삭제하기
          </Button>
        </div>
      </div>
    </div>
  );
}

function EventRow({
  event,
  onEdit,
  onDelete,
}: {
  event: EventSummary;
  onEdit: () => void;
  onDelete: () => void;
}) {
  const starts = new Date(event.startsAt);
  const dateStr = starts.toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "short",
    day: "numeric",
  });

  return (
    <div className="rounded-2xl border border-gray-100 bg-white px-5 py-4 shadow-soft-sm">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0 flex-1 space-y-1">
          <p className="truncate text-base font-semibold tracking-tight-1 text-gray-900">
            {event.title}
          </p>
          <p className="text-sm text-gray-500">{dateStr}</p>
          <div className="flex items-center gap-2">
            <span
              className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold tracking-tight-1 ${
                STATUS_COLORS[event.status] ?? "bg-gray-100 text-gray-500"
              }`}
            >
              {STATUS_LABELS[event.status] ?? event.status}
            </span>
            <span className="text-xs text-gray-500 tabular-nums">
              {event.joinedCount ?? 0} / {event.capacity}명
            </span>
          </div>
        </div>
        <div className="flex shrink-0 items-center gap-2">
          <button
            onClick={onEdit}
            className="rounded-xl border border-gray-200 px-3 py-1.5 text-sm font-semibold text-gray-700 hover:bg-gray-50 active:bg-gray-100"
          >
            수정
          </button>
          <button
            onClick={onDelete}
            className="rounded-xl border border-red-200 bg-red-50 px-3 py-1.5 text-sm font-semibold text-red-600 hover:bg-red-100 active:bg-red-200"
          >
            삭제
          </button>
        </div>
      </div>
    </div>
  );
}

export default function MyEventsPage() {
  const [page, setPage] = useState(0);
  const { data, isLoading, isError } = useMyEvents({ page, size: 20 });

  const [editTarget, setEditTarget] = useState<EventSummary | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<EventSummary | null>(null);
  const [toast, setToast] = useState<string | null>(null);

  const updateMutation = useUpdateEvent(editTarget?.id ?? "");
  const deleteMutation = useDeleteEvent(deleteTarget?.id ?? "");

  function showToast(msg: string) {
    setToast(msg);
    setTimeout(() => setToast(null), 3000);
  }

  async function handleSave(data: UpdateEventRequest) {
    if (!editTarget) return;
    try {
      await updateMutation.mutateAsync(data);
      setEditTarget(null);
      showToast("이벤트가 수정됐어요.");
    } catch (err) {
      showToast(err instanceof Error ? err.message : "수정 중 오류가 발생했습니다.");
    }
  }

  async function handleDelete(reason: string) {
    if (!deleteTarget) return;
    try {
      await deleteMutation.mutateAsync(reason);
      setDeleteTarget(null);
      showToast("이벤트가 삭제됐어요.");
    } catch (err) {
      showToast(err instanceof Error ? err.message : "삭제 중 오류가 발생했습니다.");
    }
  }

  return (
    <UserAuthGuard allowedRoles={["TUNTUN", "MANAGER"]}>
      <TopBar title="내 이벤트 관리" showBack />

      <div className="px-4 py-5 space-y-3">
        {isLoading && (
          <div className="flex justify-center py-12">
            <span className="text-base text-gray-400">불러오는 중…</span>
          </div>
        )}

        {isError && (
          <div className="rounded-xl border border-gray-200 bg-gray-50 p-4 text-base text-gray-900">
            이벤트를 불러오지 못했습니다. 다시 시도해 주세요.
          </div>
        )}

        {data && data.content.length === 0 && !isLoading && (
          <div className="flex flex-col items-center gap-3 py-16 text-gray-400">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.5} className="h-10 w-10">
              <path strokeLinecap="round" strokeLinejoin="round" d="M2.25 13.5h3.86a2.25 2.25 0 012.012 1.244l.256.512a2.25 2.25 0 002.013 1.244h3.218a2.25 2.25 0 002.013-1.244l.256-.512a2.25 2.25 0 012.013-1.244h3.859m-19.5.338V18a2.25 2.25 0 002.25 2.25h15A2.25 2.25 0 0021.75 18v-4.162c0-.224-.034-.447-.1-.661L19.24 5.338a2.25 2.25 0 00-2.15-1.588H6.911a2.25 2.25 0 00-2.15 1.588L2.35 13.177a2.25 2.25 0 00-.1.661z" />
            </svg>
            <p className="text-base">등록한 이벤트가 없어요</p>
          </div>
        )}

        {data?.content.map((event) => (
          <EventRow
            key={event.id}
            event={event}
            onEdit={() => setEditTarget(event)}
            onDelete={() => setDeleteTarget(event)}
          />
        ))}

        {data && data.totalPages > 1 && (
          <div className="flex items-center justify-center gap-3 pt-2">
            <button
              disabled={page === 0}
              onClick={() => setPage((p) => p - 1)}
              className="h-11 rounded-xl border border-gray-300 bg-white px-5 text-base font-medium disabled:opacity-40"
            >
              이전
            </button>
            <span className="text-base text-gray-600">
              {page + 1} / {data.totalPages}
            </span>
            <button
              disabled={page >= data.totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
              className="h-11 rounded-xl border border-gray-300 bg-white px-5 text-base font-medium disabled:opacity-40"
            >
              다음
            </button>
          </div>
        )}
      </div>

      {editTarget && (
        <EditModal
          event={editTarget}
          onClose={() => setEditTarget(null)}
          onSave={handleSave}
          isPending={updateMutation.isPending}
        />
      )}

      {deleteTarget && (
        <DeleteDialog
          event={deleteTarget}
          onClose={() => setDeleteTarget(null)}
          onConfirm={handleDelete}
          isPending={deleteMutation.isPending}
        />
      )}

      {toast && (
        <div className="fixed bottom-24 left-1/2 z-50 -translate-x-1/2 rounded-xl bg-gray-900 px-5 py-3 text-base text-white shadow-lg">
          {toast}
        </div>
      )}
    </UserAuthGuard>
  );
}
