import { uGet, uPost, uDel } from "./user-client";
import type {
  LinkageListItem,
  PendingLinkageItem,
  TuntunSearchResult,
} from "@/lib/types-user";

export async function searchTuntun(uniqueCode: string): Promise<TuntunSearchResult> {
  return uGet<TuntunSearchResult>(`/api/v1/users/search?uniqueCode=${encodeURIComponent(uniqueCode)}`);
}

export async function getLinkages(): Promise<LinkageListItem[]> {
  return uGet<LinkageListItem[]>("/api/v1/linkages/me");
}

export async function getPendingOutgoingLinkages(): Promise<PendingLinkageItem[]> {
  return uGet<PendingLinkageItem[]>("/api/v1/linkages/me/pending-outgoing");
}

export async function createLinkage(data: {
  tuntunId: string;
  isPrimary: boolean;
}): Promise<LinkageListItem> {
  return uPost<LinkageListItem>("/api/v1/linkages", data);
}

export async function deleteLinkage(id: string): Promise<void> {
  return uDel<void>(`/api/v1/linkages/${id}`);
}
