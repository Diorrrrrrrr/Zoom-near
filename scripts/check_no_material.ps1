# ZOOM NEAR - Material/Cupertino 위젯 직접 사용 자동 검사
#
# 시니어 UX 룰: app/lib/features/** 에서는 Sr* 위젯만 사용해야 함.
# Sr* 위젯 내부에서만 Material/Cupertino를 wrapping. features/ 화면 코드는 직접 사용 금지.
#
# 사용법:
#   pwsh ./scripts/check_no_material.ps1
#   Exit 0 = 통과, Exit 1 = 위반 발견

$ErrorActionPreference = "Stop"
Set-Location (Split-Path -Parent $PSScriptRoot)

$featuresPath = "app/lib/features"

# 금지 패턴: 화면 코드에서 직접 사용 불가
$forbidden = @(
    'ElevatedButton\s*\(',
    'OutlinedButton\s*\(',
    'TextButton\s*\(',
    'FilledButton\s*\(',
    'IconButton\s*\(',
    'FloatingActionButton\s*\(',
    'TextField\s*\(',
    'TextFormField\s*\(',
    'AppBar\s*\(',
    'Scaffold\s*\(',
    'Drawer\s*\(',
    'BottomNavigationBar\s*\(',
    'BottomAppBar\s*\(',
    'AlertDialog\s*\(',
    'SimpleDialog\s*\('
)

# 허용 패턴 (Sr* 위젯, Material 데이터 클래스, 합성 패턴은 제외)
$allowedContains = @(
    'SrScaffold',
    'SrAppBar',
    'SrButton',
    'SrText',
    'SrCard',
    'SrInput',
    'SrConfirmDialog',
    'SrBottomNav'
)

if (-not (Test-Path $featuresPath)) {
    Write-Host "features 경로 부재: $featuresPath" -ForegroundColor Red
    exit 1
}

$violations = @()

Get-ChildItem -Path $featuresPath -Recurse -Filter "*.dart" | ForEach-Object {
    $file = $_.FullName
    $relPath = $file.Substring((Get-Location).Path.Length + 1)
    $lineNo = 0
    Get-Content $file | ForEach-Object {
        $lineNo++
        $line = $_
        foreach ($pat in $forbidden) {
            if ($line -match $pat) {
                # 코멘트 라인 제외
                if ($line.TrimStart() -notmatch '^//') {
                    $violations += [PSCustomObject]@{
                        File = $relPath
                        Line = $lineNo
                        Match = $matches[0]
                        Context = $line.Trim()
                    }
                }
            }
        }
    }
}

if ($violations.Count -eq 0) {
    Write-Host ""
    Write-Host "PASS: features/ 에서 Material/Cupertino 직접 사용 0건" -ForegroundColor Green
    Write-Host ""
    exit 0
} else {
    Write-Host ""
    Write-Host "FAIL: Material/Cupertino 직접 사용 $($violations.Count)건 발견" -ForegroundColor Red
    Write-Host ""
    $violations | Format-Table -AutoSize File, Line, Match, Context
    Write-Host ""
    Write-Host "Sr* 위젯으로 교체하세요. (app/lib/core/widgets/sr_*.dart)" -ForegroundColor Yellow
    exit 1
}
