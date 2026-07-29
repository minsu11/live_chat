$ErrorActionPreference = "Stop"

$dbName = if ([string]::IsNullOrWhiteSpace($env:PERF_DB_NAME)) { "chat_server_perf" } else { $env:PERF_DB_NAME }
$dbPassword = if ([string]::IsNullOrWhiteSpace($env:PERF_DB_PASSWORD)) { "1234" } else { $env:PERF_DB_PASSWORD }
$snapshotPath = Join-Path $PSScriptRoot "..\..\performance\snapshots\baseline.sql"

if (-not (Test-Path $snapshotPath)) {
    throw "baseline.sql이 없습니다. 먼저 save-baseline.ps1을 실행하세요."
}

Write-Host "API 서버가 종료되어 있는지 확인합니다. DB를 초기 상태로 복원합니다."

docker exec chatalk-perf-mysql mysql -uroot -p$dbPassword -e "DROP DATABASE IF EXISTS $dbName; CREATE DATABASE $dbName DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
if ($LASTEXITCODE -ne 0) {
    throw "성능 테스트 DB 재생성에 실패했습니다."
}

$restoreCommand = "docker exec -i chatalk-perf-mysql mysql -uroot -p$dbPassword $dbName < `"$snapshotPath`""
cmd /c $restoreCommand
if ($LASTEXITCODE -ne 0) {
    throw "기준 DB 스냅샷 복원에 실패했습니다."
}

docker exec chatalk-perf-redis redis-cli FLUSHDB | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "Redis 초기화에 실패했습니다."
}

Write-Host "DB와 Redis를 기준 상태로 복원했습니다."

