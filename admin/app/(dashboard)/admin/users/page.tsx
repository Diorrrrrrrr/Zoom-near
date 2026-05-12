"use client";

import React, { useState } from "react";
import { DataTable, Pagination, type Column } from "@/components/ui/data-table";
import {
  Badge,
  userStatusVariant,
  userStatusLabel,
  roleVariant,
  roleLabel,
} from "@/components/ui/badge";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { ReasonInputDialog } from "@/components/ui/reason-input-dialog";
import { useToast } from "@/components/ui/toast";
import {
  useUsers,
  useSuspendUser,
  useActivateUser,
  useChangeUserRole,
  type ChangeableRole,
} from "@/lib/api/users";
import type { User } from "@/lib/types";

const PAGE_SIZE = 20;

/// 권한 변경은 튼튼↔든든 전환만 허용. 매니저 승격은 매니저 신청 승인 경로로 처리.
const ROLE_OPTIONS: { value: ChangeableRole; label: string }[] = [
  { value: "TUNTUN", label: "튼튼이" },
  { value: "DUNDUN", label: "든든이" },
];

export default function UsersPage() {
  const { toast } = useToast();

  // 필터 상태
  const [statusFilter, setStatusFilter] = useState("");
  const [roleFilter, setRoleFilter] = useState("");
  const [q, setQ] = useState("");
  const [qInput, setQInput] = useState("");
  const [page, setPage] = useState(0);

  // 액션 상태
  const [suspendTarget, setSuspendTarget] = useState<User | null>(null);
  const [activateTarget, setActivateTarget] = useState<User | null>(null);
  const [roleTarget, setRoleTarget] = useState<User | null>(null);

  const { data, isLoading, error } = useUsers({
    status: statusFilter || undefined,
    q: q || undefined,
    page,
    size: PAGE_SIZE,
  });

  const suspendUser = useSuspendUser();
  const activateUser = useActivateUser();
  const changeRole = useChangeUserRole();

  function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    setQ(qInput);
    setPage(0);
  }

  async function handleSuspend(reason: string) {
    if (!suspendTarget) return;
    try {
      await suspendUser.mutateAsync({ id: suspendTarget.id, reason });
      toast(`${suspendTarget.name} 님을 정지했습니다.`, "success");
    } catch {
      toast("정지 처리 중 오류가 발생했습니다.", "error");
    } finally {
      setSuspendTarget(null);
    }
  }

  async function handleActivate() {
    if (!activateTarget) return;
    try {
      await activateUser.mutateAsync(activateTarget.id);
      toast(`${activateTarget.name} 님을 활성화했습니다.`, "success");
    } catch {
      toast("활성화 처리 중 오류가 발생했습니다.", "error");
    } finally {
      setActivateTarget(null);
    }
  }

  async function handleChangeRole(next: ChangeableRole) {
    if (!roleTarget) return;
    try {
      await changeRole.mutateAsync({ id: roleTarget.id, role: next });
      toast(
        `${roleTarget.name} 님의 역할을 ${
          ROLE_OPTIONS.find((r) => r.value === next)?.label ?? next
        }로 변경했습니다.`,
        "success",
      );
    } catch (err) {
      const msg = err instanceof Error ? err.message : "역할 변경 실패";
      toast(`역할 변경 실패: ${msg}`, "error");
    } finally {
      setRoleTarget(null);
    }
  }

  const columns: Column<User>[] = [
    {
      key: "loginId",
      header: "로그인 ID",
      render: (u) => <span className="font-mono text-sm">{u.loginId}</span>,
    },
    {
      key: "name",
      header: "이름",
      render: (u) => <span className="font-medium">{u.name}</span>,
    },
    {
      key: "role",
      header: "역할",
      render: (u) => (
        <div className="flex items-center gap-2">
          <Badge variant={roleVariant(u.role)} label={roleLabel(u.role)} />
          {(u.role === "TUNTUN" || u.role === "DUNDUN") &&
            u.status !== "WITHDRAWN" && (
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  setRoleTarget(u);
                }}
                className="rounded-lg border border-gray-200 px-2 py-1 text-xs font-medium text-gray-600 hover:bg-gray-50"
              >
                변경
              </button>
            )}
        </div>
      ),
    },
    {
      key: "status",
      header: "상태",
      render: (u) => (
        <Badge
          variant={userStatusVariant(u.status)}
          label={userStatusLabel(u.status)}
        />
      ),
    },
    {
      key: "uniqueCode",
      header: "고유 코드",
      render: (u) => (
        <span className="font-mono text-sm text-gray-500">{u.uniqueCode}</span>
      ),
    },
    {
      key: "createdAt",
      header: "가입일",
      render: (u) => (
        <span className="text-sm text-gray-500">
          {new Date(u.createdAt).toLocaleDateString("ko-KR")}
        </span>
      ),
    },
    {
      key: "actions",
      header: "관리",
      render: (u) => {
        if (u.status === "WITHDRAWN")
          return <span className="text-sm text-gray-400">—</span>;
        return u.status === "ACTIVE" ? (
          <button
            onClick={(e) => {
              e.stopPropagation();
              setSuspendTarget(u);
            }}
            className="rounded-lg border border-red-200 bg-red-50 px-3 py-1.5 text-sm font-medium text-red-700 hover:bg-red-100 focus:outline-none focus:ring-2 focus:ring-red-300"
            aria-label={`${u.name} 정지`}
          >
            정지
          </button>
        ) : (
          <button
            onClick={(e) => {
              e.stopPropagation();
              setActivateTarget(u);
            }}
            className="rounded-lg border border-green-200 bg-green-50 px-3 py-1.5 text-sm font-medium text-green-700 hover:bg-green-100 focus:outline-none focus:ring-2 focus:ring-green-300"
            aria-label={`${u.name} 활성화`}
          >
            활성화
          </button>
        );
      },
      className: "w-24",
    },
  ];

  return (
    <div>
      <h1 className="mb-6 text-2xl font-bold text-gray-900">회원 관리</h1>

      {/* 필터 */}
      <div className="mb-4 flex flex-wrap items-end gap-3">
        <div>
          <label
            htmlFor="status-filter"
            className="mb-1 block text-sm font-medium text-gray-600"
          >
            상태
          </label>
          <select
            id="status-filter"
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setPage(0);
            }}
            className="rounded-lg border border-gray-300 bg-white px-3 py-2 text-base focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/30"
          >
            <option value="">전체</option>
            <option value="ACTIVE">활성</option>
            <option value="SUSPENDED">정지</option>
            <option value="WITHDRAWN">탈퇴</option>
          </select>
        </div>

        <div>
          <label
            htmlFor="role-filter"
            className="mb-1 block text-sm font-medium text-gray-600"
          >
            역할
          </label>
          <select
            id="role-filter"
            value={roleFilter}
            onChange={(e) => {
              setRoleFilter(e.target.value);
              setPage(0);
            }}
            className="rounded-lg border border-gray-300 bg-white px-3 py-2 text-base focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/30"
          >
            <option value="">전체</option>
            <option value="TUNTUN">튼튼이</option>
            <option value="DUNDUN">든든이</option>
            <option value="MANAGER">매니저</option>
            <option value="ADMIN">관리자</option>
          </select>
        </div>

        <form onSubmit={handleSearch} className="flex items-end gap-2">
          <div>
            <label
              htmlFor="q-input"
              className="mb-1 block text-sm font-medium text-gray-600"
            >
              검색
            </label>
            <input
              id="q-input"
              type="text"
              value={qInput}
              onChange={(e) => setQInput(e.target.value)}
              placeholder="이름 또는 로그인 ID"
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
            총 <strong>{data.totalElements.toLocaleString()}</strong>명
          </p>
        )}
      </div>

      {error && (
        <div
          role="alert"
          className="mb-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-base text-red-700"
        >
          데이터를 불러오지 못했습니다: {error.message}
        </div>
      )}

      <DataTable
        columns={columns}
        data={data?.content ?? []}
        keyExtractor={(u) => u.id}
        isLoading={isLoading}
        emptyMessage="회원이 없습니다."
      />

      <Pagination
        page={page}
        totalPages={data?.totalPages ?? 0}
        onPageChange={setPage}
      />

      {/* 정지 사유 입력 다이얼로그 */}
      <ReasonInputDialog
        open={!!suspendTarget}
        title={`${suspendTarget?.name} 님 정지`}
        description="정지 사유를 입력해 주세요. 사용자에게 표시될 수 있습니다."
        placeholder="예: 운영 정책 위반으로 인한 계정 정지"
        confirmLabel="정지 처리"
        destructive
        onConfirm={handleSuspend}
        onCancel={() => setSuspendTarget(null)}
      />

      {/* 활성화 확인 다이얼로그 */}
      <ConfirmDialog
        open={!!activateTarget}
        title={`${activateTarget?.name} 님 활성화`}
        description="계정을 다시 활성화하시겠습니까?"
        confirmLabel="활성화"
        onConfirm={handleActivate}
        onCancel={() => setActivateTarget(null)}
      />

      {/* 역할 변경 다이얼로그 */}
      {roleTarget && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
          onClick={() => setRoleTarget(null)}
        >
          <div
            className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-xl"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 className="text-lg font-bold text-gray-900">
              {roleTarget.name} 님 역할 변경
            </h3>
            <p className="mt-1 text-sm text-gray-500">
              현재 역할: {roleLabel(roleTarget.role)}
            </p>
            <p className="mt-3 text-sm text-gray-600">
              튼튼이↔든든이 전환만 지원합니다. 매니저 승격은 매니저 신청
              승인을 통해 처리하세요.
            </p>
            <div className="mt-4 space-y-2">
              {ROLE_OPTIONS.map((opt) => {
                const isCurrent = opt.value === roleTarget.role;
                return (
                  <button
                    key={opt.value}
                    disabled={isCurrent || changeRole.isPending}
                    onClick={() => handleChangeRole(opt.value)}
                    className={`flex w-full items-center justify-between rounded-lg border px-4 py-3 text-base font-medium transition-colors ${
                      isCurrent
                        ? "cursor-not-allowed border-gray-200 bg-gray-50 text-gray-400"
                        : "border-gray-300 bg-white text-gray-800 hover:border-brand-500 hover:bg-brand-50 hover:text-brand-500"
                    }`}
                  >
                    <span>{opt.label}</span>
                    {isCurrent && (
                      <span className="text-xs font-normal">현재</span>
                    )}
                  </button>
                );
              })}
            </div>
            <button
              onClick={() => setRoleTarget(null)}
              className="mt-4 w-full rounded-lg border border-gray-300 px-4 py-2 text-base font-medium text-gray-600 hover:bg-gray-50"
            >
              취소
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
