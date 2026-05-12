import { post } from "./client";
import type { TokenResponse } from "@/lib/types";

export interface LoginRequest {
  loginId: string;
  password: string;
}

export async function login(data: LoginRequest): Promise<TokenResponse> {
  return post<TokenResponse>("/api/v1/auth/login", data);
}
