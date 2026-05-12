import { redirect } from "next/navigation";

/**
 * 루트 경로(/) → /dashboard 로 redirect
 */
export default function RootPage() {
  redirect("/login");
}
