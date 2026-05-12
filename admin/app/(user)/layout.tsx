"use client";

import React from "react";
import { BottomNav } from "@/components/user-ui/bottom-nav";
import { UserAuthGuard } from "@/components/user-ui/user-auth-guard";

export default function UserLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <UserAuthGuard>
      <div className="flex min-h-screen flex-col bg-[#FAFAF8]">
        <div
          className="mx-auto w-full max-w-lg flex-1 pb-24"
          style={{ paddingBottom: "calc(6rem + env(safe-area-inset-bottom))" }}
        >
          {children}
        </div>
        <BottomNav />
      </div>
    </UserAuthGuard>
  );
}
