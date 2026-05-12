# Day 1 PM 게이트 검증 체크리스트

이 문서는 Day 1 PM 종료 시 "다음 단계로 넘어가도 되는지" 판단하는 절차입니다. 사용자가 직접 따라 실행하거나, code-reviewer/verifier 에이전트가 자동으로 통과 여부를 점검합니다.

## 0. 준비

```bash
# 의존: Docker, Java 21, Flutter, Node 20
cd "C:/Users/hoho9/Documents/26-1 NEXT/seenear"

# Postgres 16 기동
docker compose up -d
docker compose ps   # zoomnear-postgres healthy 확인

# (선택) DB 직접 접속
docker exec -it zoomnear-postgres psql -U zoomnear -d zoomnear
```

## 1. 백엔드 빌드·기동

```bash
cd api
./gradlew clean build -x test       # 빌드만 우선
./gradlew test                       # 단위 테스트
./gradlew bootRun                    # 8080 포트 서비스 시작
```

**합격 조건**:
- [ ] 컴파일 에러 0
- [ ] AuthFacadeTest, PermissionMatrixTest, EventParticipationFacadeTest, PointServiceTest, LinkageGuardTest, InviteFacadeTest 모두 GREEN
- [ ] Flyway V001/V002 + seed가 자동 적용되어 `flyway_schema_history` 테이블에 4 rows 이상
- [ ] `unique_codes` 테이블에 90만 row, 미할당 풀 정상
- [ ] `ranks` 테이블에 4 rows
- [ ] `bootRun` 시작 후 `curl http://localhost:8080/actuator/health` 가 `{"status":"UP"}`

## 2. API 스모크 테스트 (curl)

```bash
# 가입(튼튼이)
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"loginId":"tuntun01","password":"password!1","phone":"010-1111-1111","name":"김튼튼","role":"TUNTUN"}'

# 가입(든든이)
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"loginId":"dundun01","password":"password!1","phone":"010-2222-2222","name":"이든든","role":"DUNDUN"}'

# 로그인
TUN_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"loginId":"tuntun01","password":"password!1"}' | jq -r .accessToken)

# 본인 충전(가짜)
curl -X POST http://localhost:8080/api/v1/points/mock-topup \
  -H "Authorization: Bearer $TUN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"amount":10000}'

# 잔액 확인
curl http://localhost:8080/api/v1/points/me/balance \
  -H "Authorization: Bearer $TUN_TOKEN"
```

**합격 조건**:
- [ ] 가입 시 `accessToken`, `refreshToken`, `userId`, `role` 응답
- [ ] DB의 `users` 테이블에 가입한 2명 + `point_wallets` 2 rows + `unique_codes.assigned_to` 채움
- [ ] mock-topup 후 `point_ledger`에 row 1개 (UNIQUE 제약 동작)
- [ ] balance가 10000

## 3. 정원 race 테스트 (k6)

```bash
# 사전: 이벤트 1개 생성 (capacity=10), TUNTUN 50명 가입 + 충분 잔액
# (k6 setup() 함수가 자동 처리하도록 작성됨)
cd test/load/k6
BASE_URL=http://localhost:8080 k6 run join_event.js
```

**합격 조건**:
- [ ] threshold 통과: 정확히 10명 CONFIRMED
- [ ] HTTP 5xx 비율 0%
- [ ] p95 < 500ms
- [ ] 잔액 음수 0건
- [ ] `event_participations` 테이블 (event_id, participant_id, status IN PENDING/CONFIRMED) UNIQUE 위반 0건

## 4. mock_topup idempotency 테스트

```bash
BASE_URL=http://localhost:8080 k6 run mock_topup_idempotency.js
```

**합격 조건**:
- [ ] 동일 reference_id로 100회 호출 시 ledger row 1개만 생성
- [ ] 잔액 amount × 1 (= 100배가 아님)

## 5. 권한 매트릭스 단위 테스트

```bash
cd api
./gradlew test --tests "*PermissionMatrixTest"
```

**합격 조건**:
- [ ] 권한 매트릭스 57행 시나리오 모두 GREEN
- [ ] 비로그인 → 401, 권한 없음 → 403, 정상 → 200/201/204 분류 일치

## 6. 모바일 앱 빌드·실행

```bash
cd app
flutter pub get
flutter analyze         # 정적 분석
flutter test            # 위젯 + golden 테스트
flutter run             # 에뮬레이터/디바이스
```

**합격 조건**:
- [ ] `flutter analyze` 경고 5개 미만
- [ ] golden test (textScaler 0.8/1.0/1.5/2.0) 통과
- [ ] 앱 실행 시 로그인 화면 진입
- [ ] tuntun01 로그인 → 마이페이지에서 잔액·6자리·이름 표시
- [ ] 이벤트 목록 진입 가능, 이벤트 상세에서 TTS 자동 발화 동작
- [ ] 충전 모달에서 mock-topup 호출 → 잔액 즉시 갱신
- [ ] dundun01 로그인 → 6자리로 tuntun01 검색 → 즉시 연동
- [ ] 든든이가 초대 링크 생성 → OS Share Sheet 호출

## 7. 어드민 웹 (Day 2 본격 작업, Day 1 PM은 placeholder만)

```bash
cd admin
pnpm install
pnpm dev          # http://localhost:3000
```

**합격 조건**:
- [ ] 페이지 진입 가능
- [ ] "ZOOM NEAR Admin" 헤더 + Day 2 placeholder 안내 표시

## 8. 시니어 UX 자동 검사

- [ ] 명도대비 7:1 (브랜드 색 위 흰 글씨, 본문 색 #1A1A1A)
- [ ] 모든 본문 18px 이상
- [ ] Material/Cupertino 직접 사용 0회 (`grep -R "Material(" app/lib/features` 결과 0)
- [ ] 햄버거 메뉴 0개, 캐러셀 자동슬라이드 0개

## 9. 게이트 결과 보고

다음 형식으로 보고:

```
## Day 1 PM Gate

### Pass
- (체크된 항목 나열)

### Fail / Blocked
- (실패한 항목 + 사유)

### 다음 단계
- 모두 Pass → Day 2 AM 시작
- 일부 Fail → 사용자 결정 (재시도 / 자르기 옵션 §9.5 적용 / 일정 재합의)
```

## 부록: 자르기 옵션 (가용 시간 부족 시 §9.5 우선순위)

1. 카카오 공유 SDK → OS Share Sheet만으로 충분
2. AWS 배포 → 로컬 시연
3. Next.js 어드민 → Flutter 앱 안의 매니저/관리자 화면으로 대체
4. STT 음성 검색 → 텍스트 검색만
5. 행정코드 인접 매트릭스 → 동네명 텍스트 매칭만
6. 토스페이먼츠 샌드박스 → mock_topup 유지
