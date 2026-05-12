"use client";

import React, { useState } from "react";
import { useRouter } from "next/navigation";
import { useCreateEvent } from "@/lib/hooks/use-events-user";
import { TopBar } from "@/components/user-ui/top-bar";
import { Input } from "@/components/user-ui/input";
import { Button } from "@/components/user-ui/button";
import { UserAuthGuard } from "@/components/user-ui/user-auth-guard";
import { getUserRole } from "@/lib/auth/user-session";

const CATEGORIES = ["문화", "스포츠", "봉사", "교육", "기타"];

/// 날짜(YYYY-MM-DD) + 시 + 분 → ISO 문자열. 잘못된 입력이면 null.
function composeIso(date: string, hh: string, mm: string): string | null {
  if (!date) return null;
  const h = Number(hh);
  const m = Number(mm);
  if (!Number.isFinite(h) || h < 0 || h > 23) return null;
  if (!Number.isFinite(m) || m < 0 || m > 59) return null;
  const local = new Date(
    `${date}T${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}:00`
  );
  if (Number.isNaN(local.getTime())) return null;
  return local.toISOString();
}

export default function CreateEventPage() {
  const router = useRouter();
  const createMutation = useCreateEvent();
  const role = getUserRole();

  const [form, setForm] = useState({
    title: "",
    description: "",
    regionText: "",
    category: "기타",
    startDate: "",
    startHour: "",
    startMinute: "",
    endDate: "",
    endHour: "",
    endMinute: "",
    sameDay: false,
    capacity: "10",
    pointCost: "0",
  });
  const [error, setError] = useState<string | null>(null);

  function setField<K extends keyof typeof form>(key: K, value: (typeof form)[K]) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  /// 0 이상의 정수만 통과시키는 onChange. 음수·소수·문자 입력 차단.
  function digitsOnly(setter: (v: string) => void, maxValue?: number) {
    return (e: React.ChangeEvent<HTMLInputElement>) => {
      const v = e.target.value;
      if (v === "") return setter("");
      if (!/^\d+$/.test(v)) return;
      if (maxValue !== undefined && Number(v) > maxValue) return;
      setter(v);
    };
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    if (
      !form.title ||
      !form.description ||
      !form.regionText ||
      !form.startDate ||
      form.startHour === "" ||
      form.startMinute === ""
    ) {
      setError("필수 항목을 모두 입력해 주세요.");
      return;
    }

    const startIso = composeIso(form.startDate, form.startHour, form.startMinute);
    if (!startIso) {
      setError("시작 일시가 올바르지 않습니다.");
      return;
    }

    let endIso: string | null;
    if (form.sameDay) {
      endIso = composeIso(form.startDate, "23", "59");
    } else {
      if (!form.endDate || form.endHour === "" || form.endMinute === "") {
        setError("종료 일시를 모두 입력해 주세요.");
        return;
      }
      endIso = composeIso(form.endDate, form.endHour, form.endMinute);
      if (!endIso) {
        setError("종료 일시가 올바르지 않습니다.");
        return;
      }
      if (new Date(endIso) <= new Date(startIso)) {
        setError("종료 일시는 시작 일시 이후여야 합니다.");
        return;
      }
    }

    const capacityNum = Math.max(1, Number(form.capacity) || 0);
    const pointCostNum = Math.max(0, Number(form.pointCost) || 0);

    try {
      const event = await createMutation.mutateAsync({
        title: form.title,
        description: form.description,
        regionText: form.regionText,
        category: form.category,
        startsAt: startIso,
        endsAt: endIso!,
        capacity: capacityNum,
        pointCost: pointCostNum,
        managerProgram: role === "MANAGER",
      });
      router.replace(`/events/${event.id}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "등록 중 오류가 발생했습니다.");
    }
  }

  const endDisabled = form.sameDay;

  return (
    <UserAuthGuard allowedRoles={["TUNTUN", "DUNDUN", "MANAGER"]}>
      <TopBar title="이벤트 등록" showBack />

      <form onSubmit={handleSubmit} className="space-y-5 px-4 py-5">
        <Input
          label="이벤트 제목 *"
          name="title"
          value={form.title}
          onChange={(e) => setField("title", e.target.value)}
          placeholder="이벤트 이름을 입력하세요"
        />

        <div className="flex flex-col gap-1.5">
          <label className="text-base font-medium text-gray-700">상세 내용 *</label>
          <textarea
            value={form.description}
            onChange={(e) => setField("description", e.target.value)}
            rows={4}
            placeholder="이벤트 내용을 입력하세요"
            className="w-full rounded-xl border border-gray-300 px-4 py-3.5 text-lg text-gray-900 placeholder-gray-400 focus:border-forest-700 focus:outline-none focus:ring-2 focus:ring-forest-700/20"
          />
        </div>

        <Input
          label="지역 *"
          name="regionText"
          value={form.regionText}
          onChange={(e) => setField("regionText", e.target.value)}
          placeholder="예: 서울 강남구"
        />

        <div className="flex flex-col gap-1.5">
          <label className="text-base font-medium text-gray-700">카테고리</label>
          <select
            value={form.category}
            onChange={(e) => setField("category", e.target.value)}
            className="w-full rounded-xl border border-gray-300 px-4 py-3.5 text-lg text-gray-900 focus:border-forest-700 focus:outline-none focus:ring-2 focus:ring-forest-700/20"
          >
            {CATEGORIES.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>
        </div>

        {/* 시작 일시 — 왼쪽: 날짜, 오른쪽: 시 / 분 */}
        <div className="flex flex-col gap-1.5">
          <label className="text-base font-medium text-gray-700">시작 일시 *</label>
          <div className="grid grid-cols-[1fr_4rem_4rem] gap-2">
            <input
              type="date"
              value={form.startDate}
              onChange={(e) => setField("startDate", e.target.value)}
              className="w-full rounded-xl border border-gray-300 px-3 py-3 text-base text-gray-900 focus:border-forest-700 focus:outline-none focus:ring-2 focus:ring-forest-700/20"
            />
            <input
              type="text"
              inputMode="numeric"
              pattern="\d*"
              maxLength={2}
              placeholder="시"
              value={form.startHour}
              onChange={digitsOnly((v) => setField("startHour", v), 23)}
              className="w-full rounded-xl border border-gray-300 px-2 py-3 text-center text-base text-gray-900 placeholder-gray-400 focus:border-forest-700 focus:outline-none focus:ring-2 focus:ring-forest-700/20"
              aria-label="시작 시"
            />
            <input
              type="text"
              inputMode="numeric"
              pattern="\d*"
              maxLength={2}
              placeholder="분"
              value={form.startMinute}
              onChange={digitsOnly((v) => setField("startMinute", v), 59)}
              className="w-full rounded-xl border border-gray-300 px-2 py-3 text-center text-base text-gray-900 placeholder-gray-400 focus:border-forest-700 focus:outline-none focus:ring-2 focus:ring-forest-700/20"
              aria-label="시작 분"
            />
          </div>

          <label className="mt-1 flex items-center gap-2.5 text-sm text-gray-700">
            <input
              type="checkbox"
              checked={form.sameDay}
              onChange={(e) => setField("sameDay", e.target.checked)}
              className="h-4 w-4 rounded border-gray-300 accent-forest-700"
            />
            당일 일정 (종료 일시 자동 설정)
          </label>
        </div>

        {/* 종료 일시 */}
        <div className="flex flex-col gap-1.5">
          <label
            className={`text-base font-medium ${
              endDisabled ? "text-gray-400" : "text-gray-700"
            }`}
          >
            종료 일시 {endDisabled ? "" : "*"}
          </label>
          <div className="grid grid-cols-[1fr_4rem_4rem] gap-2">
            <input
              type="date"
              value={form.endDate}
              disabled={endDisabled}
              onChange={(e) => setField("endDate", e.target.value)}
              className="w-full rounded-xl border border-gray-300 px-3 py-3 text-base text-gray-900 focus:border-forest-700 focus:outline-none focus:ring-2 focus:ring-forest-700/20 disabled:bg-gray-50 disabled:text-gray-400"
            />
            <input
              type="text"
              inputMode="numeric"
              pattern="\d*"
              maxLength={2}
              placeholder="시"
              disabled={endDisabled}
              value={form.endHour}
              onChange={digitsOnly((v) => setField("endHour", v), 23)}
              className="w-full rounded-xl border border-gray-300 px-2 py-3 text-center text-base text-gray-900 placeholder-gray-400 focus:border-forest-700 focus:outline-none focus:ring-2 focus:ring-forest-700/20 disabled:bg-gray-50 disabled:text-gray-400"
              aria-label="종료 시"
            />
            <input
              type="text"
              inputMode="numeric"
              pattern="\d*"
              maxLength={2}
              placeholder="분"
              disabled={endDisabled}
              value={form.endMinute}
              onChange={digitsOnly((v) => setField("endMinute", v), 59)}
              className="w-full rounded-xl border border-gray-300 px-2 py-3 text-center text-base text-gray-900 placeholder-gray-400 focus:border-forest-700 focus:outline-none focus:ring-2 focus:ring-forest-700/20 disabled:bg-gray-50 disabled:text-gray-400"
              aria-label="종료 분"
            />
          </div>
        </div>

        <Input
          label="정원"
          name="capacity"
          type="text"
          inputMode="numeric"
          value={form.capacity}
          onChange={digitsOnly((v) => setField("capacity", v))}
          placeholder="10"
        />

        <Input
          label="참여 포인트"
          name="pointCost"
          type="text"
          inputMode="numeric"
          value={form.pointCost}
          onChange={digitsOnly((v) => setField("pointCost", v))}
          placeholder="0"
        />

        {error && (
          <div
            className="rounded-xl border border-gray-200 bg-gray-50 p-4 text-base text-gray-900"
            role="alert"
          >
            {error}
          </div>
        )}

        <Button type="submit" variant="primary" fullWidth loading={createMutation.isPending}>
          이벤트 등록하기
        </Button>
      </form>
    </UserAuthGuard>
  );
}
