import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { getMe, changePassword, switchRole } from "@/lib/api/me";

export function useMe() {
  return useQuery({
    queryKey: ["me"],
    queryFn: getMe,
  });
}

export function useChangePassword() {
  return useMutation({
    mutationFn: changePassword,
  });
}

export function useSwitchRole() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (newRole: string) => switchRole(newRole),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["me"] });
    },
  });
}
