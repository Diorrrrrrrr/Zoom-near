import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  getLinkages,
  getPendingOutgoingLinkages,
  createLinkage,
  deleteLinkage,
  searchTuntun,
} from "@/lib/api/linkages";
import { createInvite } from "@/lib/api/invites";

export function useLinkages() {
  return useQuery({
    queryKey: ["linkages"],
    queryFn: getLinkages,
  });
}

export function usePendingOutgoingLinkages() {
  return useQuery({
    queryKey: ["linkages", "pending-outgoing"],
    queryFn: getPendingOutgoingLinkages,
  });
}

export function useSearchTuntun(uniqueCode: string) {
  return useQuery({
    queryKey: ["tuntun-search", uniqueCode],
    queryFn: () => searchTuntun(uniqueCode),
    enabled: uniqueCode.length === 6,
  });
}

export function useCreateLinkage() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: { tuntunId: string; isPrimary: boolean }) =>
      createLinkage(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["linkages"] });
    },
  });
}

export function useDeleteLinkage() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => deleteLinkage(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["linkages"] });
    },
  });
}

export function useCreateInvite() {
  return useMutation({
    mutationFn: createInvite,
  });
}
