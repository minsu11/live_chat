$ErrorActionPreference = "Stop"

$env:PERF_DB_PASSWORD = [string](
    docker exec chatalk-perf-mysql `
      printenv MYSQL_ROOT_PASSWORD
)

$env:PERF_DB_NAME = [string](
    docker exec chatalk-perf-mysql `
      printenv MYSQL_DATABASE
)

$env:PERF_DB_PASSWORD =
    $env:PERF_DB_PASSWORD.Trim()

$env:PERF_DB_NAME =
    $env:PERF_DB_NAME.Trim()

if ([string]::IsNullOrWhiteSpace($env:PERF_DB_PASSWORD)) {
    throw "PERF_DB_PASSWORD가 비어 있습니다."
}

if ([string]::IsNullOrWhiteSpace($env:PERF_DB_NAME)) {
    throw "PERF_DB_NAME이 비어 있습니다."
}

Write-Host "DB NAME         = $env:PERF_DB_NAME"
Write-Host "PASSWORD LENGTH = $($env:PERF_DB_PASSWORD.Length)"

docker exec `
  -e "MYSQL_PWD=$env:PERF_DB_PASSWORD" `
  chatalk-perf-mysql `
  mysql `
  -uroot `
  -e "SELECT 1 AS connection_test;"

if ($LASTEXITCODE -ne 0) {
    throw "MySQL 접속 확인에 실패했습니다."
}

$roomIdResult = docker exec `
  -e "MYSQL_PWD=$env:PERF_DB_PASSWORD" `
  chatalk-perf-mysql `
  mysql `
  --batch `
  --skip-column-names `
  -uroot `
  $env:PERF_DB_NAME `
  -e "
      SELECT cr.id
      FROM chat_room cr
      JOIN chat_list cl
        ON cl.chat_room_id = cr.id
      WHERE cr.room_type = 'GROUP'
      GROUP BY cr.id
      HAVING COUNT(cl.id) = 10
      ORDER BY cr.id DESC
      LIMIT 1;
  "

if ($LASTEXITCODE -ne 0) {
    throw "10명 그룹방 조회에 실패했습니다."
}

$env:ROOM_ID = (
    [string]$roomIdResult
).Trim()

if ([string]::IsNullOrWhiteSpace($env:ROOM_ID)) {
    throw "10명 그룹방을 찾지 못했습니다."
}

$senderIds = @(
    "perfuser04",
    "perfuser05",
    "perfuser06",
    "perfuser07",
    "perfuser08",
    "perfuser09",
    "perfuser10",
    "perfuser11",
    "perfuser12",
    "perfuser13"
)

$tokens = @()
$uuids = @()

foreach ($loginId in $senderIds) {
    $loginJson = @{
        userId   = $loginId
        password = "Perf1234!"
    } | ConvertTo-Json -Compress

    $loginBytes =
        [System.Text.Encoding]::UTF8.GetBytes(
            $loginJson
        )

    try {
        $loginResponse = Invoke-RestMethod `
          -Uri "http://localhost:9090/api/v1/auth/login" `
          -Method Post `
          -ContentType "application/json; charset=utf-8" `
          -Body $loginBytes
    } catch {
        throw "[LOGIN FAILED] $loginId / $($_.Exception.Message)"
    }

    $loginData = $loginResponse

    for ($depth = 0; $depth -lt 3; $depth++) {
        if ($null -eq $loginData) {
            break
        }

        $dataProperty =
            $loginData.PSObject.Properties["data"]

        if ($null -eq $dataProperty) {
            break
        }

        $loginData = $loginData.data
    }

    $token = [string]$loginData.accessToken

    if ([string]::IsNullOrWhiteSpace($token)) {
        $loginResponse |
            ConvertTo-Json -Depth 10

        throw "[TOKEN NOT FOUND] $loginId"
    }

    $uuidResult = docker exec `
      -e "MYSQL_PWD=$env:PERF_DB_PASSWORD" `
      chatalk-perf-mysql `
      mysql `
      --batch `
      --skip-column-names `
      -uroot `
      $env:PERF_DB_NAME `
      -e "
          SELECT uuid
          FROM user
          WHERE input_id = '$loginId'
            AND status = 'ACTIVE'
          LIMIT 1;
      "

    if ($LASTEXITCODE -ne 0) {
        throw "[UUID QUERY FAILED] $loginId"
    }

    $uuid = (
        [string]$uuidResult
    ).Trim()

    if ([string]::IsNullOrWhiteSpace($uuid)) {
        throw "[UUID NOT FOUND] $loginId"
    }

    $tokens += $token
    $uuids += $uuid

    Write-Host "[SENDER OK] $loginId / $uuid"
}

$env:ACCESS_TOKENS = $tokens -join ","
$env:USER_IDS = $uuids -join ","

$tokenCount =
    ($env:ACCESS_TOKENS -split ",").Count

$userCount =
    ($env:USER_IDS -split ",").Count

Write-Host ""
Write-Host "ROOM_ID     = $env:ROOM_ID"
Write-Host "TOKEN COUNT = $tokenCount"
Write-Host "USER COUNT  = $userCount"

if ($tokenCount -ne 10) {
    throw "토큰이 10개가 아닙니다. actual=$tokenCount"
}

if ($userCount -ne 10) {
    throw "UUID가 10개가 아닙니다. actual=$userCount"
}

Write-Host "k6 환경 준비 완료"