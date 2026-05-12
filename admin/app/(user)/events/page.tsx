"use client";

import React, { useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { useEvents } from "@/lib/hooks/use-events-user";
import { TopBar } from "@/components/user-ui/top-bar";
import { EventCard } from "@/components/user-ui/event-card";
import { Icon } from "@/components/user-ui/icon";
import { getUserRole } from "@/lib/auth/user-session";
import type { EventSummary } from "@/lib/types-user";

type SortKey = "latest" | "popular" | "reviews";

const CATEGORY_OPTIONS: { value: string; label: string }[] = [
  { value: "SPORTS", label: "스포츠" },
  { value: "BOOK", label: "독서모임" },
  { value: "CULTURE", label: "문화·예술" },
  { value: "VOLUNTEER", label: "봉사" },
  { value: "EDUCATION", label: "교육·강의" },
  { value: "FOOD", label: "음식·카페" },
  { value: "WALK", label: "산책·등산" },
  { value: "ETC", label: "기타" },
];

const REGION_OPTIONS = [
  "서울",
  "경기",
  "인천",
  "부산",
  "대구",
  "광주",
  "대전",
  "울산",
  "기타",
];

const CAPACITY_BUCKETS: { value: string; label: string; test: (cap: number) => boolean }[] = [
  { value: "1-5", label: "1–5명", test: (c) => c >= 1 && c <= 5 },
  { value: "6-10", label: "6–10명", test: (c) => c >= 6 && c <= 10 },
  { value: "11-30", label: "11–30명", test: (c) => c >= 11 && c <= 30 },
  { value: "31+", label: "31명 이상", test: (c) => c >= 31 },
];

const SORT_OPTIONS: { value: SortKey; label: string; experimental?: boolean }[] = [
  { value: "latest", label: "최신순" },
  { value: "popular", label: "인기순" },
  { value: "reviews", label: "후기순", experimental: true },
];

/// 외부 클릭 시 콜백을 실행하는 훅.
function useOutsideClick(ref: React.RefObject<HTMLElement>, onOutside: () => void) {
  useEffect(() => {
    function handler(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) onOutside();
    }
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, [ref, onOutside]);
}

interface FilterChipProps {
  label: string;
  active: boolean;
  onClick: () => void;
}

function FilterChip({ label, active, onClick }: FilterChipProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`inline-flex w-full items-center justify-between gap-1 rounded-full border px-3.5 py-2.5 text-sm font-semibold tracking-tight-1 transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-forest-700/30 ${
        active
          ? "border-forest-700 bg-forest-700 text-white shadow-soft-sm hover:bg-forest-800"
          : "border-gray-200 bg-white text-gray-800 hover:border-gray-300 hover:bg-gray-50"
      }`}
    >
      <span className="truncate">{label}</span>
      <svg
        xmlns="http://www.w3.org/2000/svg"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth={2}
        className={`h-3.5 w-3.5 shrink-0 transition-transform ${active ? "rotate-180" : ""}`}
      >
        <path strokeLinecap="round" strokeLinejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
      </svg>
    </button>
  );
}

interface DropdownProps {
  open: boolean;
  onClose: () => void;
  children: React.ReactNode;
}

function Dropdown({ open, onClose, children }: DropdownProps) {
  const ref = useRef<HTMLDivElement>(null);
  useOutsideClick(ref, onClose);
  if (!open) return null;
  // 칩 폭이 좁아도 드롭다운은 최소 13rem 폭 보장 → 풋터 버튼 글자 안 깨짐
  return (
    <div
      ref={ref}
      className="absolute left-0 top-full z-30 mt-2 min-w-[13rem] max-w-[calc(100vw-2rem)] rounded-2xl border border-gray-100 bg-white shadow-soft-md"
    >
      {children}
    </div>
  );
}

function FabMenu({ role }: { role: string | null }) {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useOutsideClick(ref as React.RefObject<HTMLElement>, () => setOpen(false));

  useEffect(() => {
    function onEsc(e: KeyboardEvent) {
      if (e.key === "Escape") setOpen(false);
    }
    document.addEventListener("keydown", onEsc);
    return () => document.removeEventListener("keydown", onEsc);
  }, []);

  if (role !== "TUNTUN" && role !== "MANAGER") return null;

  return (
    <div ref={ref} className="fixed bottom-24 right-5 z-40 flex flex-col items-end gap-2">
      {open && (
        <div className="flex flex-col items-end gap-2 animate-in fade-in slide-in-from-bottom-2 duration-150">
          <button
            onClick={() => { setOpen(false); router.push("/events/mine"); }}
            className="flex h-11 items-center gap-2 rounded-full bg-white px-5 text-sm font-semibold text-gray-900 shadow-soft-md hover:bg-gray-50 active:bg-gray-100 border border-gray-100"
          >
            내 이벤트 관리하기
          </button>
          <button
            onClick={() => { setOpen(false); router.push("/events/create"); }}
            className="flex h-11 items-center gap-2 rounded-full bg-white px-5 text-sm font-semibold text-gray-900 shadow-soft-md hover:bg-gray-50 active:bg-gray-100 border border-gray-100"
          >
            이벤트 생성하기
          </button>
        </div>
      )}
      <button
        type="button"
        aria-label="이벤트 메뉴 열기"
        aria-expanded={open}
        onClick={() => setOpen((v) => !v)}
        className="flex h-14 w-14 items-center justify-center rounded-full bg-forest-700 text-white shadow-soft-md transition-all hover:-translate-y-0.5 hover:bg-forest-800 hover:shadow-soft-md active:translate-y-0"
      >
        <svg
          xmlns="http://www.w3.org/2000/svg"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth={2.5}
          className={`h-7 w-7 transition-transform duration-200 ${open ? "rotate-45" : ""}`}
        >
          <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
        </svg>
      </button>
    </div>
  );
}

export default function EventsPage() {
  const [q, setQ] = useState("");
  const [page, setPage] = useState(0);

  const [categories, setCategories] = useState<Set<string>>(new Set());
  const [regions, setRegions] = useState<Set<string>>(new Set());
  const [capacities, setCapacities] = useState<Set<string>>(new Set());
  const [sort, setSort] = useState<SortKey>("latest");

  const [openFilter, setOpenFilter] = useState<
    "category" | "region" | "capacity" | "sort" | null
  >(null);

  const role = getUserRole();

  // 다중 필터를 위해 한 번에 50개 fetch하고 클라이언트에서 필터/정렬
  const { data, isLoading, isError } = useEvents({
    status: "OPEN",
    q: q || undefined,
    page,
    size: 50,
  });

  const filteredSorted = useMemo<EventSummary[]>(() => {
    const list = data?.content ?? [];
    const filtered = list.filter((ev) => {
      if (categories.size > 0 && !categories.has(ev.category)) return false;
      if (regions.size > 0) {
        const hit = Array.from(regions).some((r) => ev.regionText?.includes(r));
        if (!hit) return false;
      }
      if (capacities.size > 0) {
        const hit = Array.from(capacities).some((bv) => {
          const bucket = CAPACITY_BUCKETS.find((b) => b.value === bv);
          return bucket ? bucket.test(ev.capacity) : false;
        });
        if (!hit) return false;
      }
      return true;
    });
    const sorted = [...filtered].sort((a, b) => {
      if (sort === "popular") {
        return (b.joinedCount ?? 0) - (a.joinedCount ?? 0);
      }
      if (sort === "reviews") {
        // 후기 데이터 미구현 — 임시로 인기순으로 폴백
        return (b.joinedCount ?? 0) - (a.joinedCount ?? 0);
      }
      // latest: startsAt 최근 등록 추정 (createdAt 없으므로 ISO 비교 내림차순)
      return (b.startsAt ?? "").localeCompare(a.startsAt ?? "");
    });
    return sorted;
  }, [data, categories, regions, capacities, sort]);

  function toggle(set: Set<string>, val: string, setter: (s: Set<string>) => void) {
    const next = new Set(set);
    if (next.has(val)) next.delete(val);
    else next.add(val);
    setter(next);
  }

  function chipLabel(name: string, count: number): string {
    return count > 0 ? `${name} ${count}` : name;
  }

  const sortLabel = SORT_OPTIONS.find((s) => s.value === sort)?.label ?? "정렬";

  return (
    <>
      <TopBar title="이벤트" />

      <div className="px-4 py-4 space-y-4">
        {/* 검색 */}
        <input
          type="search"
          placeholder="이벤트 이름으로 검색"
          value={q}
          onChange={(e) => {
            setQ(e.target.value);
            setPage(0);
          }}
          className="w-full rounded-xl border border-gray-200 px-4 py-3 text-base text-gray-900 placeholder-gray-400 focus:border-forest-700 focus:outline-none focus:ring-2 focus:ring-forest-700/20"
        />

        {/* 필터 칩 바 — 4개를 가로 균등 분할 */}
        <div className="relative">
          <div className="grid grid-cols-4 gap-2">
            <div className="relative">
              <FilterChip
                label={chipLabel("카테고리", categories.size)}
                active={categories.size > 0 || openFilter === "category"}
                onClick={() =>
                  setOpenFilter(openFilter === "category" ? null : "category")
                }
              />
              <Dropdown
                open={openFilter === "category"}
                onClose={() => setOpenFilter(null)}
              >
                <div className="max-h-[14rem] overflow-y-auto py-2">
                  {CATEGORY_OPTIONS.map((opt) => {
                    const checked = categories.has(opt.value);
                    return (
                      <button
                        key={opt.value}
                        onClick={() => toggle(categories, opt.value, setCategories)}
                        className={`flex w-full items-center justify-between px-4 py-2.5 text-left text-sm font-medium transition-colors ${
                          checked
                            ? "bg-forest-50 text-forest-700"
                            : "text-gray-800 hover:bg-gray-50"
                        }`}
                      >
                        {opt.label}
                        {checked && (
                          <Icon name="check" className="h-4 w-4 text-forest-700" />
                        )}
                      </button>
                    );
                  })}
                </div>
                <div className="flex border-t border-gray-100 p-2">
                  <button
                    onClick={() => setCategories(new Set())}
                    className="flex-1 rounded-lg px-3 py-2 text-sm font-medium text-gray-500 whitespace-nowrap hover:bg-gray-50"
                  >
                    초기화
                  </button>
                  <button
                    onClick={() => setOpenFilter(null)}
                    className="flex-1 rounded-lg bg-forest-700 px-3 py-2 text-sm font-semibold text-white hover:bg-forest-800 whitespace-nowrap"
                  >
                    완료
                  </button>
                </div>
              </Dropdown>
            </div>

            <div className="relative">
              <FilterChip
                label={chipLabel("지역", regions.size)}
                active={regions.size > 0 || openFilter === "region"}
                onClick={() =>
                  setOpenFilter(openFilter === "region" ? null : "region")
                }
              />
              <Dropdown
                open={openFilter === "region"}
                onClose={() => setOpenFilter(null)}
              >
                <div className="max-h-[14rem] overflow-y-auto py-2">
                  {REGION_OPTIONS.map((r) => {
                    const checked = regions.has(r);
                    return (
                      <button
                        key={r}
                        onClick={() => toggle(regions, r, setRegions)}
                        className={`flex w-full items-center justify-between px-4 py-2.5 text-left text-sm font-medium transition-colors ${
                          checked
                            ? "bg-forest-50 text-forest-700"
                            : "text-gray-800 hover:bg-gray-50"
                        }`}
                      >
                        {r}
                        {checked && (
                          <Icon name="check" className="h-4 w-4 text-forest-700" />
                        )}
                      </button>
                    );
                  })}
                </div>
                <div className="flex border-t border-gray-100 p-2">
                  <button
                    onClick={() => setRegions(new Set())}
                    className="flex-1 rounded-lg px-3 py-2 text-sm font-medium text-gray-500 whitespace-nowrap hover:bg-gray-50"
                  >
                    초기화
                  </button>
                  <button
                    onClick={() => setOpenFilter(null)}
                    className="flex-1 rounded-lg bg-forest-700 px-3 py-2 text-sm font-semibold text-white hover:bg-forest-800 whitespace-nowrap"
                  >
                    완료
                  </button>
                </div>
              </Dropdown>
            </div>

            <div className="relative">
              <FilterChip
                label={chipLabel("인원", capacities.size)}
                active={capacities.size > 0 || openFilter === "capacity"}
                onClick={() =>
                  setOpenFilter(openFilter === "capacity" ? null : "capacity")
                }
              />
              <Dropdown
                open={openFilter === "capacity"}
                onClose={() => setOpenFilter(null)}
              >
                <div className="max-h-[14rem] overflow-y-auto py-2">
                  {CAPACITY_BUCKETS.map((b) => {
                    const checked = capacities.has(b.value);
                    return (
                      <button
                        key={b.value}
                        onClick={() => toggle(capacities, b.value, setCapacities)}
                        className={`flex w-full items-center justify-between px-4 py-2.5 text-left text-sm font-medium transition-colors ${
                          checked
                            ? "bg-forest-50 text-forest-700"
                            : "text-gray-800 hover:bg-gray-50"
                        }`}
                      >
                        {b.label}
                        {checked && (
                          <Icon name="check" className="h-4 w-4 text-forest-700" />
                        )}
                      </button>
                    );
                  })}
                </div>
                <div className="flex border-t border-gray-100 p-2">
                  <button
                    onClick={() => setCapacities(new Set())}
                    className="flex-1 rounded-lg px-3 py-2 text-sm font-medium text-gray-500 whitespace-nowrap hover:bg-gray-50"
                  >
                    초기화
                  </button>
                  <button
                    onClick={() => setOpenFilter(null)}
                    className="flex-1 rounded-lg bg-forest-700 px-3 py-2 text-sm font-semibold text-white hover:bg-forest-800 whitespace-nowrap"
                  >
                    완료
                  </button>
                </div>
              </Dropdown>
            </div>

            <div className="relative">
              <FilterChip
                label={sortLabel}
                active={openFilter === "sort"}
                onClick={() =>
                  setOpenFilter(openFilter === "sort" ? null : "sort")
                }
              />
              <Dropdown
                open={openFilter === "sort"}
                onClose={() => setOpenFilter(null)}
              >
                <div className="max-h-[14rem] overflow-y-auto py-2">
                  {SORT_OPTIONS.map((opt) => {
                    const checked = sort === opt.value;
                    return (
                      <button
                        key={opt.value}
                        onClick={() => {
                          setSort(opt.value);
                          setOpenFilter(null);
                        }}
                        className={`flex w-full items-center justify-between px-4 py-2.5 text-left text-sm font-medium transition-colors ${
                          checked
                            ? "bg-forest-50 text-forest-700"
                            : "text-gray-800 hover:bg-gray-50"
                        }`}
                      >
                        <span>
                          {opt.label}
                          {opt.experimental && (
                            <span className="ml-1.5 text-[10px] font-normal text-gray-400">
                              (준비 중)
                            </span>
                          )}
                        </span>
                        {checked && (
                          <Icon name="check" className="h-4 w-4 text-forest-700" />
                        )}
                      </button>
                    );
                  })}
                </div>
              </Dropdown>
            </div>
          </div>
        </div>

        {/* 결과 카운트 */}
        {data && (
          <p className="text-sm text-gray-500">
            총 {filteredSorted.length}개의 이벤트
          </p>
        )}

        {/* 목록 */}
        {isLoading && (
          <div className="flex justify-center py-12">
            <span className="text-base text-gray-400">불러오는 중…</span>
          </div>
        )}

        {isError && (
          <div className="rounded-xl border border-gray-200 bg-gray-50 p-4 text-base text-gray-900">
            이벤트를 불러오지 못했습니다. 다시 시도해 주세요.
          </div>
        )}

        {data && filteredSorted.length === 0 && !isLoading && (
          <div className="flex flex-col items-center gap-3 py-16 text-gray-400">
            <Icon name="inbox" className="h-10 w-10" strokeWidth={1.5} />
            <p className="text-base">조건에 맞는 이벤트가 없어요</p>
          </div>
        )}

        <div className="space-y-3">
          {filteredSorted.map((event) => (
            <EventCard key={event.id} event={event} />
          ))}
        </div>

        {/* 페이지네이션 (서버 페이지 기준) */}
        {data && data.totalPages > 1 && (
          <div className="flex items-center justify-center gap-3 pt-2">
            <button
              disabled={page === 0}
              onClick={() => setPage((p) => p - 1)}
              className="h-11 rounded-xl border border-gray-300 bg-white px-5 text-base font-medium disabled:opacity-40"
            >
              이전
            </button>
            <span className="text-base text-gray-600">
              {page + 1} / {data.totalPages}
            </span>
            <button
              disabled={page >= data.totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
              className="h-11 rounded-xl border border-gray-300 bg-white px-5 text-base font-medium disabled:opacity-40"
            >
              다음
            </button>
          </div>
        )}
      </div>

      {/* 이벤트 FAB 드롭업 — 튼튼이/매니저 한정 */}
      <FabMenu role={role} />
    </>
  );
}
