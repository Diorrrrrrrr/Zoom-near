import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { get, post, patch, del } from "./client";
import type { Page, SocialEvent } from "@/lib/types";
import type { UpdateEventRequest } from "@/lib/api/user-events";

export interface EventsParams {
  status?: string;
  page?: number;
  size?: number;
}

export function useEvents(params: EventsParams = {}) {
  const searchParams = new URLSearchParams();
  if (params.status) searchParams.set("status", params.status);
  if (params.page !== undefined) searchParams.set("page", String(params.page));
  if (params.size !== undefined) searchParams.set("size", String(params.size));

  const query = searchParams.toString();

  return useQuery<Page<SocialEvent>>({
    queryKey: ["events", params],
    queryFn: () =>
      get<Page<SocialEvent>>(`/api/v1/admin/events${query ? `?${query}` : ""}`),
  });
}

export interface AdminCreateEventInput {
  title: string;
  description: string;
  regionText: string;
  category: string;
  startsAt: string;
  endsAt: string;
  capacity: number;
  pointCost: number;
}

/// 관리자/매니저 이벤트 생성. managerProgram=true 강제 (주니어 자체 프로그램).
export function useAdminCreateEvent() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: AdminCreateEventInput) =>
      post<SocialEvent>("/api/v1/events", { ...input, managerProgram: true }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["events"] });
    },
  });
}

export function useForceCloseEvent() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) =>
      post<void>(`/api/v1/admin/events/${id}/force-close`, { reason }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["events"] });
    },
  });
}

export function useAdminUpdateEvent() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      id,
      event,
      reason,
    }: {
      id: string;
      event: UpdateEventRequest;
      reason: string;
    }) => patch<SocialEvent>(`/api/v1/admin/events/${id}`, { event, reason }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["events"] });
    },
  });
}

export function useAdminDeleteEvent() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) =>
      del<void>(
        `/api/v1/admin/events/${id}?reason=${encodeURIComponent(reason)}`
      ),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["events"] });
    },
  });
}
