$ErrorActionPreference = "Stop"

$dbPassword = [string](
    docker exec chatalk-perf-mysql `
      printenv MYSQL_ROOT_PASSWORD
)

$dbName = [string](
    docker exec chatalk-perf-mysql `
      printenv MYSQL_DATABASE
)

$dbPassword = $dbPassword.Trim()
$dbName = $dbName.Trim()

if ([string]::IsNullOrWhiteSpace($dbPassword)) {
    throw "MYSQL_ROOT_PASSWORD를 찾지 못했습니다."
}

if ([string]::IsNullOrWhiteSpace($dbName)) {
    throw "MYSQL_DATABASE를 찾지 못했습니다."
}

$snapshotPath = Join-Path `
    $PSScriptRoot `
    "..\..\performance\snapshots\baseline.sql"

if (-not (Test-Path $snapshotPath)) {
    throw "baseline.sql이 없습니다. 먼저 save-baseline.ps1을 실행하세요."
}

$snapshotPath = (
    Resolve-Path $snapshotPath
).Path

Write-Host "API 서버와 Auth 서버가 종료되어 있는지 확인합니다."
Write-Host "DB를 초기 상태로 복원합니다."

docker exec chatalk-perf-mysql `
    mysql `
    -uroot `
    "--password=$dbPassword" `
    -e "SELECT 1;" |
    Out-Null

if ($LASTEXITCODE -ne 0) {
    throw "MySQL 접속에 실패했습니다."
}

docker cp `
    "$snapshotPath" `
    "chatalk-perf-mysql:/tmp/chatalk-baseline.sql"

if ($LASTEXITCODE -ne 0) {
    throw "baseline.sql 컨테이너 복사에 실패했습니다."
}

$recreateSql = (
    "DROP DATABASE IF EXISTS ``$dbName``; " +
    "CREATE DATABASE ``$dbName`` " +
    "DEFAULT CHARACTER SET utf8mb4 " +
    "COLLATE utf8mb4_unicode_ci;"
)

docker exec chatalk-perf-mysql `
    mysql `
    -uroot `
    "--password=$dbPassword" `
    -e $recreateSql

if ($LASTEXITCODE -ne 0) {
    throw "성능 테스트 DB 재생성에 실패했습니다."
}

docker exec chatalk-perf-mysql `
    mysql `
    --default-character-set=utf8mb4 `
    -uroot `
    "--password=$dbPassword" `
    $dbName `
    -e "SOURCE /tmp/chatalk-baseline.sql;"

if ($LASTEXITCODE -ne 0) {
    throw "기준 DB 스냅샷 복원에 실패했습니다."
}

docker exec chatalk-perf-mysql `
    rm -f /tmp/chatalk-baseline.sql

docker exec chatalk-perf-redis `
    redis-cli `
    -n 0 `
    FLUSHDB |
    Out-Null

if ($LASTEXITCODE -ne 0) {
    throw "Redis 초기화에 실패했습니다."
}

Write-Host "DB와 Redis를 기준 상태로 복원했습니다."