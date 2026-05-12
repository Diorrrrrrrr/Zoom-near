import { uGet, uPost } from "./user-client";
import type { ApprovalItem } from "@/lib/types-user";

export async function getApprovals(params?: {
  status?: string;
  limit?: number;
}): Promise<{ items: ApprovalItem[] }> {
  const qs = new URLSearchParams();
  if (params?.status) qs.set("status", params.status);
  if (params?.limit != null) qs.set("limit", String(params.limit));
  const query = qs.toString() ? `?${qs.toString()}` : "";
  return uGet<{ items: ApprovalItem[] }>(`/api/v1/approvals/me${query}`);
}

export async function approveItem(id: string): Promise<ApprovalItem> {
  return uPost<ApprovalItem>(`/api/v1/approvals/${id}/approve`, {});
}

export async function rejectItem(id: string): Promise<ApprovalItem> {
  return uPost<ApprovalItem>(`/api/v1/approvals/${id}/reject`, {});
}
