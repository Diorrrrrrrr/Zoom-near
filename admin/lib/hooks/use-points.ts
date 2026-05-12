import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { getBalance, getLedger, mockTopup, mockTopupProxy } from "@/lib/api/points";

export function useBalance() {
  return useQuery({
    queryKey: ["balance"],
    queryFn: getBalance,
  });
}

export function useLedger(params?: { limit?: number; offset?: number }) {
  return useQuery({
    queryKey: ["ledger", params],
    queryFn: () => getLedger(params),
  });
}

export function useTopup() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: { amount: number; reasonText?: string }) =>
      mockTopup(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["balance"] });
      queryClient.invalidateQueries({ queryKey: ["ledger"] });
      queryClient.invalidateQueries({ queryKey: ["me"] });
    },
  });
}

export function useTopupProxy() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: { tuntunId: string; amount: number; reasonText?: string }) =>
      mockTopupProxy(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["balance"] });
    },
  });
}
