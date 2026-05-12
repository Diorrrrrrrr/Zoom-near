"use client";

import React, { useState } from "react";
import { TopBar } from "@/components/user-ui/top-bar";
import { Button } from "@/components/user-ui/button";
import { getUserRole } from "@/lib/auth/user-session";
import { applyForManager } from "@/lib/api/manager";

const FAQ = [
  {
    q: "포인트는 어떻게 충전하나요?",
    a: "내 정보 → 포인트 충전 메뉴에서 원하는 금액을 선택해 충전할 수 있어요.",
  },
  {
    q: "이벤트 참여를 취소할 수 있나요?",
    a: "이벤트 상세 페이지에서 '참여 취소하기' 버튼을 눌러 취소할 수 있습니다. 이벤트 시작 전까지 취소 가능해요.",
  },
  {
    q: "튼튼이와 든든이는 무엇인가요?",
    a: "튼튼이는 이벤트에 직접 참여하는 회원, 든든이는 튼튼이를 도와주는 보호자 역할이에요. 든든이는 연동 후 대리 충전·참여가 가능합니다.",
  },
  {
    q: "6자리 코드는 무엇인가요?",
    a: "회원마다 부여되는 고유 식별 코드예요. 든든이가 튼튼이와 연동할 때 사용합니다.",
  },
  {
    q: "매니저 신청은 어떻게 하나요?",
    a: "든든이만 신청 가능합니다. 도움말 페이지 하단의 '매니저 신청하기'를 눌러주세요. 검토 후 승인되면 이벤트 등록·운영 권한이 부여됩니다.",
  },
  {
    q: "비밀번호를 잊어버렸어요.",
    a: "현재 비밀번호 찾기 기능은 준비 중입니다. 관리자에게 문의해 주세요.",
  },
];

export default function HelpPage() {
  const role = getUserRole();
  const isDundun = role === "DUNDUN";

  const [showConfirm, setShowConfirm] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [toast, setToast] = useState<string | null>(null);

  function showToast(msg: string) {
    setToast(msg);
    setTimeout(() => setToast(null), 3000);
  }

  async function handleApply() {
    setSubmitting(true);
    try {
      await applyForManager({ reason: "든든이 매니저 신청" });
      setShowConfirm(false);
      showToast("매니저 신청이 접수됐어요. 검토 후 알려드릴게요.");
    } catch (err) {
      setShowConfirm(false);
      const msg = err instanceof Error ? err.message : "신청 중 오류가 발생했어요.";
      showToast(msg);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <TopBar title="도움말" showBack />
      <div className="px-4 py-6 space-y-4">
        <h2 className="text-xl font-bold text-gray-900">자주 묻는 질문</h2>
        <div className="space-y-3">
          {FAQ.map((item, idx) => (
            <div key={idx} className="rounded-2xl border border-gray-200 bg-white p-5">
              <p className="text-lg font-semibold text-gray-900">Q. {item.q}</p>
              <p className="mt-2 text-base leading-relaxed text-gray-700">{item.a}</p>
            </div>
          ))}
        </div>

        <div className="rounded-2xl border border-forest-100 bg-forest-50 p-5">
          <p className="text-base font-semibold text-forest-800">문의가 더 있으신가요?</p>
          <p className="mt-1 text-base text-forest-700">
            이메일: support@zoomnear.kr
            <br />
            운영 시간: 평일 09:00 ~ 18:00
          </p>

          {isDundun && (
            <button
              onClick={() => setShowConfirm(true)}
              className="mt-4 block w-full rounded-xl border border-forest-300 bg-white px-4 py-3 text-base font-semibold text-forest-800 hover:bg-forest-100"
            >
              매니저 신청하기
            </button>
          )}
        </div>
      </div>

      {/* 신청 확인 다이얼로그 */}
      {showConfirm && (
        <div
          className="fixed inset-0 z-[60] flex items-end bg-black/40 p-4"
          style={{ paddingBottom: "calc(1rem + env(safe-area-inset-bottom))" }}
        >
          <div className="w-full rounded-2xl bg-white p-6 space-y-4">
            <h3 className="text-xl font-bold text-gray-900">매니저 신청하시겠습니까?</h3>
            <p className="text-base text-gray-600">
              신청 후 운영팀 검토를 거쳐 승인되면 매니저 권한이 부여됩니다.
            </p>
            <div className="flex gap-3">
              <Button variant="secondary" fullWidth onClick={() => setShowConfirm(false)}>
                아니요
              </Button>
              <Button variant="primary" fullWidth onClick={handleApply} loading={submitting}>
                네, 신청할게요
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
    </>
  );
}
