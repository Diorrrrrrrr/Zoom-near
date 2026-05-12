"use client";

import React, { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { clearSession, getRole, isAuthenticated } from "@/lib/auth/session";
import { isIdleExpired, touchActivity } from "@/lib/auth/session-activity";

const ACTIVITY_EVENTS: (keyof WindowEventMap)[] = [
  "click",
  "keydown",
  "touchstart",
  "scroll",
  "mousemove",
];

/**
 * 인증·권한 가드.
 * 1) 마운트 시 인증/역할 검증
 * 2) 사용자 입력 이벤트마다 활동 시각 갱신
 * 3) 60초 주기로 30분 미활동 여부 확인 → 만료 시 자동 로그아웃 + /login 복귀
 *
 * @param allowedRoles 허용할 역할 목록 (기본값: ["ADMIN"])
 */
export function AuthGuard({
  children,
  allowedRoles = ["ADMIN"],
}: {
  children: React.ReactNode;
  allowedRoles?: string[];
}) {
  const router = useRouter();
  const [ready, setReady] = useState(false);
  const expiredRef = useRef(false);

  useEffect(() => {
    if (!isAuthenticated()) {
      router.replace("/login");
      return;
    }
    const role = getRole();
    if (!role || !allowedRoles.includes(role)) {
      // 허용되지 않은 역할: /login으로 이동
      router.replace("/login");
      return;
    }
    touchActivity();
    setReady(true);

    function forceExpire() {
      if (expiredRef.current) return;
      expiredRef.current = true;
      clearSession();
      window.location.href = "/login?reason=idle";
    }

    function checkIdle() {
      if (isIdleExpired()) forceExpire();
    }

    function onActivity() {
      touchActivity();
    }

    ACTIVITY_EVENTS.forEach((ev) =>
      window.addEventListener(ev, onActivity, { passive: true })
    );
    const interval = window.setInterval(checkIdle, 60_000);
    document.addEventListener("visibilitychange", checkIdle);

    return () => {
      ACTIVITY_EVENTS.forEach((ev) =>
        window.removeEventListener(ev, onActivity)
      );
      window.clearInterval(interval);
      document.removeEventListener("visibilitychange", checkIdle);
    };
  }, [router]);

  if (!ready) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p className="text-base text-gray-400">인증 확인 중…</p>
      </div>
    );
  }

  return <>{children}</>;
}
