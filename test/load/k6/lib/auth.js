/**
 * k6 공통 인증 헬퍼 라이브러리
 *
 * 사용 예:
 *   import { login, signup, createUserPool } from './lib/auth.js';
 *
 *   export function setup() {
 *     const token = login('tuntun01', 'P@ssw0rd!');
 *     return { token };
 *   }
 */

import http from "k6/http";
import { check } from "k6";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";

/**
 * 로그인하여 accessToken을 반환한다.
 * 실패 시 null 반환 (호출측에서 처리).
 *
 * @param {string} loginId
 * @param {string} password
 * @returns {string|null} accessToken
 */
export function login(loginId, password) {
  const res = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ loginId, password }),
    { headers: { "Content-Type": "application/json" } }
  );

  const ok = check(res, {
    "login 200": (r) => r.status === 200,
  });

  if (!ok) {
    console.error(`login failed: loginId=${loginId} status=${res.status} body=${res.body}`);
    return null;
  }

  try {
    const body = JSON.parse(res.body);
    // API 응답 필드: accessToken (camelCase)
    return body.accessToken || body.access_token || null;
  } catch {
    console.error(`login response parse failed: ${res.body}`);
    return null;
  }
}

/**
 * 신규 사용자를 가입시키고 accessToken을 반환한다.
 * setup() 단계에서 fixture 생성용으로 사용.
 *
 * @param {string} loginId
 * @param {string} password
 * @param {string} phone
 * @param {string} name
 * @param {string} role  "TUNTUN" | "DUNDUN"
 * @param {string|null} inviteToken
 * @returns {string|null} accessToken
 */
export function signup(loginId, password, phone, name, role, inviteToken = null) {
  const payload = { loginId, password, phone, name, role };
  if (inviteToken) {
    payload.inviteToken = inviteToken;
  }

  const res = http.post(
    `${BASE_URL}/api/v1/auth/signup`,
    JSON.stringify(payload),
    { headers: { "Content-Type": "application/json" } }
  );

  const ok = check(res, {
    "signup 201": (r) => r.status === 201,
  });

  if (!ok) {
    console.error(`signup failed: loginId=${loginId} status=${res.status} body=${res.body}`);
    return null;
  }

  try {
    const body = JSON.parse(res.body);
    return body.accessToken || body.access_token || null;
  } catch {
    return null;
  }
}

/**
 * TUNTUN 사용자 풀을 생성한다.
 * setup() 단계에서 1회 호출. 각 VU가 고유 계정을 사용하도록 배열 반환.
 *
 * @param {number} count  생성할 사용자 수
 * @param {string} prefix loginId 접두사 (기본: "tuntun_load_")
 * @param {string} password 공통 비밀번호
 * @returns {Array<{loginId: string, password: string, token: string}>}
 */
export function createUserPool(count, prefix = "tuntun_load_", password = "P@ssw0rd1!") {
  const users = [];

  for (let i = 1; i <= count; i++) {
    const loginId = `${prefix}${String(i).padStart(3, "0")}`;
    const phone = `010-${String(Math.floor(1000 + i)).slice(-4)}-${String(Math.floor(1000 + i * 7)).slice(-4)}`;
    const name = `부하테스트${i}`;

    const token = signup(loginId, password, phone, name, "TUNTUN");
    if (token) {
      users.push({ loginId, password, token });
    } else {
      // 이미 존재하면 로그인 시도
      const existingToken = login(loginId, password);
      if (existingToken) {
        users.push({ loginId, password, token: existingToken });
      }
    }
  }

  console.log(`createUserPool: ${users.length}/${count} users ready`);
  return users;
}

/**
 * MANAGER 토큰을 발급한다.
 * 사전에 DB에 MANAGER 계정이 존재해야 한다.
 *
 * @returns {string|null} accessToken
 */
export function getManagerToken() {
  const managerLoginId = __ENV.MANAGER_LOGIN_ID || "manager01";
  const managerPassword = __ENV.MANAGER_PASSWORD || "ManagerP@ss1!";
  return login(managerLoginId, managerPassword);
}

/**
 * mock-topup으로 사용자에게 포인트를 부여한다.
 * setup() 단계에서 사용.
 *
 * @param {string} token   accessToken
 * @param {number} amount  충전 금액
 * @returns {boolean} 성공 여부
 */
export function topupPoints(token, amount) {
  const refId = `setup-topup-${Date.now()}-${Math.random().toString(36).slice(2)}`;
  const res = http.post(
    `${BASE_URL}/api/v1/points/mock-topup`,
    JSON.stringify({ amount, referenceId: refId }),
    {
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
    }
  );

  return res.status === 200 || res.status === 201;
}
