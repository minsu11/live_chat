param(
    [switch]$ResetVolumes
)

$ErrorActionPreference = "Stop"
$composeFile = Join-Path $PSScriptRoot "..\..\performance\docker-compose.yml"

if ($ResetVolumes) {
    docker compose -f $composeFile down -v
} else {
    docker compose -f $composeFile down
}

if ($LASTEXITCODE -ne 0) {
    throw "성능 테스트 인프라 종료에 실패했습니다."
}

if ($ResetVolumes) {
    Write-Host "컨테이너와 성능 테스트용 볼륨을 모두 삭제했습니다."
} else {
    Write-Host "컨테이너를 종료했습니다. 데이터 볼륨은 유지됩니다."
}
