"use client";

import React from "react";
import { useRouter } from "next/navigation";

interface TopBarProps {
  title: string;
  showBack?: boolean;
  actions?: React.ReactNode;
}

export function TopBar({ title, showBack = false, actions }: TopBarProps) {
  const router = useRouter();

  return (
    <header className="sticky top-0 z-40 border-b border-gray-100 bg-white/85 backdrop-blur-md supports-[backdrop-filter]:bg-white/70">
      <div className="flex h-14 items-center justify-between px-4">
        <div className="flex items-center gap-2">
          {showBack && (
            <button
              onClick={() => router.back()}
              aria-label="뒤로가기"
              className="flex h-10 w-10 items-center justify-center rounded-full text-gray-700 transition-colors hover:bg-gray-100 active:bg-gray-200"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth={2}
                className="h-5 w-5"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M15.75 19.5L8.25 12l7.5-7.5"
                />
              </svg>
            </button>
          )}
          <h1 className="text-lg font-semibold tracking-tight-2 text-gray-900">{title}</h1>
        </div>
        {actions && <div className="flex items-center gap-2">{actions}</div>}
      </div>
    </header>
  );
}
