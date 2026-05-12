import React from "react";
import { TopBar } from "@/components/user-ui/top-bar";

export default function PrivacyPage() {
  return (
    <>
      <TopBar title="개인정보처리방침" showBack />
      <div className="px-4 py-6 space-y-4">
        <h2 className="text-xl font-bold text-gray-900">개인정보처리방침</h2>
        <div className="space-y-4 text-base text-gray-700">
          <section>
            <h3 className="text-lg font-semibold text-gray-900">수집하는 개인정보</h3>
            <p>회원가입 시 이름, 아이디, 비밀번호, 휴대폰 번호, 이메일(선택)을 수집합니다.</p>
          </section>
          <section>
            <h3 className="text-lg font-semibold text-gray-900">개인정보 이용 목적</h3>
            <p>수집된 개인정보는 서비스 제공, 본인 확인, 고객 지원 목적으로만 사용됩니다.</p>
          </section>
          <section>
            <h3 className="text-lg font-semibold text-gray-900">개인정보 보유 기간</h3>
            <p>회원 탈퇴 시 즉시 삭제되며, 관련 법령에 의해 보존이 필요한 경우 해당 기간 동안 보관됩니다.</p>
          </section>
          <section>
            <h3 className="text-lg font-semibold text-gray-900">개인정보 제3자 제공</h3>
            <p>회원의 동의 없이 개인정보를 제3자에게 제공하지 않습니다.</p>
          </section>
        </div>
        <p className="text-sm text-gray-400">최종 업데이트: 2026년 1월 1일</p>
      </div>
    </>
  );
}
