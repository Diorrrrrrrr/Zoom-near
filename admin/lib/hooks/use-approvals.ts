import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { getApprovals, approveItem, rejectItem } from "@/lib/api/approvals";

export function useApprovals(params?: { status?: string; limit?: number }) {
  return useQuery({
    queryKey: ["approvals", params],
    queryFn: () => getApprovals(params),
  });
}

export function useApproveItem() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => approveItem(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["approvals"] });
    },
  });
}

export function useRejectItem() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => rejectItem(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["approvals"] });
    },
  });
}
