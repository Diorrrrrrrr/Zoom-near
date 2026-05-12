/**
 * 사용자 인터페이스에 노출되는 enum/코드 → 한국어 라벨 매핑.
 * 백엔드가 영문 enum을 보내도 사용자는 한국어로만 보게 한다.
 */

const CATEGORY_LABELS: Record<string, string> = {
  SPORTS: "스포츠",
  BOOK: "독서모임",
  CULTURE: "문화·예술",
  VOLUNTEER: "봉사",
  EDUCATION: "교육·강의",
  FOOD: "음식·카페",
  WALK: "산책·등산",
  ETC: "기타",
};

export function formatCategory(value?: string | null): string {
  if (!value) return "";
  return CATEGORY_LABELS[value] ?? value;
}

const ROLE_LABELS: Record<string, string> = {
  TUNTUN: "튼튼이",
  DUNDUN: "든든이",
  MANAGER: "매니저",
  ADMIN: "관리자",
};

export function formatRole(value?: string | null): string {
  if (!value) return "";
  return ROLE_LABELS[value] ?? value;
}

const EVENT_STATUS_LABELS: Record<string, string> = {
  OPEN: "모집중",
  CLOSED: "마감",
  CANCELLED: "취소됨",
  CANCELED: "취소됨",
};

export function formatEventStatus(value?: string | null): string {
  if (!value) return "";
  return EVENT_STATUS_LABELS[value] ?? value;
}

/// 한국어 날짜 (예: 2026년 5월 18일 (일))
export function formatKoreanDate(iso: string): string {
  const d = new Date(iso);
  const days = ["일", "월", "화", "수", "목", "금", "토"];
  const y = d.getFullYear();
  const m = d.getMonth() + 1;
  const day = d.getDate();
  return `${y}년 ${m}월 ${day}일 (${days[d.getDay()]})`;
}

/// 짧은 날짜 + 요일 (예: 5월 18일 (일))
export function formatShortKoreanDate(iso: string): string {
  const d = new Date(iso);
  const days = ["일", "월", "화", "수", "목", "금", "토"];
  return `${d.getMonth() + 1}월 ${d.getDate()}일 (${days[d.getDay()]})`;
}

/// 한국어 시간 (예: 오전 9:00 / 오후 3:30)
export function formatKoreanTime(iso: string): string {
  const d = new Date(iso);
  const h = d.getHours();
  const m = d.getMinutes();
  const ampm = h < 12 ? "오전" : "오후";
  const hour12 = h === 0 ? 12 : h > 12 ? h - 12 : h;
  return `${ampm} ${hour12}:${String(m).padStart(2, "0")}`;
}

/// 날짜 + 시간 한 줄 (예: 5월 18일 (일) 오전 9:00)
export function formatShortKoreanDateTime(iso: string): string {
  return `${formatShortKoreanDate(iso)} ${formatKoreanTime(iso)}`;
}

/// 시간 범위 (예: 오전 9:00 ~ 오전 11:00)
export function formatKoreanTimeRange(startIso: string, endIso: string): string {
  return `${formatKoreanTime(startIso)} ~ ${formatKoreanTime(endIso)}`;
}
