import React from "react";
import Link from "next/link";

interface PointDisplayProps {
  balance: number;
  label?: string;
  size?: "sm" | "md" | "lg";
  /** 우측 액션 버튼 노출 (예: 충전하기) */
  action?: { href: string; label: string };
}

export function PointDisplay({
  balance,
  label = "포인트 잔액",
  size = "md",
  action,
}: PointDisplayProps) {
  const amountClass =
    size === "lg"
      ? "text-4xl"
      : size === "sm"
      ? "text-xl"
      : "text-3xl";

  return (
    <div
      className="relative overflow-hidden rounded-2xl px-6 py-6 text-white shadow-soft-md"
      style={{
        background:
          "linear-gradient(135deg, #1f4332 0%, #143126 55%, #0c1f18 100%)",
      }}
    >
      {/* 미세한 글로우 패턴 */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute -right-12 -top-12 h-40 w-40 rounded-full"
        style={{
          background:
            "radial-gradient(circle, rgba(255,255,255,0.12) 0%, transparent 70%)",
        }}
      />
      <div className="relative flex items-center justify-between gap-4">
        <div className="min-w-0">
          <p className="mb-1 text-xs font-medium uppercase tracking-widest text-white/60">
            {label}
          </p>
          <p
            className={`${amountClass} font-bold tracking-tight-2 tabular-nums leading-none`}
          >
            {balance.toLocaleString()}
            <span className="ml-1.5 text-base font-semibold text-white/70">
              P
            </span>
          </p>
        </div>
        {action && (
          <Link
            href={action.href}
            className="shrink-0 rounded-xl border border-white/30 bg-white/10 px-4 py-2.5 text-sm font-semibold tracking-tight-1 text-white backdrop-blur-sm transition-colors hover:bg-white/20"
          >
            {action.label}
          </Link>
        )}
      </div>
    </div>
  );
}
