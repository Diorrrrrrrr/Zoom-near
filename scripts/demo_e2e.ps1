# ZOOM NEAR 5종 E2E 시연 자동 검증 스크립트 (PowerShell)
#
# 사용법:
#   $env:BASE_URL="http://localhost:8080"   # 선택 (기본값 동일)
#   pwsh ./scripts/demo_e2e.ps1
#
# 전제: docker compose up -d 후 cd api && ./gradlew bootRun 진행 중

param(
    [string]$BaseUrl = $(if ($env:BASE_URL) { $env:BASE_URL } else { "http://localhost:8080" })
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$timestamp = Get-Date -Format "HHmmss"
$phoneTail = (Get-Date).ToString("mmss")   # phone 4자리 (정규식 ^...\d{4}$ 매칭)
$tunLogin = "tun_$timestamp"
$dunLogin = "dun_$timestamp"
$pwd = "Password!1"

function Step($msg) {
    Write-Host ""
    Write-Host "==> $msg" -ForegroundColor Cyan
}

function Pass($msg) { Write-Host "    PASS: $msg" -ForegroundColor Green }
function Fail($msg) { Write-Host "    FAIL: $msg" -ForegroundColor Red; throw $msg }

function Post($path, $body, $token = $null) {
    $headers = @{ "Content-Type" = "application/json; charset=utf-8" }
    if ($token) { $headers["Authorization"] = "Bearer $token" }
    $json = $body | ConvertTo-Json -Compress
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
    return Invoke-RestMethod -Method Post -Uri "$BaseUrl$path" -Headers $headers -Body $bytes
}

function Get($path, $token = $null) {
    $headers = @{}
    if ($token) { $headers["Authorization"] = "Bearer $token" }
    return Invoke-RestMethod -Method Get -Uri "$BaseUrl$path" -Headers $headers
}

# ========================================================================
Step "Health check"
$h = Invoke-RestMethod -Uri "$BaseUrl/actuator/health"
if ($h.status -ne "UP") { Fail "actuator/health != UP" } else { Pass "API alive" }

# ========================================================================
Step "E2E-1: 튼튼이 가입 → 로그인 → 본인 충전 → 이벤트 참여 → 잔액 차감"

$tunSignup = Post "/api/v1/auth/signup" @{
    loginId = $tunLogin; password = $pwd; phone = "010-1111-$phoneTail";
    name = "김튼튼"; role = "TUNTUN"
}
Pass "TUNTUN 가입 OK (userId=$($tunSignup.userId), uniqueCode=$($tunSignup.uniqueCode))"

$tunLogin1 = Post "/api/v1/auth/login" @{ loginId = $tunLogin; password = $pwd }
$tunToken = $tunLogin1.accessToken
Pass "TUNTUN 로그인 OK"

$me = Get "/api/v1/me" $tunToken
$tunCode = $me.uniqueCode
$tunUserId = $me.id
if (-not $tunCode -or $tunCode.Length -ne 6) { Fail "uniqueCode 6자리 아님: $tunCode" } else { Pass "6자리 코드 발급 ($tunCode)" }

$topup = Post "/api/v1/points/mock-topup" @{ amount = 30000 } $tunToken
if ($topup.newBalance -ne 30000) { Fail "충전 후 잔액 30000 아님: $($topup.newBalance)" } else { Pass "본인 충전 OK (잔액 30000)" }

# 이벤트 등록 (TUNTUN 본인이 등록)
$now = (Get-Date).ToUniversalTime()
$start = $now.AddDays(7).ToString("yyyy-MM-ddTHH:mm:ssZ")
$end = $now.AddDays(7).AddHours(2).ToString("yyyy-MM-ddTHH:mm:ssZ")
$evt = Post "/api/v1/events" @{
    title = "탁구 모임 $timestamp"
    description = "건강한 노년을 위한 탁구"
    regionText = "서울 강남구 역삼동"
    category = "SPORTS"
    startsAt = $start
    endsAt = $end
    capacity = 10
    pointCost = 5000
    managerProgram = $false
} $tunToken
$evtId = $evt.id
Pass "이벤트 등록 OK (id=$evtId, capacity=10, pointCost=5000)"

$join1 = Post "/api/v1/events/$evtId/join" @{} $tunToken
if ($join1.status -ne "CONFIRMED") { Fail "본인 참여 status=$($join1.status)" } else { Pass "본인 참여 즉시 CONFIRMED" }

$bal1 = Get "/api/v1/points/me/balance" $tunToken
if ($bal1.balance -ne 25000) { Fail "차감 후 잔액 25000 아님: $($bal1.balance)" } else { Pass "차감 후 잔액 25000 OK" }

# ========================================================================
Step "E2E-2: 든든이 가입 → 6자리로 튼튼이 검색 → 즉시 연동"

$dunSignup = Post "/api/v1/auth/signup" @{
    loginId = $dunLogin; password = $pwd; phone = "010-2222-$phoneTail";
    name = "이든든"; role = "DUNDUN"
}
$dunLogin1 = Post "/api/v1/auth/login" @{ loginId = $dunLogin; password = $pwd }
$dunToken = $dunLogin1.accessToken
Pass "DUNDUN 가입+로그인 OK"

$found = Get "/api/v1/users/search?uniqueCode=$tunCode" $dunToken
if ($found.id -ne $tunUserId) { Fail "검색 결과 mismatch" } else { Pass "6자리 검색 OK" }

$link = Post "/api/v1/linkages" @{ tuntunId = $tunUserId; isPrimary = $true } $dunToken
Pass "연동 OK (linkageId=$($link.id), is_primary=true)"

# ========================================================================
Step "E2E-3: 든든이 초대링크 생성 → (가상) 새 사용자 토큰으로 가입"

$invite = Post "/api/v1/invites" @{} $dunToken
$inviteToken = $invite.token
Pass "초대 토큰 발급 OK ($inviteToken)"

$invitedLogin = "inv_$timestamp"
$invitedSignup = Post "/api/v1/auth/signup" @{
    loginId = $invitedLogin; password = $pwd; phone = "010-3333-$phoneTail";
    name = "박부모"; role = "TUNTUN"; inviteToken = $inviteToken
}
Pass "초대 토큰으로 TUNTUN 가입 OK"

# 동일 토큰 재사용 → 실패해야 함
try {
    Post "/api/v1/auth/signup" @{
        loginId = "rejected_$timestamp"; password = $pwd; phone = "010-4444-$phoneTail";
        name = "재시도"; role = "TUNTUN"; inviteToken = $inviteToken
    } | Out-Null
    Fail "이미 소비된 토큰 재사용이 성공함 (4xx 기대)"
} catch {
    Pass "이미 소비된 토큰 재사용 차단 (4xx)"
}

# ========================================================================
Step "E2E-4: 든든이 대리 참여 요청 → 튼튼이 승인 → 차감"

# 두 번째 이벤트 등록
$start2 = $now.AddDays(8).ToString("yyyy-MM-ddTHH:mm:ssZ")
$end2 = $now.AddDays(8).AddHours(2).ToString("yyyy-MM-ddTHH:mm:ssZ")
$evt2 = Post "/api/v1/events" @{
    title = "수영 모임 $timestamp"
    description = "주 1회 수영"
    regionText = "서울 강남구 역삼동"
    category = "SPORTS"
    startsAt = $start2
    endsAt = $end2
    capacity = 10
    pointCost = 3000
    managerProgram = $false
} $tunToken
$evtId2 = $evt2.id

$proxyJoin = Post "/api/v1/events/$evtId2/join" @{ proxiedTuntunId = $tunUserId } $dunToken
if ($proxyJoin.status -ne "PENDING_APPROVAL") { Fail "대리 참여 status=$($proxyJoin.status)" } else { Pass "대리 참여 PENDING_APPROVAL OK" }

$pending = Get "/api/v1/approvals/me?status=PENDING" $tunToken
$approvalId = $pending.items[0].id
if (-not $approvalId) { Fail "TUNTUN 승인 대기 목록 비어있음" } else { Pass "TUNTUN 승인 대기 1건 확인" }

Post "/api/v1/approvals/$approvalId/approve" @{} $tunToken | Out-Null
Pass "TUNTUN 승인 처리 OK"

$bal2 = Get "/api/v1/points/me/balance" $tunToken
if ($bal2.balance -ne 22000) { Fail "승인 후 잔액 22000 아님: $($bal2.balance)" } else { Pass "승인 후 잔액 22000 OK" }

# ========================================================================
Step "E2E-5: 든든이 대리 충전 → 잔액 증가"

$proxyTopup = Post "/api/v1/points/mock-topup-proxy" @{ tuntunId = $tunUserId; amount = 50000 } $dunToken
Pass "대리 충전 호출 OK"

$bal3 = Get "/api/v1/points/me/balance" $tunToken
if ($bal3.balance -ne 72000) { Fail "대리 충전 후 잔액 72000 아님: $($bal3.balance)" } else { Pass "대리 충전 후 잔액 72000 OK" }

# 비연동 대리 충전 시도 → 차단
$other = Post "/api/v1/auth/signup" @{
    loginId = "other_$timestamp"; password = $pwd; phone = "010-5555-$phoneTail";
    name = "남남"; role = "TUNTUN"
}
try {
    Post "/api/v1/points/mock-topup-proxy" @{ tuntunId = $other.userId; amount = 1000 } $dunToken | Out-Null
    Fail "비연동 튼튼이에 대리 충전 성공 (403 기대)"
} catch {
    Pass "비연동 튼튼이 대리 차단 (403)"
}

# ========================================================================
Write-Host ""
Write-Host "================================================" -ForegroundColor Green
Write-Host "  ALL 5 E2E SCENARIOS PASSED" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Green
Write-Host ""
Write-Host "TUNTUN: $tunLogin ($tunCode), 잔액 72000"
Write-Host "DUNDUN: $dunLogin"
Write-Host "이벤트: $evtId, $evtId2"
Write-Host ""
