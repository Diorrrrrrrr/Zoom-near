"use client";

import React, { useMemo, useState } from "react";
import {
  useLinkages,
  usePendingOutgoingLinkages,
  useSearchTuntun,
  useCreateLinkage,
  useDeleteLinkage,
  useCreateInvite,
} from "@/lib/hooks/use-linkages";
import { TopBar } from "@/components/user-ui/top-bar";
import { Button } from "@/components/user-ui/button";
import { Card } from "@/components/user-ui/card";
import { UserAuthGuard } from "@/components/user-ui/user-auth-guard";
import { getUserRole } from "@/lib/auth/user-session";

export default function LinkagePage() {
  const role = getUserRole();
  const counterpartLabel = role === "TUNTUN" ? "든든이" : "튼튼이";

  const { data: linkages, isLoading } = useLinkages();
  const { data: pendingOutgoing } = usePendingOutgoingLinkages();
  const [code, setCode] = useState("");
  const searchQuery = useSearchTuntun(code);
  const createLinkage = useCreateLinkage();
  const deleteLinkage = useDeleteLinkage();
  const createInvite = useCreateInvite();
  const [toast, setToast] = useState<string | null>(null);
  const [deleteId, setDeleteId] = useState<string | null>(null);
  const [inviteUrl, setInviteUrl] = useState<string | null>(null);

  // 검색 결과 사용자가 이미 내가 보낸 대기중 요청 대상인지 빠르게 판별
  const pendingOtherIds = useMemo(
    () => new Set((pendingOutgoing ?? []).map((p) => p.otherUserId)),
    [pendingOutgoing],
  );

  function showToast(msg: string) {
    setToast(msg);
    setTimeout(() => setToast(null), 3000);
  }

  async function handleRequest() {
    if (!searchQuery.data) return;
    try {
      await createLinkage.mutateAsync({
        tuntunId: searchQuery.data.id,
        isPrimary: false,
      });
      setCode("");
      showToast(
        `${searchQuery.data.name}님에게 연동 요청을 보냈어요. 승인을 기다려 주세요.`,
      );
    } catch (err) {
      const raw = err instanceof Error ? err.message : "요청 중 오류가 발생했습니다.";
      // 백엔드 CONFLICT/FORBIDDEN 메시지를 사용자 친화 문구로 매핑
      const friendly = /이미 대기|이미 활성/.test(raw)
        ? raw.replace(/^\[\d+\][^—]*— /, "").replace(/^.*"message":"([^"]+)".*/, "$1")
        : `요청 중 오류가 발생했습니다.`;
      showToast(friendly);
    }
  }

  async function handleDelete(id: string) {
    try {
      await deleteLinkage.mutateAsync(id);
      setDeleteId(null);
      showToast("연동이 해제됐어요.");
    } catch (err) {
      showToast(err instanceof Error ? err.message : "해제 중 오류가 발생했습니다.");
    }
  }

  async function handleCreateInvite() {
    try {
      const result = await createInvite.mutateAsync();
      setInviteUrl(result.url);
    } catch (err) {
      showToast(
        err instanceof Error ? err.message : "초대 링크 생성 중 오류가 발생했습니다.",
      );
    }
  }

  async function handleShare() {
    if (!inviteUrl) return;
    if (navigator.share) {
      await navigator.share({ title: "ZOOM NEAR 초대", url: inviteUrl });
    } else {
      await navigator.clipboard.writeText(inviteUrl);
      showToast("초대 링크가 복사됐어요!");
    }
  }

  // 검색 결과가 이미 PENDING 대상이면 버튼 비활성화 + 라벨 변경
  const searchTarget = searchQuery.data;
  const searchTargetPending = searchTarget
    ? pendingOtherIds.has(searchTarget.id)
    : false;

  return (
    <UserAuthGuard allowedRoles={["TUNTUN", "DUNDUN"]}>
      <TopBar title="연동 관리" showBack />

      <div className="px-4 py-5 space-y-5">
        {/* 6자리 코드 검색 */}
        <Card>
          <p className="mb-1 text-lg font-bold text-gray-900">
            {counterpartLabel} 코드로 연동
          </p>
          <p className="mb-3 text-sm text-gray-500">
            상대방이 승인하면 연동이 시작됩니다.
          </p>
          <div className="flex gap-2">
            <input
              type="text"
              maxLength={6}
              value={code}
              onChange={(e) => setCode(e.target.value.toUpperCase())}
              placeholder="6자리 코드 입력"
              className="flex-1 rounded-xl border border-gray-300 px-4 py-3.5 text-lg text-gray-900 placeholder-gray-400 focus:border-forest-700 focus:outline-none focus:ring-2 focus:ring-forest-700/20 uppercase"
            />
          </div>

          {code.length === 6 && searchQuery.isLoading && (
            <p className="mt-2 text-base text-gray-400">검색 중…</p>
          )}
          {code.length === 6 && searchTarget && (
            <div className="mt-3 flex items-center justify-between rounded-xl bg-forest-50 p-3">
              <div className="min-w-0 flex-1">
                <p className="text-base font-semibold text-gray-900">
                  {searchTarget.name}
                </p>
                <p className="text-sm text-gray-500">
                  코드: {searchTarget.uniqueCode}
                </p>
              </div>
              {searchTargetPending ? (
                <button
                  disabled
                  className="h-10 shrink-0 rounded-xl border border-gray-200 bg-gray-100 px-4 text-sm font-semibold text-gray-500 cursor-not-allowed"
                >
                  승인 대기
                </button>
              ) : (
                <Button
                  variant="primary"
                  className="h-10 px-4 text-sm"
                  onClick={handleRequest}
                  loading={createLinkage.isPending}
                >
                  연동 요청
                </Button>
              )}
            </div>
          )}
          {code.length === 6 && searchQuery.isError && (
            <p className="mt-2 text-base text-gray-800">
              해당 코드의 {counterpartLabel}를 찾을 수 없어요.
            </p>
          )}
        </Card>

        {/* 초대 링크 (DUNDUN 만) */}
        {role === "DUNDUN" && (
          <Card>
            <p className="mb-3 text-lg font-bold text-gray-900">초대 링크 만들기</p>
            {inviteUrl ? (
              <div className="space-y-3">
                <p className="break-all rounded-xl bg-gray-50 p-3 text-sm text-gray-700">
                  {inviteUrl}
                </p>
                <Button variant="secondary" fullWidth onClick={handleShare}>
                  공유하기 / 복사
                </Button>
                <button
                  onClick={() => setInviteUrl(null)}
                  className="w-full text-center text-sm text-gray-400"
                >
                  새 링크 만들기
                </button>
              </div>
            ) : (
              <Button
                variant="secondary"
                fullWidth
                onClick={handleCreateInvite}
                loading={createInvite.isPending}
              >
                초대 링크 생성
              </Button>
            )}
          </Card>
        )}

        {/* 대기중인 요청 (내가 보낸) */}
        {pendingOutgoing && pendingOutgoing.length > 0 && (
          <div>
            <p className="mb-3 text-lg font-bold text-gray-900">
              연동 대기 중인 {counterpartLabel}
            </p>
            <div className="space-y-3">
              {pendingOutgoing.map((p) => (
                <Card key={p.approvalId}>
                  <div className="flex items-center justify-between">
                    <div className="min-w-0">
                      <p className="text-lg font-semibold text-gray-900">
                        {p.otherUserName}
                      </p>
                      <p className="text-sm text-gray-500">
                        코드: {p.otherUserUniqueCode}
                      </p>
                      <p className="mt-0.5 text-xs text-gray-400">
                        승인 대기 중 · 만료{" "}
                        {new Date(p.expiresAt).toLocaleString("ko-KR", {
                          month: "short",
                          day: "numeric",
                          hour: "2-digit",
                          minute: "2-digit",
                        })}
                      </p>
                    </div>
                    <span className="shrink-0 rounded-full border border-gray-200 bg-gray-50 px-3 py-1 text-xs font-semibold text-gray-600">
                      대기 중
                    </span>
                  </div>
                </Card>
              ))}
            </div>
          </div>
        )}

        {/* 연동 목록 */}
        <div>
          <p className="mb-3 text-lg font-bold text-gray-900">
            연동된 {counterpartLabel}
          </p>
          {isLoading && <p className="text-base text-gray-400">불러오는 중…</p>}
          {!isLoading && (!linkages || linkages.length === 0) && (
            <p className="text-base text-gray-400">
              아직 연동된 {counterpartLabel}가 없어요.
            </p>
          )}
          <div className="space-y-3">
            {linkages?.map((item) => (
              <Card key={item.id}>
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-lg font-semibold text-gray-900">
                      {item.otherUserName}
                    </p>
                    <p className="text-base text-gray-500">
                      코드: {item.otherUserUniqueCode}
                    </p>
                    {item.isPrimary && (
                      <span className="text-sm font-medium text-forest-700">
                        주 연동
                      </span>
                    )}
                  </div>
                  <button
                    onClick={() => setDeleteId(item.id)}
                    className="rounded-xl border border-gray-200 px-4 py-2 text-base text-gray-800 hover:bg-gray-50"
                  >
                    해제
                  </button>
                </div>
              </Card>
            ))}
          </div>
        </div>
      </div>

      {/* 해제 확인 */}
      {deleteId !== null && (
        <div
          className="fixed inset-0 z-[60] flex items-end bg-black/40 p-4"
          style={{ paddingBottom: "calc(1rem + env(safe-area-inset-bottom))" }}
        >
          <div className="w-full rounded-2xl bg-white p-6 space-y-4">
            <h3 className="text-xl font-bold text-gray-900">연동을 해제할까요?</h3>
            <p className="text-base text-gray-600">해제 후 다시 연동할 수 있습니다.</p>
            <div className="flex gap-3">
              <Button variant="secondary" fullWidth onClick={() => setDeleteId(null)}>
                취소
              </Button>
              <Button
                variant="danger"
                fullWidth
                onClick={() => handleDelete(deleteId)}
                loading={deleteLinkage.isPending}
              >
                해제하기
              </Button>
            </div>
          </div>
        </div>
      )}

      {toast && (
        <div className="fixed bottom-24 left-1/2 z-50 -translate-x-1/2 rounded-xl bg-gray-900 px-5 py-3 text-base text-white shadow-lg">
          {toast}
        </div>
      )}
    </UserAuthGuard>
  );
}
