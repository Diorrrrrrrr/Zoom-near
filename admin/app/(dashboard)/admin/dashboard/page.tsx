"use client";

import React from "react";
import Link from "next/link";
import { useUsers } from "@/lib/api/users";
import { useEvents } from "@/lib/api/events";
import { useManagerApplications } from "@/lib/api/manager-applications";
import { useAuditLogs } from "@/lib/api/audit-logs";

function StatCard({
  title,
  value,
  href,
  color,
  icon,
}: {
  title: string;
  value: string | number | undefined;
  href: string;
  color: string;
  icon: string;
}) {
  return (
    <Link
      href={href}
      className={`flex flex-col gap-3 rounded-2xl border bg-white p-6 shadow-sm transition-shadow hover:shadow-md ${color}`}
    >
      <div className="flex items-center justify-between">
        <span className="text-base font-medium text-gray-600">{title}</span>
        <span className="text-2xl" aria-hidden="true">
          {icon}
        </span>
      </div>
      <p className="text-4xl font-bold text-gray-900">
        {value === undefined ? (
          <span className="animate-pulse text-gray-300">—</span>
        ) : (
          value.toLocaleString()
        )}
      </p>
    </Link>
  );
}

export default function DashboardPage() {
  const { data: usersData } = useUsers({ size: 1 });
  const { data: openEvents } = useEvents({ status: "OPEN", size: 1 });
  const { data: pendingApps } = useManagerApplications({ status: "PENDING" });
  const { data: recentLogs } = useAuditLogs({ size: 1 });

  return (
    <div>
      <h1 className="mb-8 text-2xl font-bold text-gray-900">대시보드</h1>

      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          title="총 회원 수"
          value={usersData?.totalElements}
          href="/admin/users"
          color="border-blue-100"
          icon="👤"
        />
        <StatCard
          title="모집 중 이벤트"
          value={openEvents?.totalElements}
          href="/admin/events?status=OPEN"
          color="border-green-100"
          icon="📅"
        />
        <StatCard
          title="대기 중 매니저 신청"
          value={pendingApps?.length}
          href="/admin/manager-applications?status=PENDING"
          color="border-yellow-100"
          icon="📋"
        />
        <StatCard
          title="전체 감사 로그"
          value={recentLogs?.totalElements}
          href="/admin/audit-logs"
          color="border-purple-100"
          icon="🔍"
        />
      </div>

      <div className="mt-10 rounded-2xl border border-dashed border-gray-300 bg-white p-8 text-center text-base text-gray-400">
        각 카드를 클릭하면 해당 관리 페이지로 이동합니다.
      </div>
    </div>
  );
}
