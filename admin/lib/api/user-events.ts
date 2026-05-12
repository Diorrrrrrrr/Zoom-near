import { uGet, uPost, uPatch, uDelWithBody } from "./user-client";
import type {
  EventSummary,
  EventDetail,
  ParticipationResponse,
} from "@/lib/types-user";
import type { Page } from "@/lib/types";

export interface EventListParams {
  status?: string;
  q?: string;
  regionText?: string;
  page?: number;
  size?: number;
}

export async function listEvents(
  params?: EventListParams
): Promise<Page<EventSummary>> {
  const qs = new URLSearchParams();
  if (params?.status) qs.set("status", params.status);
  if (params?.q) qs.set("q", params.q);
  if (params?.regionText) qs.set("regionText", params.regionText);
  if (params?.page != null) qs.set("page", String(params.page));
  if (params?.size != null) qs.set("size", String(params.size));
  const query = qs.toString() ? `?${qs.toString()}` : "";
  return uGet<Page<EventSummary>>(`/api/v1/events${query}`);
}

export async function getEvent(id: string): Promise<EventDetail> {
  return uGet<EventDetail>(`/api/v1/events/${id}`);
}

export interface CreateEventRequest {
  title: string;
  description: string;
  regionText: string;
  category: string;
  startsAt: string;
  endsAt: string;
  capacity: number;
  pointCost: number;
  managerProgram: boolean;
}

export async function createEvent(
  data: CreateEventRequest
): Promise<EventDetail> {
  return uPost<EventDetail>("/api/v1/events", data);
}

export async function joinEvent(
  id: string,
  proxiedTuntunId?: string
): Promise<ParticipationResponse> {
  return uPost<ParticipationResponse>(`/api/v1/events/${id}/join`, {
    ...(proxiedTuntunId != null ? { proxiedTuntunId } : {}),
  });
}

export async function cancelEvent(id: string): Promise<void> {
  return uPost<void>(`/api/v1/events/${id}/cancel`, {});
}

export interface UpdateEventRequest {
  title?: string;
  description?: string;
  regionText?: string;
  category?: string;
  startsAt?: string;
  endsAt?: string;
  capacity?: number;
  pointCost?: number;
}

export async function listMyEvents(params?: {
  page?: number;
  size?: number;
}): Promise<Page<EventSummary>> {
  const qs = new URLSearchParams();
  if (params?.page != null) qs.set("page", String(params.page));
  if (params?.size != null) qs.set("size", String(params.size));
  const query = qs.toString() ? `?${qs.toString()}` : "";
  return uGet<Page<EventSummary>>(`/api/v1/events/mine${query}`);
}

export async function updateEvent(
  id: string,
  data: UpdateEventRequest
): Promise<EventDetail> {
  return uPatch<EventDetail>(`/api/v1/events/${id}`, data);
}

export async function deleteEvent(id: string, reason: string): Promise<void> {
  return uDelWithBody<void>(`/api/v1/events/${id}`, { reason });
}
