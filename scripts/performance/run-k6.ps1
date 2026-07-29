param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("sync-db", "redis-write-back")]
    [string]$Mode,

    [int]$Vus = 10,
    [int]$MessagesPerVu = 20,
    [int]$SendIntervalMs = 100,
    [int]$DrainSeconds = 30
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($env:ROOM_ID)) {
    throw "ROOM_ID 환경변수를 설정하세요."
}

if ([string]::IsNullOrWhiteSpace($env:ACCESS_TOKEN) -and [string]::IsNullOrWhiteSpace($env:ACCESS_TOKENS)) {
    throw "ACCESS_TOKEN 또는 ACCESS_TOKENS 환경변수를 설정하세요."
}

if ([string]::IsNullOrWhiteSpace($env:USER_ID) -and [string]::IsNullOrWhiteSpace($env:USER_IDS)) {
    throw "USER_ID 또는 USER_IDS 환경변수를 설정하세요."
}

$composeFile = Join-Path $PSScriptRoot "..\..\performance\docker-compose.yml"
$runId = "{0}-{1}" -f $Mode, (Get-Date -Format "yyyyMMdd-HHmmss")

$dockerArgs = @(
    "compose",
    "-f", $composeFile,
    "--profile", "load",
    "run", "--rm",
    "-e", "MODE=$Mode",
    "-e", "RUN_ID=$runId",
    "-e", "RESULT_DIR=/results",
    "-e", "VUS=$Vus",
    "-e", "MESSAGES_PER_VU=$MessagesPerVu",
    "-e", "SEND_INTERVAL_MS=$SendIntervalMs",
    "-e", "DRAIN_SECONDS=$DrainSeconds",
    "-e", "ROOM_ID=$($env:ROOM_ID)"
)

$optionalEnvironmentVariables = @(
    "ACCESS_TOKEN",
    "ACCESS_TOKENS",
    "USER_ID",
    "USER_IDS",
    "WS_BASE_URL",
    "SOCKJS_ENDPOINT",
    "MESSAGE_TYPE",
    "MESSAGE_PREFIX"
)

foreach ($name in $optionalEnvironmentVariables) {
    $value = [Environment]::GetEnvironmentVariable($name)
    if (-not [string]::IsNullOrWhiteSpace($value)) {
        $dockerArgs += @("-e", "$name=$value")
    }
}

$dockerArgs += @(
    "k6",
    "run",
    "--out", "csv=/results/$runId-samples.csv",
    "/scripts/redis-write-back-comparison.js"
)

Write-Host "실행 모드: $Mode"
Write-Host "Run ID   : $runId"
Write-Host "부하 조건 : VUs=$Vus, messages/VU=$MessagesPerVu, interval=${SendIntervalMs}ms, drain=${DrainSeconds}s"
Write-Host ""

& docker @dockerArgs
if ($LASTEXITCODE -ne 0) {
    throw "k6 실행이 실패했습니다. performance/results의 결과 파일과 콘솔 오류를 확인하세요."
}

Write-Host ""
Write-Host "결과 파일: performance/results/$runId-summary.json"
Write-Host "샘플 CSV : performance/results/$runId-samples.csv"

