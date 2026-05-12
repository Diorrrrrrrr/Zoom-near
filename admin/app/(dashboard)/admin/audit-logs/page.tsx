"use client";

import React, { useState } from "react";
import { DataTable, Pagination, type Column } from "@/components/ui/data-table";
import { Badge, auditStatusVariant } from "@/components/ui/badge";
import { useAuditLogs } from "@/lib/api/audit-logs";
import type { AuditLog } from "@/lib/types";

const PAGE_SIZE = 30;

function PayloadModal({
  log,
  onClose,
}: {
  log: AuditLog;
  onClose: () => void;
}) {
  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="payload-modal-title"
      className="fixed inset-0 z-50 flex items-center justify-center"
    >
      <div
        className="absolute inset-0 bg-black/40"
        onClick={onClose}
        aria-hidden="true"
      />
      <div className="relative z-10 w-full max-w-2xl rounded-2xl bg-white p-8 shadow-xl">
        <div className="mb-4 flex items-center justify-between">
          <h2 id="payload-modal-title" className="text-lg font-bold text-gray-900">
            감사 로그 상세 — #{log.id}
          </h2>
          <button
            onClick={onClose}
            aria-label="닫기"
            className="rounded-lg p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
          >
            ✕
          </button>
        </div>
        <dl className="mb-4 grid grid-cols-2 gap-x-6 gap-y-2 text-sm">
          <div>
            <dt className="font-medium text-gray-500">액션</dt>
            <dd className="font-mono text-gray-900">{log.action}</dd>
          </div>
          <div>
            <dt className="font-medium text-gray-500">대상</dt>
            <dd className="text-gray-900">
              {log.targetType} #{log.targetId}
            </dd>
          </div>
          <div>
            <dt className="font-medium text-gray-500">처리자 ID</dt>
            <dd className="text-gray-900">{log.actorId}</dd>
          </div>
          <div>
            <dt className="font-medium text-gray-500">일시</dt>
            <dd className="text-gray-900">
              {new Date(log.createdAt).toLocaleString("ko-KR")}
            </dd>
          </div>
        </dl>
        <div>
          <p className="mb-1.5 text-sm font-medium text-gray-500">Payload</p>
          <pre className="max-h-80 overflow-auto rounded-lg bg-gray-900 p-4 text-sm text-green-400">
            {log.payload
              ? JSON.stringify(log.payload, null, 2)
              : "(payload 없음)"}
          </pre>
        </div>
      </div>
    </div>
  );
}

export default function AuditLogsPage() {
  const [actionFilter, setActionFilter] = useState("");
  const [actorIdFilter, setActorIdFilter] = useState("");
  const [actorIdInput, setActorIdInput] = useState("");
  const [page, setPage] = useState(0);
  const [selectedLog, setSelectedLog] = useState<AuditLog | null>(null);

  const { data, isLoading, error } = useAuditLogs({
    page,
    size: PAGE_SIZE,
    action: actionFilter || undefined,
    actorId: actorIdFilter || undefined,
  });

  function handleActorIdSearch(e: React.FormEvent) {
    e.preventDefault();
    setActorIdFilter(actorIdInput);
    setPage(0);
  }

  const columns: Column<AuditLog>[] = [
    {
      key: "createdAt",
      header: "일시",
      render: (l) => (
        <span className="whitespace-nowrap text-sm text-gray-500">
          {new Date(l.createdAt).toLocaleString("ko-KR")}
        </span>
      ),
    },
    {
      key: "actorId",
      header: "처리자 ID",
      render: (l) => <span className="font-mono text-sm">{l.actorId}</span>,
      className: "w-24",
    },
    {
      key: "action",
      header: "액션",
      render: (l) => (
        <span className="font-mono text-sm font-medium text-gray-800">
          {l.action}
        </span>
      ),
    },
    {
      key: "target",
      header: "대상",
      render: (l) => (
        <span className="text-sm text-gray-600">
          {l.targetType}
          {l.targetId != null ? ` #${l.targetId}` : ""}
        </span>
      ),
    },
    {
      key: "status",
      header: "결과",
      render: (l) => (
        <Badge
          variant={auditStatusVariant(l.status)}
          label={l.status === "SUCCESS" ? "성공" : "실패"}
        />
      ),
      className: "w-20",
    },
    {
      key: "payload",
      header: "Payload 미리보기",
      render: (l) => (
        <span className="max-w-xs truncate font-mono text-xs text-gray-400">
          {l.payload ? JSON.stringify(l.payload).slice(0, 60) + "…" : "—"}
        </span>
      ),
    },
  ];

  return (
    <div>
      <h1 className="mb-6 text-2xl font-bold text-gray-900">감사 로그</h1>

      {/* 필터 */}
      <div className="mb-4 flex flex-wrap items-end gap-3">
        <div>
          <label htmlFor="action-filter" className="mb-1 block text-sm font-medium text-gray-600">
            액션
          </label>
          <input
            id="action-filter"
            type="text"
            value={actionFilter}
            onChange={(e) => { setActionFilter(e.target.value); setPage(0); }}
            placeholder="예: USER_SUSPEND"
            className="rounded-lg border border-gray-300 px-3 py-2 text-base focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/30"
          />
        </div>

        <form onSubmit={handleActorIdSearch} className="flex items-end gap-2">
          <div>
            <label htmlFor="actor-id" className="mb-1 block text-sm font-medium text-gray-600">
              처리자 ID
            </label>
            <input
              id="actor-id"
              type="text"
              value={actorIdInput}
              onChange={(e) => setActorIdInput(e.target.value)}
              placeholder="사용자 ID"
              className="rounded-lg border border-gray-300 px-3 py-2 text-base focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/30"
            />
          </div>
          <button
            type="submit"
            className="rounded-lg bg-brand-500 px-4 py-2 text-base font-medium text-white hover:bg-blue-600 focus:outline-none focus:ring-2 focus:ring-brand-500/50"
          >
            검색
          </button>
        </form>

        {data && (
          <p className="ml-auto self-end text-sm text-gray-500">
            총 <strong>{data.totalElements.toLocaleString()}</strong>건
          </p>
        )}
      </div>

      {error && (
        <div role="alert" className="mb-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-base text-red-700">
          <span aria-hidden="true" className="mr-1.5">⚠</span>
          데이터를 불러오지 못했습니다: {error.message}
        </div>
      )}

      <p className="mb-2 text-sm text-gray-400">행을 클릭하면 payload 상세를 볼 수 있습니다.</p>

      <DataTable
        columns={columns}
        data={data?.content ?? []}
        keyExtractor={(l) => l.id}
        onRowClick={(l) => setSelectedLog(l)}
        isLoading={isLoading}
        emptyMessage="감사 로그가 없습니다."
      />

      <Pagination
        page={page}
        totalPages={data?.totalPages ?? 0}
        onPageChange={setPage}
      />

      {selectedLog && (
        <PayloadModal log={selectedLog} onClose={() => setSelectedLog(null)} />
      )}
    </div>
  );
}
