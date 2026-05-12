"use client";

import React, { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useMe } from "@/lib/hooks/use-me";
import { useLinkages } from "@/lib/hooks/use-linkages";
import { TopBar } from "@/components/user-ui/top-bar";
import { PointDisplay } from "@/components/user-ui/point-display";
import { Card } from "@/components/user-ui/card";
import { Icon, type IconName } from "@/components/user-ui/icon";
import { clearUserSession, getUserRole } from "@/lib/auth/user-session";
import { formatRole } from "@/lib/labels";

type MenuItem = { href: string; label: string; icon: IconName };

const MENU_ITEMS: MenuItem[] = [
  { href: "/me/change-password", label: "비밀번호 변경", icon: "lock" },
  { href: "/me/font-size", label: "글자 크기 설정", icon: "typography" },
  { href: "/legal/terms", label: "이용약관", icon: "document" },
  { href: "/legal/privacy", label: "개인정보처리방침", icon: "shield" },
  { href: "/legal/help", label: "도움말", icon: "help" },
];

const TUNTUN_MENU: MenuItem[] = [
  { href: "/approvals", label: "승인 관리", icon: "check" },
  { href: "/linkage", label: "연동 관리", icon: "link" },
];

const DUNDUN_MENU: MenuItem[] = [
  { href: "/approvals", label: "승인 관리", icon: "check" },
  { href: "/linkage", label: "연동 관리", icon: "link" },
];

const MANAGER_MENU: MenuItem[] = [
  { href: "/manager/console", label: "매니저 콘솔", icon: "sliders" },
  { href: "/events/create", label: "이벤트 등록", icon: "edit" },
];

/// 이벤트 참여 횟수 기반 등급 정의 (V001_2 seed_ranks 와 동기화).
const RANK_DETAILS: { code: string; name: string; min: number; max: number | null; desc: string }[] = [
  { code: "PPOJJAK", name: "뽀짝이", min: 0, max: 4, desc: "이제 막 활동을 시작한 분" },
  { code: "GWIYOMI", name: "귀요미", min: 5, max: 9, desc: "꾸준히 이웃을 만나는 분" },
  { code: "KKAMJJIK", name: "깜찍이", min: 10, max: 29, desc: "동네의 활기를 채워가는 분" },
  { code: "HWALHWAL", name: "활활이", min: 30, max: null, desc: "이웃 모임의 든든한 축" },
];

function RankInfoPopover({ onClose }: { onClose: () => void }) {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    function handler(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) onClose();
    }
    function esc(e: KeyboardEvent) {
      if (e.key === "Escape") onClose();
    }
    document.addEventListener("mousedown", handler);
    document.addEventListener("keydown", esc);
    return () => {
      document.removeEventListener("mousedown", handler);
      document.removeEventListener("keydown", esc);
    };
  }, [onClose]);
  return (
    <div
      ref={ref}
      role="dialog"
      aria-label="등급 안내"
      className="absolute left-0 top-full z-30 mt-2 w-[20rem] max-w-[calc(100vw-2rem)] rounded-2xl border border-gray-100 bg-white p-4 shadow-soft-md"
    >
      <p className="mb-3 text-sm font-semibold tracking-tight-1 text-gray-900">
        등급은 이벤트 참여 횟수로 정해져요
      </p>
      <table className="w-full text-left">
        <thead>
          <tr className="border-b border-gray-100 text-[11px] font-semibold uppercase tracking-tight-1 text-gray-400">
            <th className="pb-1.5 pr-3 font-semibold">등급명</th>
            <th className="pb-1.5 pr-3 font-semibold whitespace-nowrap">참여횟수</th>
            <th className="pb-1.5 font-semibold">등급 설명</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-50">
          {RANK_DETAILS.map((r) => (
            <tr key={r.code} className="align-top">
              <td className="py-2 pr-3 text-sm font-semibold text-forest-700 whitespace-nowrap">
                {r.name}
              </td>
              <td className="py-2 pr-3 text-xs tabular-nums text-gray-600 whitespace-nowrap">
                {r.max === null ? `${r.min}+` : `${r.min}–${r.max}`}
              </td>
              <td className="py-2 text-xs text-gray-500">{r.desc}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default function MePage() {
  const router = useRouter();
  const { data: me, isLoading } = useMe();
  const role = getUserRole();
  const [showRankInfo, setShowRankInfo] = useState(false);

  // 든든이는 연동된 튼튼이 목록을 잔액 영역에 표시
  const { data: linkages } = useLinkages();

  function handleLogout() {
    clearUserSession();
    router.replace("/login");
  }

  const isDundun = role === "DUNDUN";
  const isTuntun = role === "TUNTUN";

  const roleMenu = isTuntun
    ? TUNTUN_MENU
    : isDundun
    ? DUNDUN_MENU
    : role === "MANAGER"
    ? MANAGER_MENU
    : [];

  return (
    <>
      <TopBar title="내 정보" />

      <div className="px-5 py-6 space-y-6">
        {/* 프로필 카드 */}
        <Card>
          {isLoading ? (
            <p className="text-base text-gray-400">불러오는 중…</p>
          ) : me ? (
            <div className="space-y-1">
              <p className="text-2xl font-bold tracking-tight-2 text-gray-900">{me.name}</p>
              <p className="text-sm text-gray-500">아이디 · {me.loginId}</p>
              <p className="text-sm text-gray-500">
                코드 · <span className="font-semibold text-forest-700">{me.uniqueCode}</span>
              </p>
              {/* 등급: 튼튼이만. 든든이/매니저는 등급 의미 없음 */}
              {isTuntun && (
                <div className="relative inline-flex items-center gap-1.5">
                  <span className="text-sm text-gray-500">등급 · {me.rankDisplayName}</span>
                  <button
                    type="button"
                    aria-label="등급 안내"
                    onClick={() => setShowRankInfo((v) => !v)}
                    className="flex h-5 w-5 items-center justify-center rounded-full border border-gray-300 text-[10px] font-bold leading-none text-gray-500 transition-colors hover:border-forest-700 hover:text-forest-700"
                  >
                    i
                  </button>
                  {showRankInfo && (
                    <RankInfoPopover onClose={() => setShowRankInfo(false)} />
                  )}
                </div>
              )}
              <p className="text-sm text-gray-500">역할 · {formatRole(me.role)}</p>
            </div>
          ) : (
            <p className="text-base text-gray-600">프로필을 불러오지 못했습니다.</p>
          )}
        </Card>

        {/* 잔액 영역: 튼튼이/매니저는 본인 잔액 + 충전, 든든이는 연동된 튼튼이 잔액 카드 */}
        {!isDundun && me && (
          <PointDisplay
            balance={me.balance}
            size="md"
            action={isTuntun ? { href: "/me/charge", label: "충전하기" } : undefined}
          />
        )}

        {isDundun && (
          <section className="space-y-3">
            <h3 className="text-sm font-semibold tracking-tight-1 text-gray-700">
              연동된 튼튼이
            </h3>
            {(!linkages || linkages.length === 0) ? (
              <Card>
                <div className="flex items-center justify-between gap-4">
                  <p className="text-sm text-gray-500">
                    아직 연동된 튼튼이가 없어요.
                  </p>
                  <Link
                    href="/linkage"
                    className="shrink-0 rounded-xl bg-forest-700 px-4 py-2 text-sm font-semibold text-white hover:bg-forest-800"
                  >
                    연동하러 가기
                  </Link>
                </div>
              </Card>
            ) : (
              linkages.map((l) => (
                <div
                  key={l.id}
                  className="relative overflow-hidden rounded-2xl px-5 py-5 text-white shadow-soft-md"
                  style={{
                    background:
                      "linear-gradient(135deg, #1f4332 0%, #143126 60%, #0c1f18 100%)",
                  }}
                >
                  <span
                    aria-hidden="true"
                    className="pointer-events-none absolute -right-10 -top-10 h-32 w-32 rounded-full"
                    style={{
                      background:
                        "radial-gradient(circle, rgba(255,255,255,0.10) 0%, transparent 70%)",
                    }}
                  />
                  <div className="relative">
                    <p className="text-xs font-medium uppercase tracking-widest text-white/55">
                      튼튼이
                    </p>
                    <p className="mt-0.5 text-lg font-bold tracking-tight-1 text-white">
                      {l.otherUserName}
                    </p>
                    <p className="text-xs text-white/65">
                      아이디 · {l.otherUserLoginId} · 코드 {l.otherUserUniqueCode}
                      {l.isPrimary && (
                        <span className="ml-2 rounded-full border border-white/30 bg-white/10 px-1.5 py-0.5 text-[10px] font-semibold tracking-tight-1">
                          주 연동
                        </span>
                      )}
                    </p>
                    <div className="mt-4 flex items-end justify-between gap-4">
                      <div>
                        <p className="text-xs font-medium uppercase tracking-widest text-white/55">
                          잔액
                        </p>
                        <p className="mt-0.5 text-3xl font-bold tracking-tight-2 tabular-nums text-white">
                          {(l.otherUserBalance ?? 0).toLocaleString()}
                          <span className="ml-1.5 text-sm font-semibold text-white/70">
                            P
                          </span>
                        </p>
                      </div>
                      <Link
                        href={`/linkage/proxy-charge?tuntunId=${l.otherUserId}`}
                        className="shrink-0 rounded-xl border border-white/30 bg-white/10 px-4 py-2.5 text-sm font-semibold tracking-tight-1 text-white backdrop-blur-sm hover:bg-white/20"
                      >
                        충전해주기
                      </Link>
                    </div>
                  </div>
                </div>
              ))
            )}
          </section>
        )}

        {/* 역할별 메뉴 */}
        {roleMenu.length > 0 && (
          <div className="rounded-2xl border border-gray-100 bg-white shadow-soft-sm overflow-hidden">
            {roleMenu.map((item, idx) => (
              <Link
                key={item.href}
                href={item.href}
                className={`flex items-center gap-4 px-5 py-4 text-base font-medium tracking-tight-1 text-gray-900 transition-colors hover:bg-gray-50 active:bg-gray-100 ${
                  idx > 0 ? "border-t border-gray-100" : ""
                }`}
              >
                <Icon name={item.icon} className="h-5 w-5 text-forest-700" />
                {item.label}
                <svg className="ml-auto h-5 w-5 text-gray-300" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.75}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
                </svg>
              </Link>
            ))}
          </div>
        )}

        {/* 공통 메뉴 */}
        <div className="rounded-2xl border border-gray-100 bg-white shadow-soft-sm overflow-hidden">
          {MENU_ITEMS.map((item, idx) => (
            <Link
              key={item.href}
              href={item.href}
              className={`flex items-center gap-4 px-5 py-4 text-base font-medium tracking-tight-1 text-gray-900 transition-colors hover:bg-gray-50 active:bg-gray-100 ${
                idx > 0 ? "border-t border-gray-100" : ""
              }`}
            >
              <Icon name={item.icon} className="h-5 w-5 text-forest-700" />
              {item.label}
              <svg className="ml-auto h-5 w-5 text-gray-300" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.75}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
              </svg>
            </Link>
          ))}
        </div>

        {/* 로그아웃 */}
        <button
          onClick={handleLogout}
          className="w-full rounded-2xl border border-red-200 bg-white px-5 py-4 text-center text-base font-bold text-red-600 transition-colors hover:bg-red-50"
        >
          로그아웃
        </button>
      </div>
    </>
  );
}
