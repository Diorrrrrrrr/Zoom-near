import { useQuery } from "@tanstack/react-query";
import { get } from "./client";
import type { Page, AuditLog } from "@/lib/types";

export interface AuditLogsParams {
  page?: number;
  size?: number;
  action?: string;
  actorId?: string;
  dateFrom?: string;
  dateTo?: string;
}

interface BackendAuditLogsResponse {
  items: AuditLog[];
  total: number;
  page: number;
  size: number;
}

/// 백엔드는 {items, total, page, size}로 반환 — Spring Page 형식으로 어댑팅.
export function useAuditLogs(params: AuditLogsParams = {}) {
  const searchParams = new URLSearchParams();
  if (params.page !== undefined) searchParams.set("page", String(params.page));
  if (params.size !== undefined) searchParams.set("size", String(params.size));
  if (params.action) searchParams.set("action", params.action);
  if (params.actorId) searchParams.set("actorId", params.actorId);
  if (params.dateFrom) searchParams.set("dateFrom", params.dateFrom);
  if (params.dateTo) searchParams.set("dateTo", params.dateTo);

  const query = searchParams.toString();

  return useQuery<Page<AuditLog>>({
    queryKey: ["audit-logs", params],
    queryFn: async () => {
      const raw = await get<BackendAuditLogsResponse>(
        `/api/v1/admin/audit-logs${query ? `?${query}` : ""}`
      );
      const size = raw?.size ?? params.size ?? 30;
      const total = raw?.total ?? 0;
      const totalPages = total > 0 ? Math.ceil(total / size) : 0;
      return {
        content: raw?.items ?? [],
        totalElements: total,
        totalPages,
        number: raw?.page ?? params.page ?? 0,
        size,
      } as Page<AuditLog>;
    },
  });
}
