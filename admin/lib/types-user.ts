/** 사용자 역할 */
export type UserRole = "TUNTUN" | "DUNDUN" | "MANAGER" | "ADMIN";

/** 내 프로필 */
export interface MeProfile {
  id: string;
  loginId: string;
  name: string;
  role: UserRole;
  uniqueCode: string;
  balance: number;
  rankCode: string;
  rankDisplayName: string;
}

/** 이벤트 요약 (목록) */
export interface EventSummary {
  id: string;
  title: string;
  regionText: string;
  category: string;
  status: "OPEN" | "CLOSED" | "CANCELLED";
  capacity: number;
  /** 목록 응답에는 없을 수 있고, 상세는 currentJoinedCount 로 옴 */
  joinedCount?: number;
  pointCost: number;
  startsAt: string;
  endsAt: string;
  managerProgram: boolean;
}

/** 이벤트 상세 (백엔드 EventDetailResponse 형상 기준) */
export interface EventDetail extends EventSummary {
  description: string;
  creatorId?: string;
  currentJoinedCount?: number;
  visibility?: string;
  createdAt?: string;
  updatedAt?: string;
  organizerName?: string;
  myParticipationStatus?: "JOINED" | "CANCELLED" | null;
}

/** 이벤트 참여 응답 */
export interface ParticipationResponse {
  participationId: string;
  status: string;
}

/** 포인트 원장 항목 (point_ledger.id 는 BIGSERIAL) */
export interface LedgerItem {
  id: number;
  amount: number;
  reason: string;
  createdAt: string;
  balanceAfter: number;
}

/** 포인트 잔액 */
export interface BalanceResponse {
  balance: number;
}

/** 충전 응답 (mock_topups.id 는 BIGSERIAL) */
export interface TopupResponse {
  newBalance: number;
  amount: number;
  reasonText?: string;
}

/** 연동 대상 사용자 검색 결과 */
export interface TuntunSearchResult {
  id: string;
  name: string;
  uniqueCode: string;
}

/** 내가 보낸 대기중 연동 요청 항목 */
export interface PendingLinkageItem {
  approvalId: string;
  otherUserId: string;
  otherUserName: string;
  otherUserLoginId: string;
  otherUserUniqueCode: string;
  createdAt: string;
  expiresAt: string;
}

/** 연동 목록 항목 (호출자 시점에서 상대 사용자 정보) */
export interface LinkageListItem {
  id: string;
  /** 상대 사용자 id (든든이 시점 → 튼튼이 id, 그 반대도 동일) */
  otherUserId: string;
  otherUserName: string;
  otherUserLoginId: string;
  otherUserUniqueCode: string;
  /** 든든이가 자신과 연동된 튼튼이의 잔액을 조회할 때만 채워짐 */
  otherUserBalance: number | null;
  isPrimary: boolean;
  /** 호출자 본인 역할 (TUNTUN | DUNDUN) */
  role: string;
}

/** 초대 링크 */
export interface InviteResponse {
  token: string;
  url: string;
  expiresAt: string;
}

/** 알림 (백엔드 NotificationItemResponse 와 동일 형상) */
export interface NotificationItem {
  id: string;
  type: string;
  title: string;
  body: string;
  payload?: Record<string, unknown> | null;
  /** ISO 문자열. null/undefined 이면 미열람 */
  readAt: string | null;
  createdAt: string;
}

/** 승인 항목 (백엔드 ApprovalListItem 와 동일 형상) */
export interface ApprovalItem {
  id: string;
  type: "EVENT_JOIN" | "EVENT_CANCEL" | "EVENT_CREATE" | "LINKAGE_CREATE";
  status: "PENDING" | "APPROVED" | "REJECTED" | "EXPIRED";
  requesterId: string;
  requesterName: string;
  requesterLoginId: string;
  /** payload 요약. 이벤트의 경우 제목, 연동의 경우 빈 문자열일 수 있음 */
  payloadSummary: string;
  expiresAt: string;
  createdAt: string;
}

/** 매니저 신청 */
export interface ManagerApplicationResponse {
  id: string;
  reason: string;
  status: "PENDING" | "APPROVED" | "REJECTED";
  createdAt: string;
}
