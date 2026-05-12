"use client";

import React from "react";
import Link from "next/link";
import { useUsers } from "@/lib/api/users";
import { useEvents } from "@/lib/api/events";

function StatCard({
  title,
  value,
  href,
  color,
}: {
  title: string;
  value: string | number | undefined;
  href: string;
  color: string;
}) {
  return (
    <Link
      href={href}
      className={`flex flex-col gap-3 rounded-2xl border bg-white p-6 shadow-sm transition-shadow hover:shadow-md ${color}`}
    >
      <div className="flex items-center justify-between">
        <span className="text-base font-medium text-gray-600">{title}</span>
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

export default function ManagerDashboardPage() {
  const { data: usersData } = useUsers({ size: 1 });
  const { data: openEvents } = useEvents({ status: "OPEN", size: 1 });

  return (
    <div>
      <h1 className="mb-8 text-2xl font-bold text-gray-900">대시보드</h1>

      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
        <StatCard
          title="총 회원 수"
          value={usersData?.totalElements}
          href="/manager/users"
          color="border-blue-100"
        />
        <StatCard
          title="모집 중 이벤트"
          value={openEvents?.totalElements}
          href="/manager/events?status=OPEN"
          color="border-green-100"
        />
      </div>

      <div className="mt-10 rounded-2xl border border-dashed border-gray-300 bg-white p-8 text-center text-base text-gray-400">
        각 카드를 클릭하면 해당 관리 페이지로 이동합니다.
      </div>
    </div>
  );
}
