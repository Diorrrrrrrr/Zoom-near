/**
 * k6 부하 테스트 — mock 충전 idempotency 검증
 *
 * 목표: 동일 사용자가 같은 referenceId로 100회 충전 시도 →
 *       ledger UNIQUE 제약으로 단 1건만 적립되는지 검증
 *
 * 실행:
 *   BASE_URL=http://localhost:8080 k6 run test/load/k6/mock_topup_idempotency.js
 *
 * 환경변수:
 *   BASE_URL   (기본: http://localhost:8080)
 *   TOPUP_AMOUNT (기본: 5000)
 */

import http from "k6/http";
import { check } from "k6";
import { Counter, Rate } from "k6/metrics";
import { signup, login } from "./lib/auth.js";

// ── 환경 변수 ──────────────────────────────────────────────
const BASE_URL    = __ENV.BASE_URL    || "http://localhost:8080";
const TOPUP_AMOUNT = parseInt(__ENV.TOPUP_AMOUNT || "5000");

// ── 커스텀 메트릭 ──────────────────────────────────────────
const duplicateRejected  = new Counter("duplicate_topup_rejected"); // 409 누적
const firstSuccessCount  = new Counter("first_topup_success");      // 200 누적
const conflictRate       = new Rate("idempotency_conflict_rate");   // 409 비율
const unexpectedStatus   = new Counter("unexpected_status");        // 200/409 외 응답

// ── 시나리오 옵션 ──────────────────────────────────────────
export const options = {
  scenarios: {
    idempotency_flood: {
      executor: "shared-iterations",
      vus: 20,
      iterations: 100, // 동일 referenceId로 100회 시도
      maxDuration: "30s",
    },
  },

  thresholds: {
    // p95 응답 500ms 이내
    http_req_duration: ["p(95)<500"],
    // 전체 check 통과율 95% 이상
    checks: ["rate>0.95"],
    // 409 비율: 100회 중 99회는 중복 거부 → 0.97 이상
    // (첫 성공 1회 + 나머지 99회 거부)
    idempotency_conflict_rate: ["rate>0.97"],
    // 5xx 오류 없음
    "http_req_failed": ["rate<0.01"],
  },
};

// ── setup: 테스트 시작 전 1회 실행 ─────────────────────────
export function setup() {
  console.log("=== idempotency setup 시작 ===");

  // 1. 테스트 전용 TUNTUN 계정 생성
  const loginId  = `idem_test_${Date.now()}`;
  const password = "P@ssw0rd1!";
  const phone    = "010-9999-0001";
  const name     = "멱등테스트";

  let token = signup(loginId, password, phone, name, "TUNTUN");
  if (!token) {
    // 이미 존재하면 로그인
    token = login(loginId, password);
  }

  if (!token) {
    console.error(`setup 실패: 테스트 계정 토큰 발급 불가 (loginId=${loginId})`);
    return { token: null, referenceId: null, amount: TOPUP_AMOUNT, loginId };
  }

  // 2. 고정 referenceId — 100회 전체에서 동일하게 사용
  const referenceId = `IDEM-TEST-${Date.now()}-FIXED`;

  // 3. 초기 잔액 기록 (검증용)
  const balanceRes = http.get(
    `${BASE_URL}/api/v1/points/me/balance`,
    { headers: { Authorization: `Bearer ${token}` } }
  );

  let initialBalance = 0;
  if (balanceRes.status === 200) {
    try {
      const body = JSON.parse(balanceRes.body);
      initialBalance = body.balance || body.amount || 0;
    } catch { /* ignore */ }
  }

  console.log(`setup 완료: loginId=${loginId}, referenceId=${referenceId}, initialBalance=${initialBalance}`);
  return { token, referenceId, amount: TOPUP_AMOUNT, loginId, initialBalance };
}

// ── default function: 각 iteration이 동일 referenceId로 충전 시도 ──
export default function (data) {
  const { token, amount, referenceId } = data;

  if (!token) {
    console.error("토큰 없음 — setup 실패");
    return;
  }

  const res = http.post(
    `${BASE_URL}/api/v1/points/mock-topup`,
    JSON.stringify({ amount, referenceId }),
    {
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
    }
  );

  // 첫 번째: 200(성공), 이후 99회: 409(CONFLICT — 중복 ledger)
  check(res, {
    "topup 200 or 409": (r) => r.status === 200 || r.status === 409,
  });

  if (res.status === 200) {
    firstSuccessCount.add(1);
    conflictRate.add(0); // 성공 = 중복 아님
  } else if (res.status === 409) {
    duplicateRejected.add(1);
    conflictRate.add(1); // 409 = 중복 거부
  } else {
    // 예상 외 응답 (500 등)
    unexpectedStatus.add(1);
    conflictRate.add(0);
    console.error(`예상 외 응답: status=${res.status} body=${res.body}`);
  }
}

// ── teardown: 잔액 및 ledger 검증 ─────────────────────────
export function teardown(data) {
  const { token, amount, referenceId, loginId, initialBalance } = data;
  console.log("=== idempotency teardown 검증 ===");
  console.log(`referenceId: ${referenceId}`);
  console.log(`amount: ${amount}`);

  if (!token) {
    console.error("teardown: 토큰 없어 잔액 검증 불가");
    return;
  }

  // 최종 잔액 확인 → initialBalance + amount(1회분)이어야 한다
  const balanceRes = http.get(
    `${BASE_URL}/api/v1/points/me/balance`,
    { headers: { Authorization: `Bearer ${token}` } }
  );

  if (balanceRes.status === 200) {
    try {
      const body = JSON.parse(balanceRes.body);
      const finalBalance = body.balance || body.amount || 0;
      const expected = initialBalance + amount;

      console.log(`최종 잔액: ${finalBalance} (기대: ${expected})`);

      if (finalBalance !== expected) {
        console.error(
          `잔액 불일치! finalBalance=${finalBalance} expected=${expected} ` +
          `→ 멱등성 위반 또는 PointController 미완`
        );
      } else {
        console.log("잔액 검증 통과: 100회 시도 중 1회만 적립됨");
      }
    } catch (e) {
      console.error(`잔액 응답 파싱 실패: ${balanceRes.body}`);
    }
  } else {
    // TODO(Day2-LaneQ): PointController GET /points/me/balance 완성 후 활성화
    console.warn(`잔액 조회 실패: status=${balanceRes.status} — PointController 미완`);
  }

  // ledger row count 검증
  // TODO(Day2-LaneQ): GET /api/v1/points/me/ledger 로 행 수 확인
  //   const ledgerRes = http.get(...);
  //   const rows = JSON.parse(ledgerRes.body).content.length;
  //   check({ rows }, { "ledger 1건만 존재": (d) => d.rows === 1 });

  console.log("기대: first_topup_success==1, duplicate_topup_rejected==99");
  console.log("teardown 완료");
}
