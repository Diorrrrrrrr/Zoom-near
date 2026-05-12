"use client";

import React, { useState } from "react";
import { DataTable, type Column } from "@/components/ui/data-table";
import {
  Badge,
  applicationStatusVariant,
  applicationStatusLabel,
} from "@/components/ui/badge";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { ReasonInputDialog } from "@/components/ui/reason-input-dialog";
import { useToast } from "@/components/ui/toast";
import {
  useManagerApplications,
  useApproveApplication,
  useRejectApplication,
} from "@/lib/api/manager-applications";
import type { ManagerApplication } from "@/lib/types";

export default function ManagerApplicationsPage() {
  const { toast } = useToast();

  const [statusFilter, setStatusFilter] = useState("PENDING");
  const [approveTarget, setApproveTarget] = useState<ManagerApplication | null>(null);
  const [rejectTarget, setRejectTarget] = useState<ManagerApplication | null>(null);

  const { data, isLoading, error } = useManagerApplications({
    status: statusFilter || undefined,
  });

  const approve = useApproveApplication();
  const reject = useRejectApplication();

  async function handleApprove() {
    if (!approveTarget) return;
    try {
      await approve.mutateAsync(approveTarget.id);
      toast(
        `${approveTarget.applicant.name} 님의 매니저 신청을 승인했습니다.`,
        "success"
      );
    } catch {
      toast("승인 처리 중 오류가 발생했습니다.", "error");
    } finally {
      setApproveTarget(null);
    }
  }

  async function handleReject(reason: string) {
    if (!rejectTarget) return;
    try {
      await reject.mutateAsync({ id: rejectTarget.id, reason });
      toast(
        `${rejectTarget.applicant.name} 님의 매니저 신청을 거절했습니다.`,
        "success"
      );
    } catch {
      toast("거절 처리 중 오류가 발생했습니다.", "error");
    } finally {
      setRejectTarget(null);
    }
  }

  const columns: Column<ManagerApplication>[] = [
    {
      key: "applicant",
      header: "신청자",
      render: (app) => (
        <div>
          <p className="font-medium text-gray-900">{app.applicant.name}</p>
          <p className="text-sm text-gray-500">{app.applicant.loginId}</p>
        </div>
      ),
    },
    {
      key: "reason",
      header: "신청 사유",
      render: (app) => (
        <p className="max-w-xs truncate text-sm text-gray-700">{app.reason}</p>
      ),
    },
    {
      key: "status",
      header: "상태",
      render: (app) => (
        <Badge
          variant={applicationStatusVariant(app.status)}
          label={applicationStatusLabel(app.status)}
        />
      ),
      className: "w-24",
    },
    {
      key: "createdAt",
      header: "신청일",
      render: (app) => (
        <span className="text-sm text-gray-500">
          {new Date(app.createdAt).toLocaleDateString("ko-KR")}
        </span>
      ),
      className: "w-28",
    },
    {
      key: "actions",
      header: "관리",
      render: (app) => {
        if (app.status !== "PENDING") {
          return <span className="text-sm text-gray-400">—</span>;
        }
        return (
          <div className="flex gap-2">
            <button
              onClick={(e) => {
                e.stopPropagation();
                setApproveTarget(app);
              }}
              className="rounded-lg border border-green-200 bg-green-50 px-3 py-1.5 text-sm font-medium text-green-700 hover:bg-green-100 focus:outline-none focus:ring-2 focus:ring-green-300"
              aria-label={`${app.applicant.name} 승인`}
            >
              승인
            </button>
            <button
              onClick={(e) => {
                e.stopPropagation();
                setRejectTarget(app);
              }}
              className="rounded-lg border border-red-200 bg-red-50 px-3 py-1.5 text-sm font-medium text-red-700 hover:bg-red-100 focus:outline-none focus:ring-2 focus:ring-red-300"
              aria-label={`${app.applicant.name} 거절`}
            >
              거절
            </button>
          </div>
        );
      },
      className: "w-36",
    },
  ];

  return (
    <div>
      <h1 className="mb-6 text-2xl font-bold text-gray-900">매니저 신청 관리</h1>

      {/* 필터 */}
      <div className="mb-4 flex flex-wrap items-end gap-3">
        <div>
          <label htmlFor="status-filter" className="mb-1 block text-sm font-medium text-gray-600">
            상태
          </label>
          <select
            id="status-filter"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="rounded-lg border border-gray-300 bg-white px-3 py-2 text-base focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/30"
          >
            <option value="">전체</option>
            <option value="PENDING">대기</option>
            <option value="APPROVED">승인</option>
            <option value="REJECTED">거절</option>
          </select>
        </div>

        {data && (
          <p className="ml-auto self-end text-sm text-gray-500">
            총 <strong>{data.length.toLocaleString()}</strong>건
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
        data={data ?? []}
        keyExtractor={(app) => app.id}
        isLoading={isLoading}
        emptyMessage="매니저 신청이 없습니다."
      />

      {/* 승인 확인 */}
      <ConfirmDialog
        open={!!approveTarget}
        title={`매니저 신청 승인`}
        description={
          approveTarget
            ? `${approveTarget.applicant.name} 님의 매니저 신청을 승인하시겠습니까?`
            : ""
        }
        confirmLabel="승인"
        onConfirm={handleApprove}
        onCancel={() => setApproveTarget(null)}
      />

      {/* 거절 사유 입력 */}
      <ReasonInputDialog
        open={!!rejectTarget}
        title={`매니저 신청 거절`}
        description={
          rejectTarget
            ? `${rejectTarget.applicant.name} 님의 신청을 거절합니다. 사유를 입력해 주세요.`
            : ""
        }
        placeholder="예: 신청 사유 불충분"
        confirmLabel="거절"
        destructive
        onConfirm={handleReject}
        onCancel={() => setRejectTarget(null)}
      />
    </div>
  );
}
