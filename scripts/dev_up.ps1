# ZOOM NEAR 통합 부트스트랩 (Windows PowerShell)
#
# 컴퓨터 껐다 켰을 때 한 번에 모든 서비스를 띄우는 스크립트입니다.
#
# 전제 조건:
#   - Docker Desktop 실행 중
#   - JDK 21 설치됨 (Eclipse Adoptium Temurin 권장)
#   - Node 18+ 설치됨 (admin 웹용)
#   - Gradle은 이미 다운로드되어 있음 (~/.gradle/dist/gradle-8.10.2)
#
# 사용법 (각 명령은 새 PowerShell 창에서):
#   pwsh ./scripts/dev_up.ps1              # 1) DB만 띄움 + 안내
#   pwsh ./scripts/dev_up.ps1 -StartAll    # 2) DB + API + Admin 모두 별도 창으로 띄움
#   pwsh ./scripts/dev_up.ps1 -Down        # 3) DB 내림

param(
    [switch]$StartAll,
    [switch]$Down
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

# 환경 변수 설정 (시스템 PATH 갱신 + JAVA_HOME)
$jdkPath = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
if (Test-Path $jdkPath) {
    $env:JAVA_HOME = $jdkPath
    if (-not ($env:PATH -like "*$jdkPath*")) {
        $env:PATH = "$jdkPath\bin;" + $env:PATH
    }
}
$gradlePath = "$env:USERPROFILE\.gradle\dist\gradle-8.10.2\bin"
if ((Test-Path $gradlePath) -and -not ($env:PATH -like "*$gradlePath*")) {
    $env:PATH = "$gradlePath;" + $env:PATH
}

if ($Down) {
    Write-Host "==> docker compose down" -ForegroundColor Yellow
    docker compose down
    Write-Host "    Done. 데이터는 볼륨에 보존됨. 완전 초기화는 'docker compose down -v'." -ForegroundColor Yellow
    exit 0
}

Write-Host "==> 1/3 docker compose up -d (Postgres 16)" -ForegroundColor Cyan
docker compose up -d | Out-Null

Write-Host "==> Postgres healthy 대기 (최대 30초)" -ForegroundColor Cyan
$ready = $false
for ($i = 1; $i -le 30; $i++) {
    $health = (docker inspect zoomnear-postgres --format '{{.State.Health.Status}}' 2>$null)
    if ($health -eq "healthy") { $ready = $true; break }
    Start-Sleep -Seconds 1
}
if (-not $ready) {
    Write-Host "    Postgres healthy 도달 실패. docker compose logs postgres 확인" -ForegroundColor Red
    exit 1
}
Write-Host "    Postgres healthy. localhost:5432/zoomnear (zoomnear/zoomnear)" -ForegroundColor Green

if (-not $StartAll) {
    Write-Host ""
    Write-Host "==> 다음 단계 (별도 PowerShell 창에서 직접 실행)" -ForegroundColor Cyan
    Write-Host "    [API]   cd api ; ./gradlew bootRun"
    Write-Host "    [Admin] cd admin ; npm run dev"
    Write-Host "    [Demo]  pwsh ./scripts/demo_e2e.ps1"
    Write-Host ""
    Write-Host "또는 한 번에:  pwsh ./scripts/dev_up.ps1 -StartAll" -ForegroundColor Yellow
    Write-Host ""
    exit 0
}

# StartAll: 백엔드 + 어드민을 각각 새 창에서 실행
Write-Host ""
Write-Host "==> 2/3 새 창에서 Spring Boot 백엔드 시작 (포트 8080)" -ForegroundColor Cyan
$apiPath = Join-Path $repoRoot "api"
$apiCmd = "Set-Location '$apiPath'; `$env:JAVA_HOME='$jdkPath'; `$env:PATH='$jdkPath\bin;' + `$env:PATH; ./gradlew bootRun"
Start-Process powershell -ArgumentList "-NoExit", "-Command", $apiCmd
Write-Host "    백엔드 창 열림. 부팅 ~10초 (Flyway 마이그레이션 자동 적용)" -ForegroundColor Green

Write-Host ""
Write-Host "==> 3/3 새 창에서 Next.js 어드민 시작 (포트 3000)" -ForegroundColor Cyan
$adminPath = Join-Path $repoRoot "admin"
$adminCmd = "Set-Location '$adminPath'; npm run dev"
Start-Process powershell -ArgumentList "-NoExit", "-Command", $adminCmd
Write-Host "    어드민 창 열림" -ForegroundColor Green

Write-Host ""
Write-Host "==> 접속 정보" -ForegroundColor Cyan
Write-Host "    Postgres : localhost:5432 / zoomnear / zoomnear"
Write-Host "    API      : http://localhost:8080  (health: /actuator/health)"
Write-Host "    Admin    : http://localhost:3000  (admin09 / admin0909)"
Write-Host ""
Write-Host "==> 데모 자동 검증 (모든 창이 Ready 된 후)" -ForegroundColor Cyan
Write-Host "    pwsh ./scripts/demo_e2e.ps1"
Write-Host ""
