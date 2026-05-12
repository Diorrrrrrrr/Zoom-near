import Link from "next/link";

export default function ForbiddenPage() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-6 p-8 text-center">
      <p className="text-6xl font-bold text-red-400">403</p>
      <h1 className="text-2xl font-bold text-gray-800">접근 권한이 없습니다</h1>
      <p className="text-base text-gray-500">
        이 페이지는 ADMIN 역할만 접근할 수 있습니다.
      </p>
      <Link
        href="/login"
        className="rounded-lg bg-brand-500 px-6 py-3 text-base font-semibold text-white hover:bg-blue-600"
      >
        로그인 페이지로 이동
      </Link>
    </main>
  );
}
