$ErrorActionPreference = "Stop"

function Get-EnvOrDefault([string]$Name, [string]$DefaultValue) {
    $value = [Environment]::GetEnvironmentVariable($Name)
    if ([string]::IsNullOrWhiteSpace($value)) {
        return $DefaultValue
    }
    return $value
}

$composeFile = Join-Path $PSScriptRoot "..\..\performance\docker-compose.yml"

docker compose -f $composeFile up -d mysql redis prometheus grafana
if ($LASTEXITCODE -ne 0) {
    throw "성능 테스트 인프라 시작에 실패했습니다."
}

docker compose -f $composeFile ps

$dbPort = Get-EnvOrDefault "PERF_DB_PORT" "3307"
$redisPort = Get-EnvOrDefault "PERF_REDIS_PORT" "6380"
$prometheusPort = Get-EnvOrDefault "PERF_PROMETHEUS_PORT" "9091"
$grafanaPort = Get-EnvOrDefault "PERF_GRAFANA_PORT" "3001"

Write-Host ""
Write-Host "MySQL     : localhost:$dbPort"
Write-Host "Redis     : localhost:$redisPort"
Write-Host "Prometheus: http://localhost:$prometheusPort"
Write-Host "Grafana   : http://localhost:$grafanaPort (admin/admin)"
Write-Host ""
Write-Host "다음 단계: CHAT_METADATA_WRITE_MODE를 설정하고 perf 프로필로 API 서버를 실행하세요."
