"use client";

import React from "react";

type BadgeVariant =
  | "active"
  | "suspended"
  | "withdrawn"
  | "open"
  | "closed"
  | "cancelled"
  | "pending"
  | "approved"
  | "rejected"
  | "success"
  | "failed"
  | "user"
  | "manager"
  | "admin"
  | "default";

const VARIANT_STYLES: Record<BadgeVariant, string> = {
  active: "bg-green-100 text-green-800 border-green-200",
  suspended: "bg-red-100 text-red-800 border-red-200",
  withdrawn: "bg-gray-100 text-gray-600 border-gray-200",
  open: "bg-blue-100 text-blue-800 border-blue-200",
  closed: "bg-gray-100 text-gray-600 border-gray-200",
  cancelled: "bg-red-100 text-red-700 border-red-200",
  pending: "bg-yellow-100 text-yellow-800 border-yellow-200",
  approved: "bg-green-100 text-green-800 border-green-200",
  rejected: "bg-red-100 text-red-800 border-red-200",
  success: "bg-green-100 text-green-800 border-green-200",
  failed: "bg-red-100 text-red-800 border-red-200",
  user: "bg-slate-100 text-slate-700 border-slate-200",
  manager: "bg-purple-100 text-purple-800 border-purple-200",
  admin: "bg-brand-50 text-brand-900 border-brand-500",
  default: "bg-gray-100 text-gray-700 border-gray-200",
};

const VARIANT_ICONS: Record<BadgeVariant, string> = {
  active: "●",
  suspended: "⊘",
  withdrawn: "○",
  open: "▶",
  closed: "■",
  cancelled: "✕",
  pending: "⋯",
  approved: "✓",
  rejected: "✕",
  success: "✓",
  failed: "✕",
  user: "U",
  manager: "M",
  admin: "A",
  default: "·",
};

interface BadgeProps {
  variant: BadgeVariant;
  label: string;
  className?: string;
}

export function Badge({ variant, label, className = "" }: BadgeProps) {
  const styles = VARIANT_STYLES[variant] ?? VARIANT_STYLES.default;
  const icon = VARIANT_ICONS[variant] ?? VARIANT_ICONS.default;

  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-xs font-medium ${styles} ${className}`}
      aria-label={label}
    >
      <span aria-hidden="true" className="text-[10px]">
        {icon}
      </span>
      {label}
    </span>
  );
}

/** 상태 값을 Badge variant 로 변환하는 헬퍼들 */
export function userStatusVariant(
  status: string
): BadgeVariant {
  const map: Record<string, BadgeVariant> = {
    ACTIVE: "active",
    SUSPENDED: "suspended",
    WITHDRAWN: "withdrawn",
  };
  return map[status] ?? "default";
}

export function userStatusLabel(status: string): string {
  const map: Record<string, string> = {
    ACTIVE: "활성",
    SUSPENDED: "정지",
    WITHDRAWN: "탈퇴",
  };
  return map[status] ?? status;
}

export function eventStatusVariant(status: string): BadgeVariant {
  const map: Record<string, BadgeVariant> = {
    OPEN: "open",
    CLOSED: "closed",
    CANCELLED: "cancelled",
  };
  return map[status] ?? "default";
}

export function eventStatusLabel(status: string): string {
  const map: Record<string, string> = {
    OPEN: "모집중",
    CLOSED: "종료",
    CANCELLED: "취소",
  };
  return map[status] ?? status;
}

export function applicationStatusVariant(status: string): BadgeVariant {
  const map: Record<string, BadgeVariant> = {
    PENDING: "pending",
    APPROVED: "approved",
    REJECTED: "rejected",
  };
  return map[status] ?? "default";
}

export function applicationStatusLabel(status: string): string {
  const map: Record<string, string> = {
    PENDING: "대기",
    APPROVED: "승인",
    REJECTED: "거절",
  };
  return map[status] ?? status;
}

export function auditStatusVariant(status: string): BadgeVariant {
  return status === "SUCCESS" ? "success" : "failed";
}

export function roleVariant(role: string): BadgeVariant {
  const map: Record<string, BadgeVariant> = {
    TUNTUN: "user",
    DUNDUN: "user",
    USER: "user",
    MANAGER: "manager",
    ADMIN: "admin",
  };
  return map[role] ?? "default";
}

export function roleLabel(role: string): string {
  const map: Record<string, string> = {
    TUNTUN: "튼튼이",
    DUNDUN: "든든이",
    USER: "일반",
    MANAGER: "매니저",
    ADMIN: "관리자",
  };
  return map[role] ?? role;
}
