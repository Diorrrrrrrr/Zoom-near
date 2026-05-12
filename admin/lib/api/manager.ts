import { uPost } from "./user-client";
import type { ManagerApplicationResponse } from "@/lib/types-user";

export async function applyForManager(data: {
  reason: string;
}): Promise<ManagerApplicationResponse> {
  return uPost<ManagerApplicationResponse>("/api/v1/manager/apply", data);
}
