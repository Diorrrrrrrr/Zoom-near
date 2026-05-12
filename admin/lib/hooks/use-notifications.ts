import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { getNotifications, markNotificationRead } from "@/lib/api/notifications";

export function useNotifications(params?: {
  unread?: boolean;
  limit?: number;
  offset?: number;
}) {
  return useQuery({
    queryKey: ["notifications", params],
    queryFn: () => getNotifications(params),
    refetchInterval: 30_000, // 30초 polling
  });
}

export function useMarkRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => markNotificationRead(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["notifications"] });
    },
  });
}
