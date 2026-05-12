import { uGet, uPut, uPost } from "./user-client";
import type { MeProfile } from "@/lib/types-user";

export async function getMe(): Promise<MeProfile> {
  return uGet<MeProfile>("/api/v1/me");
}

export async function changePassword(data: {
  oldPassword: string;
  newPassword: string;
}): Promise<void> {
  return uPut<void>("/api/v1/me/password", data);
}

export async function switchRole(newRole: string): Promise<void> {
  return uPost<void>("/api/v1/me/role-switch", { newRole });
}
