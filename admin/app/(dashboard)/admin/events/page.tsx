"use client";

import React, { useState } from "react";
import { DataTable, Pagination, type Column } from "@/components/ui/data-table";
import { Badge, eventStatusVariant, eventStatusLabel } from "@/components/ui/badge";
import { ReasonInputDialog } from "@/components/ui/reason-input-dialog";
import { useToast } from "@/components/ui/toast";
import {
  useEvents,
  useForceCloseEvent,
  useAdminUpdateEvent,
  useAdminDeleteEvent,
} from "@/lib/api/events";
import type { SocialEvent } from "@/lib/types";
import type { UpdateEventRequest } from "@/lib/api/user-events";

const PAGE_SIZE = 20;

const CATEGORIES = ["문화", "스포츠", "봉사", "교육", "기타"];

interface EditForm {
  title: string;
  description: string;
  regionText: string;
  category: string;
  startsAt: string;
  endsAt: string;
  capacity: string;
  pointCost: string;
  reason: string;
}

function toDatetimeLocal(iso: string): string {
  if (!iso) return "";
  try {
    const d = new Date(iso);
    const pad = (n: number) => String(n).padStart(2, "0");
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  } catch {
    return "";
  }
}

/// 긴 UUID 를 앞 8자 + 말줄임으로 줄여 보여주고, 클릭 한 번에 클립보드로 복사한다.
function IdCell({ id }: { id: string }) {
  const [copied, setCopied] = useState(false);
  const shortId = id.length > 8 ? `${id.slice(0, 8)}…` : id;

  async function handleCopy(e: React.MouseEvent) {
    e.stopPropagation();
    try {
      await navigator.clipboard.writeText(id);
      setCopied(true);
      setTimeout(() => setCopied(false), 1200);
    } catch {
      // 브라우저가 거부하면 무시 (HTTPS 가 아닌 경우 등)
    }
  }

  return (
    <span className="inline-flex items-center gap-1.5 whitespace-nowrap">
      <span className="font-mono text-xs text-gray-500" title={id}>
        {shortId}
      </span>
      <button
        type="button"
        onClick={handleCopy}
        aria-label="ID 복사"
        className="rounded border border-gray-200 px-1.5 py-0.5 text-[11px] font-medium text-gray-500 hover:bg-gray-50 hover:text-gray-700"
      >
        {copied ? "복사됨" : "복사"}
      </button>
    </span>
  );
}

interface EditModalProps {
  event: SocialEvent;
  onClose: () => void;
  onSave: (form: EditForm) => void;
  isPending: boolean;
}

function EditModal({ event, onClose, onSave, isPending }: EditModalProps) {
  const [form, setForm] = useState<EditForm>({
    title: event.title,
    description: "",
    regionText: event.regionText,
    category: "",
    startsAt: toDatetimeLocal(event.startsAt),
    endsAt: "",
    capacity: String(event.capacity),
    pointCost: String(event.pointCost),
    reason: "",
  });
  const [err, setErr] = useState<string | null>(null);

  function setField<K extends keyof EditForm>(k: K, v: EditForm[K]) {
    setForm((p) => ({ ...p, [k]: v }));
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!form.reason.trim()) {
      setErr("수정 사유를 입력해 주세요.");
      return;
    }
    setErr(null);
    onSave(form);
  }

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="이벤트 수정"
      className="fixed inset-0 z-50 flex items-center justify-center"
    >
      <div
        className="absolute inset-0 bg-black/40"
        onClick={onClose}
        aria-hidden="true"
      />
      <div className="relative z-10 w-full max-w-lg rounded-2xl bg-white p-8 shadow-xl overflow-y-auto max-h-[90vh]">
        <h2 className="mb-4 text-xl font-bold text-gray-900">이벤트 수정</h2>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="flex flex-col gap-1">
            <label className="text-sm font-medium text-gray-700">제목</label>
            <input
              value={form.title}
              onChange={(e) => setField("title", e.target.value)}
              className="rounded-lg border border-gray-300 px-3 py-2 text-base focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/30"
            />
          </div>
          <div className="flex flex-col gap-1">
            <label className="text-sm font-medium text-gray-700">설명</label>
            <textarea
              value={form.description}
              onChange={(e) => setField("description", e.target.value)}
              rows={3}
              className="rounded-lg border border-gray-300 px-3 py-2 text-base focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/30"
            />
          </div>
          <div className="flex flex-col gap-1">
            <label className="text-sm font-medium text-gray-700">지역</label>
            <input
              value={form.regionText}
              onChange={(e) => setField("regionText", e.target.value)}
              className="rounded-lg border border-gray-300 px-3 py-2 text-base focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/30"
            />
          </div>
          <div className="flex flex-col gap-1">
            <label className="text-sm font-medium text-gray-700">카테고리</label>
            <select
              value={form.category}
              onChange={(e) => setField("category", e.target.value)}
              className="rounded-lg border border-gray-300 px-3 py-2 text-base focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/30"
            >
              <option value="">(변경 안 함)</option>
              {CATEGORIES.map((c) => (
                <option key={c} value={c}>{c}</option>
              ))}
            </select>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div className="flex flex-col gap-1">
              <label className="text-sm font-medium text-gray-700">시작 일시</label>
              <input
                type="datetime-local"
                value={form.startsAt}
                onChange={(e) => setField("startsAt", e.target.value)}
                className="rounded-lg border border-gray-300 px-3 py-2 text-base focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/30"
              />
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-sm font-medium text-gray-700">종료 일시</label>
              <input
                type="datetime-local"
                value={form.endsAt}
                onChange={(e) => setField("endsAt", e.target.value)}
                className="rounded-lg border border-gray-300 px-3 py-2 text-base focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/30"
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div className="flex flex-col gap-1">
              <label className="text-sm font-medium text-gray-700">정원</label>
              <input
                type="number"
                min={1}
                value={form.capacity}
                onChange={(e) => setField("capacity", e.target.value)}
                className="rounded-lg border border-gray-300 px-3 py-2 text-base focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/30"
              />
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-sm font-medium text-gray-700">포인트</label>
              <input
                type="number"
                min={0}
                value={form.pointCost}
                onChange={(e) => setField("pointCost", e.target.value)}
                className="rounded-lg border border-gray-300 px-3 py-2 text-base focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/30"
              />
            </div>
          </div>
          <div className="flex flex-col gap-1">
            <label className="text-sm font-medium text-gray-700">수정 사유 *</label>
            <textarea
              value={form.reason}
              onChange={(e) => setField("reason", e.target.value)}
              rows={2}
              placeholder="수정 사유를 입력해 주세요."
              className="rounded-lg border border-gray-300 px-3 py-2 text-base focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/30"
            />
          </div>
          {err && (
            <p className="text-sm text-red-600">{err}</p>
          )}
          <div className="flex justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg border border-gray-300 bg-white px-5 py-2.5 text-base font-medium text-gray-700 hover:bg-gray-50"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={isPending}
              className="rounded-lg bg-brand-500 px-5 py-2.5 text-base font-medium text-white hover:bg-brand-600 disabled:opacity-40"
            >
              {isPending ? "저장 중…" : "저장"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function EventsPage() {
  const { toast } = useToast();

  const [statusFilter, setStatusFilter] = useState("");
  const [page, setPage] = useState(0);
  const [closeTarget, setCloseTarget] = useState<SocialEvent | null>(null);
  const [editTarget, setEditTarget] = useState<SocialEvent | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<SocialEvent | null>(null);

  const { data, isLoading, error } = useEvents({
    status: statusFilter || undefined,
    page,
    size: PAGE_SIZE,
  });

  const forceClose = useForceCloseEvent();
  const adminUpdate = useAdminUpdateEvent();
  const adminDelete = useAdminDeleteEvent();

  async function handleForceClose(reason: string) {
    if (!closeTarget) return;
    try {
      await forceClose.mutateAsync({ id: closeTarget.id, reason });
      toast(`이벤트 "${closeTarget.title}" 를 강제 종료했습니다.`, "success");
    } catch {
      toast("강제 종료 중 오류가 발생했습니다.", "error");
    } finally {
      setCloseTarget(null);
    }
  }

  async function handleEdit(form: EditForm) {
    if (!editTarget) return;
    try {
      const body: UpdateEventRequest = {};
      if (form.title) body.title = form.title;
      if (form.description) body.description = form.description;
      if (form.regionText) body.regionText = form.regionText;
      if (form.category) body.category = form.category;
      if (form.startsAt) body.startsAt = new Date(form.startsAt).toISOString();
      if (form.endsAt) body.endsAt = new Date(form.endsAt).toISOString();
      if (form.capacity) body.capacity = Math.max(1, Number(form.capacity));
      if (form.pointCost !== "") body.pointCost = Math.max(0, Number(form.pointCost));
      await adminUpdate.mutateAsync({ id: editTarget.id, event: body, reason: form.reason });
      toast(`이벤트 "${editTarget.title}" 를 수정했습니다.`, "success");
    } catch {
      toast("수정 중 오류가 발생했습니다.", "error");
    } finally {
      setEditTarget(null);
    }
  }

  async function handleDelete(reason: string) {
    if (!deleteTarget) return;
    try {
      await adminDelete.mutateAsync({ id: deleteTarget.id, reason });
      toast(`이벤트 "${deleteTarget.title}" 를 삭제했습니다.`, "success");
    } catch {
      toast("삭제 중 오류가 발생했습니다.", "error");
    } finally {
      setDeleteTarget(null);
    }
  }

  const columns: Column<SocialEvent>[] = [
    {
      key: "id",
      header: "ID",
      render: (e) => <IdCell id={String(e.id)} />,
      className: "w-32 whitespace-nowrap",
    },
    {
      key: "managerProgram",
      header: "구분",
      render: (e) =>
        e.managerProgram ? (
          <span className="inline-flex items-center rounded-full border border-emerald-200 bg-emerald-100 px-2.5 py-0.5 text-xs font-medium text-emerald-700 whitespace-nowrap">
            주니어 자체 프로그램
          </span>
        ) : (
          <span className="inline-flex items-center rounded-full border border-gray-200 bg-gray-100 px-2.5 py-0.5 text-xs font-medium text-gray-600 whitespace-nowrap">
            사용자 등록
          </span>
        ),
      className: "w-36 whitespace-nowrap",
    },
    {
      key: "title",
      header: "제목",
      render: (e) => <span className="font-medium whitespace-nowrap">{e.title}</span>,
      className: "whitespace-nowrap",
    },
    {
      key: "regionText",
      header: "지역",
      render: (e) => (
        <span className="text-sm text-gray-600 whitespace-nowrap">{e.regionText}</span>
      ),
      className: "whitespace-nowrap",
    },
    {
      key: "status",
      header: "상태",
      render: (e) => (
        <Badge
          variant={eventStatusVariant(e.status)}
          label={eventStatusLabel(e.status)}
        />
      ),
      className: "whitespace-nowrap",
    },
    {
      key: "capacity",
      header: "정원",
      render: (e) => (
        <span className="text-sm tabular-nums whitespace-nowrap">
          {e.joinedCount ?? 0} / {e.capacity}
        </span>
      ),
      className: "whitespace-nowrap",
    },
    {
      key: "pointCost",
      header: "포인트",
      render: (e) => (
        <span className="text-sm text-gray-600 tabular-nums whitespace-nowrap">
          {e.pointCost.toLocaleString()}P
        </span>
      ),
      className: "whitespace-nowrap",
    },
    {
      key: "startsAt",
      header: "시작일",
      render: (e) => (
        <span className="text-sm text-gray-500 whitespace-nowrap">
          {new Date(e.startsAt).toLocaleDateString("ko-KR")}
        </span>
      ),
      className: "whitespace-nowrap",
    },
    {
      key: "actions",
      header: "관리",
      render: (e) => (
        <div className="inline-flex flex-nowrap items-center gap-1.5 whitespace-nowrap">
          <button
            onClick={(ev) => { ev.stopPropagation(); setEditTarget(e); }}
            className="shrink-0 whitespace-nowrap rounded-lg border border-gray-200 bg-white px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-gray-300"
            aria-label={`이벤트 "${e.title}" 수정`}
          >
            수정
          </button>
          <button
            onClick={(ev) => { ev.stopPropagation(); setDeleteTarget(e); }}
            className="shrink-0 whitespace-nowrap rounded-lg border border-red-200 bg-red-50 px-3 py-1.5 text-sm font-medium text-red-700 hover:bg-red-100 focus:outline-none focus:ring-2 focus:ring-red-300"
            aria-label={`이벤트 "${e.title}" 삭제`}
          >
            삭제
          </button>
          {e.status === "OPEN" && (
            <button
              onClick={(ev) => { ev.stopPropagation(); setCloseTarget(e); }}
              className="shrink-0 whitespace-nowrap rounded-lg border border-red-200 bg-red-50 px-3 py-1.5 text-sm font-medium text-red-700 hover:bg-red-100 focus:outline-none focus:ring-2 focus:ring-red-300"
              aria-label={`이벤트 "${e.title}" 강제 종료`}
            >
              강제 종료
            </button>
          )}
        </div>
      ),
      className: "whitespace-nowrap",
    },
  ];

  return (
    <div>
      <h1 className="mb-6 text-2xl font-bold text-gray-900">이벤트 관리</h1>

      {/* 필터 */}
      <div className="mb-4 flex flex-wrap items-end gap-3">
        <div>
          <label htmlFor="status-filter" className="mb-1 block text-sm font-medium text-gray-600">
            상태
          </label>
          <select
            id="status-filter"
            value={statusFilter}
            onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
            className="rounded-lg border border-gray-300 bg-white px-3 py-2 text-base focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/30"
          >
            <option value="">전체</option>
            <option value="OPEN">모집중</option>
            <option value="CLOSED">종료</option>
            <option value="CANCELLED">취소</option>
          </select>
        </div>

        {data && (
          <p className="ml-auto self-end text-sm text-gray-500">
            총 <strong>{data.totalElements.toLocaleString()}</strong>개
          </p>
        )}
      </div>

      {error && (
        <div role="alert" className="mb-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-base text-red-700">
          <span aria-hidden="true" className="mr-1.5">⚠</span>
          데이터를 불러오지 못했습니다: {error.message}
        </div>
      )}

      <DataTable
        columns={columns}
        data={data?.content ?? []}
        keyExtractor={(e) => e.id}
        isLoading={isLoading}
        emptyMessage="이벤트가 없습니다."
      />

      <Pagination
        page={page}
        totalPages={data?.totalPages ?? 0}
        onPageChange={setPage}
      />

      {/* 강제 종료 다이얼로그 */}
      <ReasonInputDialog
        open={!!closeTarget}
        title="이벤트 강제 종료"
        description={closeTarget ? `"${closeTarget.title}" 이벤트를 강제 종료합니다.` : ""}
        placeholder="예: 운영 정책 위반으로 인한 강제 종료"
        confirmLabel="강제 종료"
        destructive
        onConfirm={handleForceClose}
        onCancel={() => setCloseTarget(null)}
      />

      {/* 삭제 다이얼로그 */}
      <ReasonInputDialog
        open={!!deleteTarget}
        title="이벤트 삭제"
        description={deleteTarget ? `"${deleteTarget.title}" 이벤트를 삭제합니다.` : ""}
        placeholder="삭제 사유를 입력해 주세요."
        confirmLabel="삭제"
        destructive
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />

      {/* 수정 모달 */}
      {editTarget && (
        <EditModal
          event={editTarget}
          onClose={() => setEditTarget(null)}
          onSave={handleEdit}
          isPending={adminUpdate.isPending}
        />
      )}
    </div>
  );
}
