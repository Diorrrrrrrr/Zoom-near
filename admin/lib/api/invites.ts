import { uPost } from "./user-client";
import type { InviteResponse } from "@/lib/types-user";

export async function createInvite(): Promise<InviteResponse> {
  return uPost<InviteResponse>("/api/v1/invites", {});
}
