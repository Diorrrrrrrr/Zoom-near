import { uGet, uPost } from "./user-client";
import type { NotificationItem } from "@/lib/types-user";

export async function getNotifications(params?: {
  unread?: boolean;
  limit?: number;
  offset?: number;
}): Promise<{ items: NotificationItem[] }> {
  const qs = new URLSearchParams();
  if (params?.unread != null) qs.set("unread", String(params.unread));
  if (params?.limit != null) qs.set("limit", String(params.limit));
  if (params?.offset != null) qs.set("offset", String(params.offset));
  const query = qs.toString() ? `?${qs.toString()}` : "";
  return uGet<{ items: NotificationItem[] }>(`/api/v1/notifications${query}`);
}

export async function markNotificationRead(id: string): Promise<void> {
  return uPost<void>(`/api/v1/notifications/${id}/read`, {});
}
