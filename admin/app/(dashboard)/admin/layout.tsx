"use client";

import React from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { clearSession } from "@/lib/auth/session";
import { AuthGuard } from "@/components/auth-guard";

const NAV_ITEMS = [
  { href: "/admin/users", label: "회원 관리" },
  { href: "/admin/events", label: "이벤트 관리" },
  { href: "/admin/audit-logs", label: "감사 로그" },
  { href: "/admin/manager-applications", label: "매니저 신청" },
];

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const pathname = usePathname();
  const router = useRouter();

  function handleLogout() {
    clearSession();
    router.replace("/login");
  }

  return (
    <AuthGuard>
      <div className="flex min-h-screen flex-col">
        {/* 상단 네비게이션 */}
        <header className="sticky top-0 z-40 border-b border-gray-200 bg-white shadow-sm">
          <div className="mx-auto flex max-w-screen-xl items-center justify-between px-6 py-4">
            <div className="flex items-center gap-8">
              <Link
                href="/admin/dashboard"
                className="text-xl font-bold text-brand-900 hover:text-brand-500"
              >
                ZOOM NEAR
                <span className="ml-2 rounded bg-brand-500 px-1.5 py-0.5 text-xs font-semibold text-white">
                  Admin
                </span>
              </Link>

              <nav aria-label="주 메뉴">
                <ul className="flex items-center gap-1">
                  {NAV_ITEMS.map((item) => {
                    const active =
                      pathname === item.href ||
                      pathname.startsWith(item.href + "/");
                    return (
                      <li key={item.href}>
                        <Link
                          href={item.href}
                          aria-current={active ? "page" : undefined}
                          className={`rounded-lg px-4 py-2 text-base font-medium transition-colors ${
                            active
                              ? "bg-brand-50 text-brand-500"
                              : "text-gray-600 hover:bg-gray-100 hover:text-gray-900"
                          }`}
                        >
                          {item.label}
                        </Link>
                      </li>
                    );
                  })}
                </ul>
              </nav>
            </div>

            <button
              onClick={handleLogout}
              className="rounded-lg border border-gray-300 bg-white px-4 py-2 text-base font-medium text-gray-600 hover:bg-gray-50 hover:text-red-600 focus:outline-none focus:ring-2 focus:ring-gray-300"
            >
              로그아웃
            </button>
          </div>
        </header>

        {/* 페이지 콘텐츠 */}
        <main className="mx-auto w-full max-w-screen-xl flex-1 px-6 py-8">
          {children}
        </main>
      </div>
    </AuthGuard>
  );
}
