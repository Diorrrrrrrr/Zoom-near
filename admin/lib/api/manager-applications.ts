import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { get, post } from "./client";
import type { ManagerApplication, Page } from "@/lib/types";

export interface ManagerApplicationsParams {
  status?: string;
}

/// 백엔드는 Spring Page를 반환하지만 화면은 단순 배열을 기대 → content 추출.
export function useManagerApplications(params: ManagerApplicationsParams = {}) {
  const searchParams = new URLSearchParams();
  if (params.status) searchParams.set("status", params.status);

  const query = searchParams.toString();

  return useQuery<ManagerApplication[]>({
    queryKey: ["manager-applications", params],
    queryFn: async () => {
      const raw = await get<Page<ManagerApplication> | ManagerApplication[]>(
        `/api/v1/admin/manager-applications${query ? `?${query}` : ""}`
      );
      // Spring Page 형식이면 content 추출, 이미 배열이면 그대로
      if (Array.isArray(raw)) return raw;
      return (raw?.content ?? []) as ManagerApplication[];
    },
  });
}

export function useApproveApplication() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number | string) =>
      post<void>(`/api/v1/admin/manager-applications/${id}/approve`, {}),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["manager-applications"] });
    },
  });
}

export function useRejectApplication() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, reason }: { id: number | string; reason: string }) =>
      post<void>(`/api/v1/admin/manager-applications/${id}/reject`, { reason }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["manager-applications"] });
    },
  });
}
