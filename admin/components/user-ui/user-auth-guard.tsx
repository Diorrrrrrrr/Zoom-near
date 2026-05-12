"use client";

import React, { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import {
  clearUserSession,
  getUserRole,
  isUserAuthenticated,
} from "@/lib/auth/user-session";
import { isIdleExpired, touchActivity } from "@/lib/auth/session-activity";

interface UserAuthGuardProps {
  children: React.ReactNode;
  /** 접근 허용 역할 목록. 비어 있으면 로그인만 확인 */
  allowedRoles?: string[];
}

const ACTIVITY_EVENTS: (keyof WindowEventMap)[] = [
  "click",
  "keydown",
  "touchstart",
  "scroll",
  "mousemove",
];

/// 사용자(TUNTUN/DUNDUN/MANAGER) 인증 가드.
/// 1) 마운트 시 인증 + 역할 검증
/// 2) 사용자 입력 이벤트마다 활동 시각 갱신
/// 3) 60초 주기로 30분 미활동 여부 확인 → 만료 시 자동 로그아웃 + /login 복귀
export function UserAuthGuard({ children, allowedRoles }: UserAuthGuardProps) {
  const router = useRouter();
  const [ready, setReady] = useState(false);
  const expiredRef = useRef(false);

  useEffect(() => {
    if (!isUserAuthenticated()) {
      router.replace("/login");
      return;
    }
    if (allowedRoles && allowedRoles.length > 0) {
      const role = getUserRole();
      if (!role || !allowedRoles.includes(role)) {
        router.replace("/home");
        return;
      }
    }
    // 라우트 진입 자체를 활동으로 인정
    touchActivity();
    setReady(true);

    function forceExpire() {
      if (expiredRef.current) return;
      expiredRef.current = true;
      clearUserSession();
      // window.location 사용 — React 라우터의 상태와 무관하게 완전 리로드
      window.location.href = "/login?reason=idle";
    }

    function checkIdle() {
      if (isIdleExpired()) forceExpire();
    }

    function onActivity() {
      touchActivity();
    }

    // 입력 이벤트 → 활동 시각 갱신 (passive 로 스크롤 성능 보호)
    ACTIVITY_EVENTS.forEach((ev) =>
      window.addEventListener(ev, onActivity, { passive: true })
    );
    // 1분 주기 만료 체크
    const interval = window.setInterval(checkIdle, 60_000);
    // 탭이 다시 활성화될 때도 즉시 한 번 체크
    document.addEventListener("visibilitychange", checkIdle);

    return () => {
      ACTIVITY_EVENTS.forEach((ev) =>
        window.removeEventListener(ev, onActivity)
      );
      window.clearInterval(interval);
      document.removeEventListener("visibilitychange", checkIdle);
    };
  }, [router, allowedRoles]);

  if (!ready) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p className="text-lg text-gray-400">확인 중…</p>
      </div>
    );
  }

  return <>{children}</>;
}
