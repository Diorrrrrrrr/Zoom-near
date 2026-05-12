/** 공통 페이지네이션 래퍼 */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // 현재 페이지 (0-based)
  size: number;
}

/** 로그인 응답 */
export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  userId: string;
  role: string;
}

/** 회원 */
export interface User {
  id: string;
  loginId: string;
  name: string;
  role: "TUNTUN" | "DUNDUN" | "MANAGER" | "ADMIN";
  status: "ACTIVE" | "SUSPENDED" | "WITHDRAWN";
  uniqueCode: string;
  createdAt: string; // ISO 8601
}

/** 소셜 이벤트 */
export interface SocialEvent {
  id: string;
  title: string;
  regionText: string;
  status: "OPEN" | "CLOSED" | "CANCELLED";
  capacity: number;
  joinedCount: number;
  pointCost: number;
  startsAt: string; // ISO 8601
  managerProgram?: boolean;
}

/** 감사 로그 */
export interface AuditLog {
  id: number;
  createdAt: string;
  actorId: number;
  action: string;
  targetType: string;
  targetId: number | null;
  status: "SUCCESS" | "FAILED";
  payload: Record<string, unknown> | null;
}

/** 매니저 신청 */
export interface ManagerApplication {
  id: number;
  applicant: {
    id: number;
    name: string;
    loginId: string;
  };
  reason: string;
  status: "PENDING" | "APPROVED" | "REJECTED";
  createdAt: string;
}
