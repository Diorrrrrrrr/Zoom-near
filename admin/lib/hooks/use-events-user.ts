import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  listEvents,
  getEvent,
  createEvent,
  joinEvent,
  cancelEvent,
  listMyEvents,
  updateEvent,
  deleteEvent,
  type EventListParams,
  type CreateEventRequest,
  type UpdateEventRequest,
} from "@/lib/api/user-events";

export function useEvents(params?: EventListParams) {
  return useQuery({
    queryKey: ["events", params],
    queryFn: () => listEvents(params),
  });
}

export function useEvent(id: string) {
  return useQuery({
    queryKey: ["event", id],
    queryFn: () => getEvent(id),
    enabled: !!id,
  });
}

export function useCreateEvent() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateEventRequest) => createEvent(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["events"] });
    },
  });
}

export function useJoinEvent(eventId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (proxiedTuntunId?: string) => joinEvent(eventId, proxiedTuntunId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["event", eventId] });
      queryClient.invalidateQueries({ queryKey: ["me"] });
    },
  });
}

export function useCancelEvent(eventId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => cancelEvent(eventId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["event", eventId] });
      queryClient.invalidateQueries({ queryKey: ["events"] });
      queryClient.invalidateQueries({ queryKey: ["me"] });
    },
  });
}

export function useMyEvents(params?: { page?: number; size?: number }) {
  return useQuery({
    queryKey: ["events", "mine", params],
    queryFn: () => listMyEvents(params),
  });
}

export function useUpdateEvent(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: UpdateEventRequest) => updateEvent(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["events"] });
      queryClient.invalidateQueries({ queryKey: ["event", id] });
      queryClient.invalidateQueries({ queryKey: ["events", "mine"] });
    },
  });
}

export function useDeleteEvent(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (reason: string) => deleteEvent(id, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["events"] });
      queryClient.invalidateQueries({ queryKey: ["events", "mine"] });
    },
  });
}
