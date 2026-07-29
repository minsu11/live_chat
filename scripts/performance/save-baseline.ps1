$ErrorActionPreference = "Stop"

$dbName = if ([string]::IsNullOrWhiteSpace($env:PERF_DB_NAME)) { "chat_server_perf" } else { $env:PERF_DB_NAME }
$dbPassword = if ([string]::IsNullOrWhiteSpace($env:PERF_DB_PASSWORD)) { "1234" } else { $env:PERF_DB_PASSWORD }
$snapshotDir = Join-Path $PSScriptRoot "..\..\performance\snapshots"
$snapshotPath = Join-Path $snapshotDir "baseline.sql"

New-Item -ItemType Directory -Path $snapshotDir -Force | Out-Null

$command = "docker exec chatalk-perf-mysql mysqldump -uroot -p$dbPassword --single-transaction --set-gtid-purged=OFF --routines --triggers $dbName > `"$snapshotPath`""
cmd /c $command

if ($LASTEXITCODE -ne 0) {
    throw "기준 DB 스냅샷 저장에 실패했습니다."
}

Write-Host "기준 DB 스냅샷을 저장했습니다: $snapshotPath"
Write-Host "사용자, 채팅방, 멤버십을 준비한 직후 한 번만 실행하세요."

