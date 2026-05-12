# 권한 매트릭스 테스트 시나리오

> **도메인 약어**
> - TUNTUN: 튼튼이(중장년/노인 시니어)
> - DUNDUN: 든든이(자녀/보호자)
> - MANAGER: 매니저(프로그램 운영자)
> - ADMIN: 관리자
> - LINKED: 해당 TUNTUN과 ACTIVE 연동 존재
> - UNLINKED: 연동 없음 또는 비활성
> - ANON: 비인증(토큰 없음)

---

## AUTH — 로그인 / 로그아웃 / 토큰

| # | 카테고리 | 액터(역할/연동상태) | 메서드 | 경로 | 페이로드/대상 요약 | 기대 응답 | 비고 |
|---|----------|---------------------|--------|------|--------------------|-----------|------|
| 1 | AUTH | ANON | POST | `/api/v1/auth/signup` | `{loginId, password, phone, name, role:TUNTUN}` 정상 | 201 | TUNTUN 가입 성공; unique_code 자동 발급 |
| 2 | AUTH | ANON | POST | `/api/v1/auth/signup` | `role:ADMIN` 포함 | 400 | 클라이언트에서 ADMIN 역할 직접 지정 불가 |
| 3 | AUTH | ANON | POST | `/api/v1/auth/login` | 올바른 loginId/password | 200 | access_token + refresh_token 반환 |
| 4 | AUTH | ANON | POST | `/api/v1/auth/login` | 잘못된 비밀번호 | 401 | UNAUTHORIZED |
| 5 | AUTH | TUNTUN | POST | `/api/v1/auth/logout` | 유효 refresh_token | 200 | refresh_token 무효화; 이후 재사용 시 401 |

---

## PROFILE — 프로필 조회·수정

| # | 카테고리 | 액터(역할/연동상태) | 메서드 | 경로 | 페이로드/대상 요약 | 기대 응답 | 비고 |
|---|----------|---------------------|--------|------|--------------------|-----------|------|
| 6 | PROFILE | TUNTUN | GET | `/api/v1/profile/me` | — | 200 | 본인 프로필 반환 |
| 7 | PROFILE | DUNDUN | PATCH | `/api/v1/profile/me` | `{name: "새이름"}` | 200 | 본인 프로필 수정 성공 |
| 8 | PROFILE | TUNTUN | GET | `/api/v1/profile/{otherId}` | 타인(DUNDUN) ID | 403 | 타인 프로필 직접 조회 금지 |
| 9 | PROFILE | ADMIN | GET | `/api/v1/admin/users/{userId}` | 임의 사용자 ID | 200 | ADMIN은 모든 프로필 조회 가능 |
| 10 | PROFILE | ANON | GET | `/api/v1/profile/me` | 토큰 없음 | 401 | 인증 필요 |

---

## LINKAGE — 연동 검색·요청·해제

| # | 카테고리 | 액터(역할/연동상태) | 메서드 | 경로 | 페이로드/대상 요약 | 기대 응답 | 비고 |
|---|----------|---------------------|--------|------|--------------------|-----------|------|
| 11 | LINKAGE | DUNDUN | GET | `/api/v1/linkage/search?code={6자리}` | 유효한 TUNTUN unique_code | 200 | 이름·마스킹 정보 반환 |
| 12 | LINKAGE | DUNDUN | GET | `/api/v1/linkage/search?code={6자리}` | 존재하지 않는 코드 | 404 | NOT_FOUND |
| 13 | LINKAGE | DUNDUN (연동0명) | POST | `/api/v1/linkage` | `{tuntunId}` 정상 | 201 | 즉시 ACTIVE 연동 생성 |
| 14 | LINKAGE | DUNDUN (연동4명) | POST | `/api/v1/linkage` | `{tuntunId}` 5번째 시도 | 409 | 든든이당 부모 최대 4명 초과 |
| 15 | LINKAGE | DUNDUN (LINKED) | POST | `/api/v1/linkage` | 이미 연동된 동일 tuntunId | 409 | CONFLICT — 중복 연동 |
| 16 | LINKAGE | DUNDUN | POST | `/api/v1/linkage` | `tuntunId = 본인 id` | 400 | 자기 자신과 연동 불가 |
| 17 | LINKAGE | TUNTUN | POST | `/api/v1/linkage` | TUNTUN이 연동 요청 시도 | 403 | TUNTUN은 연동 개시 불가 (DUNDUN 전용) |
| 18 | LINKAGE | DUNDUN (LINKED) | DELETE | `/api/v1/linkage/{tuntunId}` | 연동 해제 | 200 | status → INACTIVE |

---

## INVITE — 초대 토큰

| # | 카테고리 | 액터(역할/연동상태) | 메서드 | 경로 | 페이로드/대상 요약 | 기대 응답 | 비고 |
|---|----------|---------------------|--------|------|--------------------|-----------|------|
| 19 | INVITE | DUNDUN | POST | `/api/v1/invite/token` | — | 201 | invite_token 발급 (만료 24h) |
| 20 | INVITE | ANON | POST | `/api/v1/auth/signup-via-invite` | `{token, loginId, password, ...}` 유효 토큰 | 201 | 가입 즉시 DUNDUN-TUNTUN 연동 생성 |
| 21 | INVITE | ANON | POST | `/api/v1/auth/signup-via-invite` | 만료된 토큰 | 400 | 토큰 만료 — VALIDATION_FAILED |
| 22 | INVITE | ANON | POST | `/api/v1/auth/signup-via-invite` | 이미 사용된 토큰 | 409 | CONFLICT — 토큰 재사용 불가 |

---

## EVENT_VIEW — 이벤트 조회

| # | 카테고리 | 액터(역할/연동상태) | 메서드 | 경로 | 페이로드/대상 요약 | 기대 응답 | 비고 |
|---|----------|---------------------|--------|------|--------------------|-----------|------|
| 23 | EVENT_VIEW | TUNTUN | GET | `/api/v1/events` | 목록 조회 | 200 | 공개 이벤트 리스트 반환 |
| 24 | EVENT_VIEW | DUNDUN (LINKED) | GET | `/api/v1/events/{eventId}` | 연동된 TUNTUN 지역 이벤트 | 200 | 대리 참여 가능 이벤트 포함 |
| 25 | EVENT_VIEW | ANON | GET | `/api/v1/events/{eventId}` | 토큰 없음 | 401 | 비인증 접근 불가 |

---

## EVENT_CREATE — 이벤트 등록

| # | 카테고리 | 액터(역할/연동상태) | 메서드 | 경로 | 페이로드/대상 요약 | 기대 응답 | 비고 |
|---|----------|---------------------|--------|------|--------------------|-----------|------|
| 26 | EVENT_CREATE | TUNTUN | POST | `/api/v1/events` | `{title, capacity, fee, ...}` 정상 | 201 | TUNTUN 이벤트 등록 성공 |
| 27 | EVENT_CREATE | MANAGER | POST | `/api/v1/events` | `{is_manager_program: true, ...}` | 201 | 매니저 프로그램 등록 성공 |
| 28 | EVENT_CREATE | DUNDUN (UNLINKED) | POST | `/api/v1/events` | 대리 이벤트 등록 시도 | 403 | 비연동 DUNDUN은 이벤트 생성 불가 |
| 29 | EVENT_CREATE | ADMIN | POST | `/api/v1/events` | 임의 이벤트 | 201 | ADMIN 모든 이벤트 생성 가능 |

---

## EVENT_PARTICIPATE — 본인 참여

| # | 카테고리 | 액터(역할/연동상태) | 메서드 | 경로 | 페이로드/대상 요약 | 기대 응답 | 비고 |
|---|----------|---------------------|--------|------|--------------------|-----------|------|
| 30 | EVENT_PARTICIPATE | TUNTUN | POST | `/api/v1/events/{eventId}/join` | 잔액 충분, 정원 여유 | 201 | 참여 성공; 잔액 차감 |
| 31 | EVENT_PARTICIPATE | TUNTUN | POST | `/api/v1/events/{eventId}/join` | 정원 이미 초과 (capacity=0) | 409 | EVENT_FULL |
| 32 | EVENT_PARTICIPATE | TUNTUN | POST | `/api/v1/events/{eventId}/join` | 잔액 부족 (fee > balance) | 409 | INSUFFICIENT_POINTS |
| 33 | EVENT_PARTICIPATE | TUNTUN | POST | `/api/v1/events/{eventId}/join` | 이미 참여한 이벤트 재신청 | 409 | CONFLICT — 중복 참여 |
| 34 | EVENT_PARTICIPATE | TUNTUN | DELETE | `/api/v1/events/{eventId}/join` | 참여 취소 (환불 정책 내) | 200 | 참여 취소; 잔액 복원 |

---

## PROXY_PARTICIPATE — 든든이 대리 참여

| # | 카테고리 | 액터(역할/연동상태) | 메서드 | 경로 | 페이로드/대상 요약 | 기대 응답 | 비고 |
|---|----------|---------------------|--------|------|--------------------|-----------|------|
| 35 | PROXY_PARTICIPATE | DUNDUN (LINKED) | POST | `/api/v1/proxy/events/{eventId}/join` | `{tuntunId}` 연동됨 | 201 | approvals PENDING 생성; 튼튼이에게 알림 |
| 36 | PROXY_PARTICIPATE | DUNDUN (UNLINKED) | POST | `/api/v1/proxy/events/{eventId}/join` | `{tuntunId}` 비연동 | 403 | NOT_LINKED — 대리 불가 |
| 37 | PROXY_PARTICIPATE | TUNTUN | POST | `/api/v1/approvals/{approvalId}/approve` | 본인 approval | 200 | APPROVED; 잔액 차감 |
| 38 | PROXY_PARTICIPATE | TUNTUN | POST | `/api/v1/approvals/{approvalId}/reject` | 본인 approval | 200 | REJECTED; 이벤트 슬롯 복원 |
| 39 | PROXY_PARTICIPATE | TUNTUN | POST | `/api/v1/approvals/{approvalId}/approve` | 타인(다른 TUNTUN) approval | 403 | FORBIDDEN — 본인 승인만 가능 |
| 40 | PROXY_PARTICIPATE | DUNDUN (LINKED) | POST | `/api/v1/proxy/events/{eventId}/join` | 정원 마감 race (capacity=1, 동시 50건) | 409 | 1건만 201, 나머지 EVENT_FULL |

---

## POINT_TOPUP — 포인트 충전

| # | 카테고리 | 액터(역할/연동상태) | 메서드 | 경로 | 페이로드/대상 요약 | 기대 응답 | 비고 |
|---|----------|---------------------|--------|------|--------------------|-----------|------|
| 41 | POINT_TOPUP | TUNTUN | POST | `/api/v1/points/topup` | `{amount: 10000, referenceId: uuid}` 정상 | 200 | 잔액 증가; ledger INSERT |
| 42 | POINT_TOPUP | DUNDUN (LINKED) | POST | `/api/v1/proxy/points/topup` | `{tuntunId, amount: 5000}` 연동됨 | 201 | approvals PENDING; 튼튼이 확인 후 적립 |
| 43 | POINT_TOPUP | DUNDUN (UNLINKED) | POST | `/api/v1/proxy/points/topup` | `{tuntunId, amount: 5000}` 비연동 | 403 | NOT_LINKED |
| 44 | POINT_TOPUP | TUNTUN | POST | `/api/v1/points/topup` | `{amount: -1}` 음수 | 400 | VALIDATION_FAILED — 음수 금액 불가 |
| 45 | POINT_TOPUP | TUNTUN | POST | `/api/v1/points/topup` | `{amount: 0}` | 400 | VALIDATION_FAILED — 0원 불가 |

---

## MANAGER — 매니저 전용 권한

| # | 카테고리 | 액터(역할/연동상태) | 메서드 | 경로 | 페이로드/대상 요약 | 기대 응답 | 비고 |
|---|----------|---------------------|--------|------|--------------------|-----------|------|
| 46 | MANAGER | MANAGER | PATCH | `/api/v1/events/{ownEventId}` | 본인이 만든 이벤트 수정 | 200 | 본인 이벤트 수정 가능 |
| 47 | MANAGER | MANAGER | PATCH | `/api/v1/events/{otherManagerEventId}` | 다른 MANAGER가 만든 이벤트 수정 시도 | 403 | FORBIDDEN — 타 매니저 이벤트 수정 불가 |
| 48 | MANAGER | TUNTUN | POST | `/api/v1/events` | `{is_manager_program: true}` | 403 | TUNTUN은 매니저 프로그램 등록 불가 |

---

## ADMIN — 관리자 전용 권한

| # | 카테고리 | 액터(역할/연동상태) | 메서드 | 경로 | 페이로드/대상 요약 | 기대 응답 | 비고 |
|---|----------|---------------------|--------|------|--------------------|-----------|------|
| 49 | ADMIN | ADMIN | PATCH | `/api/v1/admin/users/{userId}/status` | `{status: SUSPENDED}` | 200 | 회원 강제 정지 |
| 50 | ADMIN | ADMIN | POST | `/api/v1/admin/events/{eventId}/close` | — | 200 | 이벤트 강제 종료 |
| 51 | ADMIN | ADMIN | GET | `/api/v1/admin/audit-logs` | `?from=2026-01-01&to=2026-01-31` | 200 | audit_logs 조회 |

---

## ROLE_SWITCH — 역할 전환

| # | 카테고리 | 액터(역할/연동상태) | 메서드 | 경로 | 페이로드/대상 요약 | 기대 응답 | 비고 |
|---|----------|---------------------|--------|------|--------------------|-----------|------|
| 52 | ROLE_SWITCH | DUNDUN (LINKED) | POST | `/api/v1/profile/switch-role` | `{targetRole: TUNTUN}` — 활성 연동 존재 | 409 | CONFLICT — 잔존 연동으로 전환 차단 |
| 53 | ROLE_SWITCH | DUNDUN | POST | `/api/v1/profile/switch-role` | `{targetRole: TUNTUN}` — 연동·결제 없음 | 200 | 전환 성공; 기존 든든이 토큰 무효화 |
| 54 | ROLE_SWITCH | TUNTUN | POST | `/api/v1/profile/switch-role` | `{targetRole: DUNDUN}` — 진행 중인 이벤트 참여 있음 | 409 | CONFLICT — 이벤트 진행 중 전환 차단 |

---

## EDGE — 경계 및 보안 케이스

| # | 카테고리 | 액터(역할/연동상태) | 메서드 | 경로 | 페이로드/대상 요약 | 기대 응답 | 비고 |
|---|----------|---------------------|--------|------|--------------------|-----------|------|
| 55 | EDGE | ANON | GET | `/api/v1/profile/me` | Authorization: `Bearer <변조된 JWT>` | 401 | 서명 검증 실패 |
| 56 | EDGE | ANON | GET | `/api/v1/profile/me` | Authorization: `Bearer <만료된 JWT>` | 401 | JWT exp 초과 |
| 57 | EDGE | TUNTUN | POST | `/api/v1/events` | `title: "' OR 1=1--"` SQL injection 입력 | 400 | VALIDATION_FAILED 또는 그대로 저장 후 이스케이프(취약점 없어야 함) |

---

> **총 시나리오 수: 57개** (AUTH 5 + PROFILE 5 + LINKAGE 8 + INVITE 4 + EVENT_VIEW 3 + EVENT_CREATE 4 + EVENT_PARTICIPATE 5 + PROXY_PARTICIPATE 6 + POINT_TOPUP 5 + MANAGER 3 + ADMIN 3 + ROLE_SWITCH 3 + EDGE 3)
