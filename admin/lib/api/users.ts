import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { get, post } from "./client";
import type { Page, User } from "@/lib/types";

export interface UsersParams {
  status?: string;
  q?: string;
  page?: number;
  size?: number;
}

export function useUsers(params: UsersParams = {}) {
  const searchParams = new URLSearchParams();
  if (params.status) searchParams.set("status", params.status);
  if (params.q) searchParams.set("q", params.q);
  if (params.page !== undefined) searchParams.set("page", String(params.page));
  if (params.size !== undefined) searchParams.set("size", String(params.size));

  const query = searchParams.toString();

  return useQuery<Page<User>>({
    queryKey: ["users", params],
    queryFn: () => get<Page<User>>(`/api/v1/admin/users${query ? `?${query}` : ""}`),
  });
}

export function useSuspendUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) =>
      post<void>(`/api/v1/admin/users/${id}/suspend`, { reason }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["users"] });
    },
  });
}

export function useActivateUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) =>
      post<void>(`/api/v1/admin/users/${id}/activate`, {}),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["users"] });
    },
  });
}

export type ChangeableRole = "TUNTUN" | "DUNDUN" | "MANAGER";

export function useChangeUserRole() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, role }: { id: string; role: ChangeableRole }) =>
      post<void>(`/api/v1/admin/users/${id}/role`, { role }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["users"] });
    },
  });
}
