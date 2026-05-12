import React from "react";
import { TopBar } from "@/components/user-ui/top-bar";

export default function TermsPage() {
  return (
    <>
      <TopBar title="이용약관" showBack />
      <div className="px-4 py-6 space-y-4">
        <h2 className="text-xl font-bold text-gray-900">ZOOM NEAR 이용약관</h2>
        <div className="prose prose-base max-w-none text-gray-700 space-y-4">
          <section>
            <h3 className="text-lg font-semibold text-gray-900">제1조 (목적)</h3>
            <p>이 약관은 ZOOM NEAR 서비스의 이용 조건 및 절차, 회사와 회원 간의 권리·의무 및 책임사항을 규정함을 목적으로 합니다.</p>
          </section>
          <section>
            <h3 className="text-lg font-semibold text-gray-900">제2조 (서비스 이용)</h3>
            <p>회원은 본 약관에 동의하고 서비스에 가입함으로써 이벤트 참여, 포인트 사용 등의 서비스를 이용할 수 있습니다.</p>
          </section>
          <section>
            <h3 className="text-lg font-semibold text-gray-900">제3조 (개인정보 보호)</h3>
            <p>회사는 관련 법령에 따라 회원의 개인정보를 보호합니다. 자세한 내용은 개인정보처리방침을 확인해 주세요.</p>
          </section>
          <section>
            <h3 className="text-lg font-semibold text-gray-900">제4조 (포인트)</h3>
            <p>포인트는 서비스 내에서만 사용 가능하며 현금으로 환불되지 않습니다. 포인트의 유효기간은 마지막 사용일로부터 1년입니다.</p>
          </section>
        </div>
        <p className="text-sm text-gray-400">최종 업데이트: 2026년 1월 1일</p>
      </div>
    </>
  );
}
