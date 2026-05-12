# E2E 핵심 시나리오 5종

> 각 시나리오는 **(전제조건 / 단계별 액션 / 검증 포인트 / 기대 상태)** 구조로 작성됩니다.
> Day 1 PM에 Lane Q가 실제 자동화 코드(Spring MockMvc / Flutter integration test)로 전환 예정.

---

## E2E-1. 튼튼이 직접 가입 → 로그인 → 이벤트 참여 → 잔액 차감

### 전제조건
- DB 초기화 상태 (no users, no events)
- 이벤트 `EVT-001`: `capacity=10`, `fee=3000`
- 테스트 픽스처: 이벤트를 미리 ADMIN 계정으로 INSERT

### 단계별 액션
1. `POST /api/v1/auth/signup` → `{loginId:"tuntun01", password:"P@ssw0rd!", phone:"010-1111-2222", name:"김튼튼", role:"TUNTUN"}`
2. `POST /api/v1/auth/login` → loginId/password 입력
3. `POST /api/v1/points/topup` → `{amount:10000, referenceId:"ref-001"}` (잔액 보충)
4. `POST /api/v1/events/EVT-001/join` (Access Token 포함)

### 검증 포인트
- step1: HTTP 201, `unique_code` 6자리 반환
- step2: HTTP 200, `access_token` + `refresh_token` 포함
- step3: HTTP 200, `balance == 10000`
- step4: HTTP 201, `balance == 7000` (10000 - 3000)
- `point_wallet.balance` DB 직접 확인 == 7000
- `event_participants` 테이블에 row 1건 존재

### 기대 상태
```
users: {loginId:"tuntun01", role:TUNTUN, status:ACTIVE}
point_wallet: {balance:7000}
event_participants: {eventId:EVT-001, userId:tuntun01-uuid, status:ACTIVE}
events: {id:EVT-001, currentCapacity:9}
```

---

## E2E-2. 든든이 가입 → 6자리 코드로 튼튼이 검색 → 즉시 연동

### 전제조건
- 튼튼이 `tuntun01` 이미 가입 완료 (`unique_code = "ABC123"`)
- 든든이 `dundun01` 미가입

### 단계별 액션
1. `POST /api/v1/auth/signup` → `{loginId:"dundun01", role:"DUNDUN", ...}`
2. `POST /api/v1/auth/login` → dundun01 로그인
3. `GET /api/v1/linkage/search?code=ABC123`
4. `POST /api/v1/linkage` → `{tuntunId: "<tuntun01-uuid>"}`

### 검증 포인트
- step3: HTTP 200, `{name:"김튼튼(마스킹)", role:TUNTUN}` 반환
- step4: HTTP 201, `linkageId` 반환
- `linkages` 테이블: `{dundun_id, tuntun_id, status:ACTIVE}` 1행
- `is_primary` 필드: 첫 번째 연동이므로 `true`
- 중복 연동 재시도 시 HTTP 409 반환 확인

### 기대 상태
```
linkages: {dundunId:dundun01-uuid, tuntunId:tuntun01-uuid, status:ACTIVE, isPrimary:true}
```

---

## E2E-3. 든든이 초대링크 생성 → 공유 → 받는 사람 가입 → 자동 연동

### 전제조건
- 든든이 `dundun02` 이미 가입·로그인 완료
- 신규 사용자(TUNTUN 예정) `tuntun_new` 아직 미가입

### 단계별 액션
1. `POST /api/v1/invite/token` (dundun02 토큰으로) → `{token:"INV-XYZ789", expiresAt:"..."}`
2. 토큰 링크 공유 (시뮬레이션: 토큰 문자열 기록)
3. `POST /api/v1/auth/signup-via-invite` → `{token:"INV-XYZ789", loginId:"tuntun_new", role:"TUNTUN", ...}`
4. `POST /api/v1/auth/login` → tuntun_new 로그인
5. `GET /api/v1/linkage/me` (tuntun_new) → 연동 목록 확인

### 검증 포인트
- step1: HTTP 201, `token` 길이 > 10, `expiresAt` ~24h 이후
- step3: HTTP 201, 자동으로 linkage ACTIVE 생성 확인
- step5: `dundun02-uuid`가 연동 목록에 포함
- 동일 토큰 재사용 → HTTP 409
- 만료 후 사용 → HTTP 400

### 기대 상태
```
invite_tokens: {token:"INV-XYZ789", used:true, usedAt:now}
linkages: {dundunId:dundun02-uuid, tuntunId:tuntunNew-uuid, status:ACTIVE}
```

---

## E2E-4. 든든이 대리 참여 요청 → 튼튼이 알림함 확인 → 승인 → 잔액 차감

### 전제조건
- `dundun03` ↔ `tuntun03` ACTIVE 연동 존재
- `tuntun03` 잔액 = 5000
- 이벤트 `EVT-002`: `capacity=5`, `fee=2000`

### 단계별 액션
1. `POST /api/v1/proxy/events/EVT-002/join` (dundun03) → `{tuntunId:"tuntun03-uuid"}`
2. `GET /api/v1/notifications` (tuntun03) → 알림 목록 조회
3. `GET /api/v1/approvals/{approvalId}` (tuntun03) → 승인 요청 상세
4. `POST /api/v1/approvals/{approvalId}/approve` (tuntun03)

### 검증 포인트
- step1: HTTP 201, `approvalId` 반환, `status:PENDING`
- step2: 알림 1건 이상, `type:PROXY_JOIN_REQUEST` 포함
- step4: HTTP 200, `status:APPROVED`
- 잔액: `tuntun03.balance == 3000` (5000 - 2000)
- `event_participants`: EVT-002에 tuntun03 row 생성
- 타인(dundun04)이 approve 시도 → HTTP 403
- PENDING 상태에서 재승인 시도 → HTTP 409 (이미 처리됨)

### 기대 상태
```
approvals: {id:approvalId, status:APPROVED, actedAt:now}
point_wallet: {userId:tuntun03-uuid, balance:3000}
event_participants: {eventId:EVT-002, userId:tuntun03-uuid, status:ACTIVE}
events: {id:EVT-002, currentCapacity:4}
```

---

## E2E-5. 든든이가 연동된 튼튼이에게 mock 충전 → 잔액 증가

### 전제조건
- `dundun04` ↔ `tuntun04` ACTIVE 연동 존재
- `tuntun04` 초기 잔액 = 0

### 단계별 액션
1. `POST /api/v1/proxy/points/topup` (dundun04) → `{tuntunId:"tuntun04-uuid", amount:20000, referenceId:"ref-proxy-001"}`
2. `GET /api/v1/approvals/{approvalId}` (tuntun04) → PENDING 확인
3. `POST /api/v1/approvals/{approvalId}/approve` (tuntun04)
4. `GET /api/v1/points/balance` (tuntun04) → 잔액 확인
5. 동일 `referenceId`로 재충전 시도 (idempotency)

### 검증 포인트
- step1: HTTP 201, approvals PENDING 생성
- step3: HTTP 200, APPROVED
- step4: `balance == 20000`
- step5: HTTP 409 — `referenceId` UNIQUE 제약으로 중복 적립 차단 (idempotency 보장)
- `ledger` 테이블에 row가 정확히 1건만 존재

### 기대 상태
```
approvals: {status:APPROVED}
point_wallet: {userId:tuntun04-uuid, balance:20000}
ledger: {referenceId:"ref-proxy-001"} — row count == 1
```

---

> **총 E2E 시나리오: 5개**
> Day 1 PM 자동화 대상: E2E-1, E2E-4 (가장 핵심 흐름, MockMvc + Testcontainers)
