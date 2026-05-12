import { uGet, uPost } from "./user-client";
import type { BalanceResponse, LedgerItem, TopupResponse } from "@/lib/types-user";

export async function getBalance(): Promise<BalanceResponse> {
  return uGet<BalanceResponse>("/api/v1/points/me/balance");
}

export async function getLedger(params?: {
  limit?: number;
  offset?: number;
}): Promise<{ items: LedgerItem[] }> {
  const qs = new URLSearchParams();
  if (params?.limit != null) qs.set("limit", String(params.limit));
  if (params?.offset != null) qs.set("offset", String(params.offset));
  const query = qs.toString() ? `?${qs.toString()}` : "";
  return uGet<{ items: LedgerItem[] }>(`/api/v1/points/me/ledger${query}`);
}

export async function mockTopup(data: {
  amount: number;
  reasonText?: string;
}): Promise<TopupResponse> {
  return uPost<TopupResponse>("/api/v1/points/mock-topup", data);
}

export async function mockTopupProxy(data: {
  tuntunId: string;
  amount: number;
  reasonText?: string;
}): Promise<TopupResponse> {
  return uPost<TopupResponse>("/api/v1/points/mock-topup-proxy", data);
}
