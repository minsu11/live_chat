$ErrorActionPreference = "Stop"

$composeFile = Join-Path $PSScriptRoot "..\..\performance\docker-compose.yml"

docker compose -f $composeFile up -d mysql redis prometheus grafana
if ($LASTEXITCODE -ne 0) {
    throw "성능 테스트 인프라 시작에 실패했습니다."
}

docker compose -f $composeFile ps

Write-Host ""
Write-Host "MySQL     : localhost:$($env:PERF_DB_PORT ?? '3307')"
Write-Host "Redis     : localhost:$($env:PERF_REDIS_PORT ?? '6380')"
Write-Host "Prometheus: http://localhost:$($env:PERF_PROMETHEUS_PORT ?? '9091')"
Write-Host "Grafana   : http://localhost:$($env:PERF_GRAFANA_PORT ?? '3001') (admin/admin)"
Write-Host ""
Write-Host "다음 단계: CHAT_METADATA_WRITE_MODE를 설정하고 perf 프로필로 API 서버를 실행하세요."
