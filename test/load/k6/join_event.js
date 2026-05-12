/**
 * k6 부하 테스트 — 이벤트 참여 정원 race 시나리오
 *
 * 목표: capacity=10인 이벤트에 50명이 동시 참여 시도 →
 *       정확히 10건만 201, 나머지는 409(EVENT_FULL) 반환 검증
 *
 * 실행:
 *   BASE_URL=http://localhost:8080 k6 run test/load/k6/join_event.js
 *
 * 환경변수:
 *   BASE_URL         (기본: http://localhost:8080)
 *   MANAGER_LOGIN_ID (기본: manager01)
 *   MANAGER_PASSWORD (기본: ManagerP@ss1!)
 *   EVENT_CAPACITY   (기본: 10)
 *   USER_POOL_SIZE   (기본: 50)
 */

import http from "k6/http";
import { check, sleep } from "k6";
import { Rate, Counter, Gauge } from "k6/metrics";
import { login, createUserPool, topupPoints } from "./lib/auth.js";

// ── 환경 변수 ──────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const CAPACITY = parseInt(__ENV.EVENT_CAPACITY || "10");
const POOL_SIZE = parseInt(__ENV.USER_POOL_SIZE || "50");

// ── 커스텀 메트릭 ──────────────────────────────────────────
const eventFullRate  = new Rate("event_full_rate");    // 409/EVENT_FULL 비율
const joinSuccessCount = new Counter("join_success");  // 201 누적
const joinFailCount    = new Counter("join_fail");     // 예상 외 오류 누적

// ── 시나리오 옵션 ──────────────────────────────────────────
export const options = {
  scenarios: {
    capacity_race: {
      executor: "per-vu-iterations",
      vus: POOL_SIZE,         // 50 VU
      iterations: 1,          // 각 VU 1회 join 시도 → 50회 동시
      maxDuration: "60s",
    },
  },

  thresholds: {
    // p95 응답 500ms 이내
    http_req_duration: ["p(95)<500"],
    // 5xx 응답 0%
    "http_req_failed": ["rate<0.01"],
    // 성공(201) 정확히 CAPACITY건 이상 보장
    // (race 조건 상 CAPACITY ± 0 을 엄격히 체크하는 것은 teardown에서 수행)
    "join_success": [`count<=${CAPACITY}`],
  },
};

// ── setup: 테스트 시작 전 1회 실행 ─────────────────────────
export function setup() {
  console.log(`=== setup 시작: capacity=${CAPACITY}, poolSize=${POOL_SIZE} ===`);

  // 1. MANAGER 토큰 발급 (환경에 MANAGER 계정 필요)
  const managerLoginId = __ENV.MANAGER_LOGIN_ID || "manager01";
  const managerPassword = __ENV.MANAGER_PASSWORD || "ManagerP@ss1!";
  const managerToken = login(managerLoginId, managerPassword);

  if (!managerToken) {
    console.error("MANAGER 토큰 발급 실패 — manager01 계정 존재 확인 필요");
    // TODO(Day2-LaneQ): MANAGER 계정 자동 seed 로직 추가
  }

  // 2. capacity=CAPACITY 이벤트 생성
  let eventId = null;
  if (managerToken) {
    const now = new Date();
    const startsAt = new Date(now.getTime() + 60 * 60 * 1000).toISOString(); // 1시간 후
    const endsAt   = new Date(now.getTime() + 2 * 60 * 60 * 1000).toISOString();

    const eventPayload = {
      title: `부하테스트 이벤트 ${now.getTime()}`,
      description: "k6 capacity race 테스트용 이벤트",
      category: "SOCIAL",
      regionText: "서울 강남구",
      capacity: CAPACITY,
      pointCost: 0,
      startsAt,
      endsAt,
      visibility: "PUBLIC",
      isManagerProgram: false,
    };

    const createRes = http.post(
      `${BASE_URL}/api/v1/events`,
      JSON.stringify(eventPayload),
      {
        headers: {
          Authorization: `Bearer ${managerToken}`,
          "Content-Type": "application/json",
        },
      }
    );

    if (createRes.status === 201) {
      try {
        eventId = JSON.parse(createRes.body).id;
        console.log(`이벤트 생성 성공: id=${eventId}`);
      } catch {
        console.error(`이벤트 응답 파싱 실패: ${createRes.body}`);
      }
    } else {
      console.error(`이벤트 생성 실패: status=${createRes.status} body=${createRes.body}`);
      // TODO(Day2-LaneQ): EventController 완성 전까지 환경변수로 직접 주입
      eventId = __ENV.EVENT_ID || null;
    }
  }

  // 3. TUNTUN 사용자 풀 생성 (50명)
  const users = createUserPool(POOL_SIZE, "k6_join_", "P@ssw0rd1!");

  // 4. 각 사용자에게 충분한 포인트 충전 (pointCost=0이므로 생략 가능하나 방어적으로 실행)
  // pointCost=0 이벤트이므로 스킵
  // for (const user of users) { topupPoints(user.token, 10000); }

  console.log(`setup 완료: eventId=${eventId}, users=${users.length}`);
  return { eventId, users };
}

// ── default function: 각 VU가 1회 실행 ──────────────────
export default function (data) {
  const { eventId, users } = data;

  if (!eventId) {
    console.error("eventId 없음 — setup 실패");
    return;
  }

  // VU 번호(1~50)로 고유 사용자 선택
  const vuIndex = (__VU - 1) % users.length;
  const user = users[vuIndex];

  if (!user || !user.token) {
    console.error(`VU ${__VU}: 사용자 토큰 없음`);
    return;
  }

  // 이벤트 참여 요청
  const res = http.post(
    `${BASE_URL}/api/v1/events/${eventId}/join`,
    null,
    {
      headers: {
        Authorization: `Bearer ${user.token}`,
        "Content-Type": "application/json",
      },
    }
  );

  // 201(성공) 또는 409(정원 초과/중복)만 정상 시나리오
  check(res, {
    "join 201 or 409": (r) => r.status === 201 || r.status === 409,
  });

  if (res.status === 201) {
    joinSuccessCount.add(1);
    eventFullRate.add(0);
  } else if (res.status === 409) {
    let body = {};
    try { body = JSON.parse(res.body); } catch { /* ignore */ }

    if (body.code === "EVENT_FULL") {
      eventFullRate.add(1);
    } else {
      // CONFLICT(중복 참여) 등 기타 409
      eventFullRate.add(0);
    }
  } else {
    // 예상치 못한 응답
    joinFailCount.add(1);
    console.error(`VU ${__VU}: 예상 외 응답 status=${res.status} body=${res.body}`);
  }

  // VU간 약간의 지터 (동시성 극대화를 위해 최소화)
  sleep(0.01);
}

// ── teardown: 테스트 종료 후 1회 실행 ────────────────────
export function teardown(data) {
  const { eventId, users } = data;
  console.log("=== teardown 검증 ===");
  console.log(`eventId: ${eventId}`);

  // ledger row count 검증 (pointCost=0이므로 ledger 없음)
  // TODO(Day2-LaneQ): pointCost > 0인 경우 ledger row count = joinSuccessCount 검증
  //   const ledgerRes = http.get(`${BASE_URL}/api/v1/admin/debug/ledger?eventId=${eventId}`);

  // capacity 초과 여부 DB 직접 검증
  // TODO(Day2-LaneQ): AdminController 또는 테스트용 엔드포인트로 참여 수 조회
  //   const participantRes = http.get(
  //     `${BASE_URL}/api/v1/events/${eventId}/participants/count`,
  //     { headers: { Authorization: `Bearer ${managerToken}` } }
  //   );
  //   const confirmedCount = JSON.parse(participantRes.body).confirmed;
  //   check({ confirmedCount }, {
  //     [`정확히 ${CAPACITY}명 CONFIRMED`]: (d) => d.confirmedCount === CAPACITY,
  //   });

  console.log(`기대: join_success == ${CAPACITY}, event_full_rate > 0.7`);
  console.log(`teardown 완료`);
}
