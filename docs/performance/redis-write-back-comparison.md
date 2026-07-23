# Redis Write-Back 비교 테스트 가이드

## 1. 문서 목적

이 문서는 Chatalk에서 발생했던 다음 현상을 다시 검증하기 위한 실행 가이드다.

- 동시 메시지 요청이 증가하면서 DB 커넥션 반환이 늦어짐
- 뒤에 들어온 요청이 커넥션 풀에서 대기함
- 부하 발생이 끝난 뒤에도 서버가 밀린 요청을 계속 처리함
- 테스트 종료 후에도 클라이언트 화면에 메시지가 한동안 계속 도착함

비교 대상은 과거 코드와 현재 코드를 그대로 비교하는 방식이 아니다. 과거와 현재는 DB 테이블, 인덱스, 조회 구조가 달라졌기 때문에 그대로 비교하면 Redis Write-Back의 영향만 분리하기 어렵다.

따라서 **현재 코드와 현재 DB 스키마를 동일하게 유지하고 메타데이터 저장 방식만 전환**한다.

| 모드 | 메타데이터 처리 방식 |
| --- | --- |
| `sync-db` | 메시지 요청 트랜잭션 안에서 `chat_room`, `chat_list`를 즉시 갱신 |
| `redis-write-back` | Redis Hash와 Dirty Set에 먼저 반영하고 Scheduler가 JDBC Batch Update 수행 |

메시지 원문은 두 모드 모두 MySQL `chat_message`에 즉시 저장한다.

---

## 2. 이번 브랜치에서 추가한 구성

```text
src/main/resources/application-perf.yml
performance/
├── docker-compose.yml
├── .env.example
├── prometheus/prometheus.yml
├── grafana/
│   ├── provisioning/
│   └── dashboards/chatalk-writeback-comparison.json
├── results/
├── snapshots/
└── sql/verify-run.sql
scripts/performance/
├── start-infra.ps1
├── stop-infra.ps1
├── save-baseline.ps1
├── restore-baseline.ps1
├── run-k6.ps1
└── k6/redis-write-back-comparison.js
```

`application-perf.yml`은 운영 환경과 분리된 MySQL, Redis 포트와 작은 HikariCP를 사용한다. 운영 DB 또는 기존 개발 DB에 `perf` 프로필을 연결하지 않는다.

---

## 3. 사전 준비

### 필요한 프로그램

- Java 21
- Docker Desktop
- PowerShell
- Chatalk Auth Server
- 테스트 사용자 계정과 JWT

### 테스트 데이터 권장 조건

- 그룹 채팅방 1개
- 채팅방 멤버 10명 이상
- 실제로 메시지를 보내는 사용자 1명 이상
- 모든 테스트 계정이 같은 채팅방의 멤버여야 함

채팅방 멤버가 많을수록 메시지 한 건에서 갱신하는 사용자별 메타데이터가 증가한다. 과거의 DB UPDATE 적체를 확인하려면 1:1 채팅방보다 10명 이상의 그룹 채팅방이 적합하다.

---

## 4. 인프라 실행

프로젝트 루트에서 실행한다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\performance\start-infra.ps1
```

기본 포트는 다음과 같다.

| 구성 | 주소 |
| --- | --- |
| MySQL | `localhost:3307` |
| Redis | `localhost:6380` |
| Prometheus | `http://localhost:9091` |
| Grafana | `http://localhost:3001` |
| API Server | `http://localhost:7070` |

Grafana 로그인 계정은 `admin / admin`이다. 익명 조회도 허용되어 있다.

---

## 5. API 서버 최초 실행과 데이터 준비

처음에는 `redis-write-back` 모드로 실행해도 되고 `sync-db` 모드로 실행해도 된다. 최초 실행에서는 Hibernate가 성능 테스트 전용 DB에 현재 Entity 기준 테이블을 생성한다.

### Windows PowerShell

```powershell
$env:CHAT_METADATA_WRITE_MODE="redis-write-back"
.\gradlew.bat bootRun --args="--spring.profiles.active=perf"
```

다른 터미널에서 상태를 확인한다.

```powershell
Invoke-RestMethod http://localhost:7070/actuator/health
```

응답의 상태가 `UP`이어야 한다.

이후 다음 데이터를 준비한다.

1. 테스트 사용자 생성 또는 로그인
2. 그룹 채팅방 생성
3. 테스트 사용자들을 채팅방에 초대
4. 메시지를 한두 건 보내 정상 동작 확인
5. 실제 `roomId`, JWT, 내부 `userId` 기록

JWT 값에는 `Bearer `를 붙이지 않는다. k6 스크립트가 STOMP CONNECT 프레임에 `Bearer `를 추가한다.

---

## 6. 기준 DB 스냅샷 저장

두 모드를 같은 데이터에서 시작시키기 위해 사용자와 채팅방을 준비한 직후 DB 스냅샷을 한 번 저장한다.

API 서버를 종료한 뒤 실행한다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\performance\save-baseline.ps1
```

생성 파일:

```text
performance/snapshots/baseline.sql
```

이 파일은 테스트 계정 정보가 포함될 수 있으므로 Git에 커밋되지 않도록 `.gitignore`에 등록되어 있다.

---

## 7. k6 환경변수 설정

### 한 명의 사용자로 여러 연결을 생성하는 경우

```powershell
$env:ROOM_ID="1"
$env:ACCESS_TOKEN="JWT_VALUE"
$env:USER_ID="1"
```

같은 사용자로 여러 WebSocket 세션을 열면 모든 세션이 동일한 사용자 destination을 구독하게 된다. 실행은 가능하지만 클라이언트 수신 부하가 증가할 수 있다.

### 여러 사용자를 사용하는 권장 방식

```powershell
$env:ROOM_ID="1"
$env:ACCESS_TOKENS="TOKEN_1,TOKEN_2,TOKEN_3"
$env:USER_IDS="1,2,3"
```

토큰과 사용자 ID의 순서는 서로 맞아야 한다. VU 수가 토큰 수보다 많으면 순환해서 다시 사용한다.

---

## 8. 워밍업

JVM JIT 컴파일, DB Buffer Pool, 커넥션 생성 영향을 줄이기 위해 본 테스트 전에 작은 부하를 한 번 실행한다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\performance\run-k6.ps1 `
  -Mode redis-write-back `
  -Vus 3 `
  -MessagesPerVu 5 `
  -SendIntervalMs 200 `
  -DrainSeconds 10
```

워밍업 결과는 최종 비교표에 포함하지 않는다.

---

## 9. 비교 테스트 실행 순서

두 모드는 반드시 같은 DB 스냅샷, Redis 초기 상태, HikariCP 크기, VU 수, 메시지 수를 사용해야 한다.

### 9.1 `sync-db` 실행

API 서버가 종료된 상태에서 DB와 Redis를 복원한다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\performance\restore-baseline.ps1
```

API 서버를 실행한다.

```powershell
$env:CHAT_METADATA_WRITE_MODE="sync-db"
.\gradlew.bat bootRun --args="--spring.profiles.active=perf"
```

서버가 완전히 시작된 뒤 다른 터미널에서 부하를 실행한다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\performance\run-k6.ps1 `
  -Mode sync-db `
  -Vus 10 `
  -MessagesPerVu 20 `
  -SendIntervalMs 100 `
  -DrainSeconds 30
```

총 전송 메시지는 `VUs × MessagesPerVu`다. 위 조건에서는 200건이다.

### 9.2 `redis-write-back` 실행

`sync-db` 서버를 종료한다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\performance\restore-baseline.ps1
```

서버를 Write-Back 모드로 다시 실행한다.

```powershell
$env:CHAT_METADATA_WRITE_MODE="redis-write-back"
.\gradlew.bat bootRun --args="--spring.profiles.active=perf"
```

동일한 부하 조건으로 실행한다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\performance\run-k6.ps1 `
  -Mode redis-write-back `
  -Vus 10 `
  -MessagesPerVu 20 `
  -SendIntervalMs 100 `
  -DrainSeconds 30
```

### 9.3 권장 반복 순서

실행 순서에 따른 캐시 효과를 줄이려면 최소 세 번 반복한다.

```text
1회차: sync-db → redis-write-back
2회차: redis-write-back → sync-db
3회차: sync-db → redis-write-back
```

최종 결과는 한 번의 최솟값보다 세 번의 중앙값을 사용하는 편이 안전하다.

---

## 10. 단계별 부하 조건

처음부터 큰 부하를 주면 인증, STOMP destination, 테스트 데이터 오류와 실제 성능 문제를 구분하기 어렵다.

| 단계 | VU | 사용자당 메시지 | 전송 간격 | 총 메시지 | 목적 |
| --- | ---: | ---: | ---: | ---: | --- |
| 기능 확인 | 3 | 5 | 200ms | 15 | 연결, 구독, 저장, 수신 확인 |
| 기본 비교 | 10 | 20 | 100ms | 200 | 모드별 차이 확인 |
| 적체 확인 | 20 | 50 | 50ms | 1,000 | 커넥션 풀 대기와 tail latency 확인 |
| 한계 확인 | 50 | 100 | 20ms | 5,000 | 타임아웃과 Scheduler 처리 한계 확인 |

5,000건 테스트는 작은 테스트가 정상적으로 끝난 뒤 진행한다.

---

## 11. k6 결과에서 확인할 지표

결과 파일은 다음 위치에 생성된다.

```text
performance/results/{mode}-{timestamp}-summary.json
performance/results/{mode}-{timestamp}-samples.csv
```

### `chat_messages_sent`

k6가 STOMP SEND로 전송한 메시지 수다.

예상값:

```text
VUs × MessagesPerVu
```

이 값은 DB 저장 성공 수가 아니라 클라이언트가 전송을 시도한 수다.

### `chat_own_messages_received`

각 VU가 자신이 만든 `clientMessageId`를 WebSocket 응답에서 다시 확인한 수다.

`sent`보다 작으면 다음 가능성이 있다.

- 메시지 저장 실패
- WebSocket 전파 실패
- 구독 destination 오류
- Drain 시간이 부족함
- 서버 적체로 테스트 종료 이후에 응답이 도착함

### `chat_message_e2e_latency_ms`

클라이언트가 메시지를 SEND한 시각부터 동일한 `clientMessageId`를 WebSocket으로 다시 받은 시각까지 걸린 시간이다.

중요한 값:

- `avg`: 전체 평균
- `p(95)`: 메시지 95%가 이 시간 안에 도착
- `p(99)`: 가장 느린 상위 1%를 확인
- `max`: 최악의 단일 메시지 지연

평균만 낮고 P99 또는 Max가 매우 크면 일부 요청이 뒤에서 오래 대기한 것이다.

### `chat_post_send_receive_delay_ms`

해당 VU가 마지막 메시지를 보낸 이후에도 뒤늦게 도착한 메시지의 지연시간이다.

과거에 관찰한 "부하 테스트가 끝났는데 화면에는 메시지가 계속 도착하는 현상"과 가장 직접적으로 연결되는 지표다.

- 값이 거의 없거나 짧음: 전송 종료와 수신 종료가 거의 비슷함
- 수초 이상 증가: 서버 또는 메시지 처리 큐에 적체가 남아 있음
- 수십 초에 접근: `DrainSeconds`보다 큰 적체가 존재할 가능성

### `all_own_messages_received`

각 VU가 자신이 전송한 메시지를 Drain 시간 안에 모두 수신했는지를 나타내는 비율이다.

성능 개선보다 먼저 이 값이 100%에 가까운지 확인해야 한다.

### `stomp_errors`

STOMP ERROR 프레임, SockJS 종료 프레임, payload 파싱 오류 수다. 0이 정상이다.

---

## 12. Grafana에서 확인할 지표

Grafana에서 `Chatalk / Chatalk Redis Write-Back Comparison` 대시보드를 연다.

### HikariCP 활성 커넥션

```text
hikaricp_connections_active
```

현재 DB 작업에 사용 중인 커넥션 수다.

- 짧게 최대치에 닿는 것만으로 바로 문제는 아님
- `active == max` 상태가 지속되면서 `pending`이 증가하면 포화 상태

### HikariCP 최대 커넥션

```text
hikaricp_connections_max
```

이번 프로필의 기본값은 10이다. 두 모드에서 같은 값을 사용해야 비교가 성립한다.

### HikariCP 대기 요청

```text
hikaricp_connections_pending
```

DB 커넥션을 얻지 못해 기다리는 요청 수다.

이번 비교에서 가장 중요한 서버 지표 중 하나다.

- 0 유지: 커넥션 풀 대기 없음
- 순간적으로 1~2 발생 후 바로 감소: 짧은 경합
- 계속 증가하거나 높은 값 유지: 요청 유입 속도를 DB 처리 속도가 따라가지 못함

### DB 커넥션 획득 대기시간

```text
hikaricp_connections_acquire
```

요청이 사용 가능한 DB 커넥션을 얻기까지 걸린 시간이다.

- P95/P99가 증가하면 뒤쪽 요청이 커넥션 반환을 기다리고 있다는 의미
- `sync-db`에서 높고 Write-Back에서 낮다면 메타데이터 DB UPDATE 분리 효과를 뒷받침함

### 커넥션 획득 타임아웃

```text
hikaricp_connections_timeout_total
```

설정한 `connection-timeout` 안에 커넥션을 얻지 못한 횟수다.

0이 정상이다. 1 이상이면 단순 지연이 아니라 실제 요청 실패가 발생한 것이다.

### 유휴 커넥션

```text
hikaricp_connections_idle
```

즉시 사용할 수 있는 커넥션 수다. 부하 중 0으로 내려갈 수 있지만, `pending`과 함께 장시간 0이면 풀 포화를 의심한다.

### CPU 사용률

DB 대기 문제인지 애플리케이션 CPU 문제인지 구분하기 위한 보조 지표다.

- CPU는 낮은데 pending이 높음: DB 커넥션 또는 Lock 대기 가능성
- CPU가 지속적으로 높음: 직렬화, 브로드캐스트, 애플리케이션 연산 병목 가능성

---

## 13. Redis에서 확인할 값

Write-Back 모드 테스트 중 다음 명령을 실행한다.

```powershell
docker exec chatalk-perf-redis redis-cli SCARD chat:user:dirty
docker exec chatalk-perf-redis redis-cli SCARD chat:room:dirty
```

의미:

- 테스트 중 Dirty Set 증가: 정상
- Scheduler 실행 후 감소: 정상
- 부하 종료 후에도 계속 증가: Scheduler가 변경량을 따라가지 못함
- 오랫동안 줄지 않음: DB Batch 실패, Scheduler 미실행 또는 Redis 값 검증 실패 가능성

특정 채팅방 메타데이터 확인:

```powershell
docker exec chatalk-perf-redis redis-cli HGETALL chat:room:{roomId}:meta
```

특정 사용자 메타데이터 확인:

```powershell
docker exec chatalk-perf-redis redis-cli HGETALL chat:room:{roomId}:user:{userId}:meta
```

Write-Back 모드에서는 부하 종료 직후 Redis와 DB가 잠시 다를 수 있다. Scheduler가 한두 번 실행된 뒤 최종 일치 여부를 확인한다.

---

## 14. MySQL에서 확인할 값

테스트 도중 현재 실행 중인 쿼리를 확인한다.

```sql
SHOW FULL PROCESSLIST;
```

Lock 대기가 의심되는 경우:

```sql
SELECT *
FROM performance_schema.data_lock_waits;
```

현재 연결과 실행 Thread 수:

```sql
SHOW STATUS LIKE 'Threads_connected';
SHOW STATUS LIKE 'Threads_running';
```

테스트 종료 후 `performance/sql/verify-run.sql`의 `roomId`, `run_prefix`를 수정한 뒤 실행한다.

검증 항목:

1. 전송한 메시지 수와 DB 저장 메시지 수가 일치하는가
2. 중복 `clientMessageId`가 없는가
3. `chat_room.last_message_id`가 실제 최신 메시지 ID와 같은가
4. 사용자별 `chat_list` 메타데이터가 최종 상태와 맞는가

---

## 15. 의미 있는 비교 수치

다음 값은 README와 포트폴리오에 사용할 가치가 높다.

| 지표 | 이유 |
| --- | --- |
| E2E P95 | 대부분 사용자가 경험하는 상위 지연을 보여 줌 |
| E2E P99 | 일부 요청이 심하게 밀리는지 보여 줌 |
| E2E Max | 최악의 요청 적체를 보여 줌 |
| Post-send tail Max | 테스트 종료 후 메시지가 계속 도착한 현상을 수치화 |
| Hikari pending Max | 커넥션 풀 앞에서 대기한 요청 규모 |
| 커넥션 acquire P95/P99 | 커넥션을 얻는 데 걸린 시간 |
| Connection timeout | 실제 요청 실패 여부 |
| 저장/수신 누락 수 | 성능과 함께 정합성을 검증 |
| Dirty Set 최종 크기 | Scheduler가 작업을 모두 따라잡았는지 확인 |

DB UPDATE 횟수를 정확히 비교하려면 MySQL `performance_schema` statement summary를 테스트 시작 전에 초기화하고 종료 후 집계해야 한다. 환경마다 설정이 달라질 수 있으므로 1차 비교에서는 Hikari pending, acquire time, E2E tail을 우선한다.

---

## 16. 결과 기록 표

각 실행의 원본 수치는 별도로 남기고, 최종 문서에는 세 번 실행의 중앙값을 기록한다.

| 항목 | `sync-db` | `redis-write-back` | 해석 |
| --- | ---: | ---: | --- |
| 총 전송 수 |  |  | 동일해야 함 |
| WebSocket 수신 수 |  |  | 전송 수와 일치해야 함 |
| DB 저장 수 |  |  | 전송 수와 일치해야 함 |
| E2E 평균 |  |  | 보조 지표 |
| E2E P95 |  |  | 핵심 지표 |
| E2E P99 |  |  | 핵심 지표 |
| E2E Max |  |  | 최악 지연 |
| Post-send tail Max |  |  | 잔여 처리 시간 |
| Hikari active Max |  |  | 최대치 도달 여부 |
| Hikari pending Max |  |  | 핵심 지표 |
| Acquire P95/P99 |  |  | 커넥션 대기시간 |
| Connection timeout |  |  | 0이어야 함 |
| Dirty Set 최종 크기 | 해당 없음 |  | 0이어야 함 |
| 중복/누락 |  |  | 모두 0이어야 함 |

---

## 17. 결과 해석 예시

### Write-Back 적용 효과가 명확한 경우

```text
sync-db에서는 active 커넥션이 최대치 10에 장시간 도달했고 pending 요청이 증가했다.
Write-Back에서는 pending이 거의 발생하지 않았으며 E2E P99와 테스트 종료 후 tail latency가 감소했다.
```

이 경우 메타데이터 DB 갱신을 요청 경로에서 분리한 것이 커넥션 점유와 적체 완화에 기여했다고 설명할 수 있다.

### 평균 차이는 작지만 tail 차이가 큰 경우

```text
평균 지연은 두 방식이 비슷했지만 sync-db의 P99와 Max가 크게 증가했다.
```

평균 성능 향상보다 부하 상황의 일부 요청이 오래 밀리는 문제를 완화한 것으로 해석한다.

### 두 방식의 차이가 거의 없는 경우

가능한 원인:

- 현재 DB 스키마와 인덱스가 과거보다 개선됨
- 부하가 병목을 만들 만큼 크지 않음
- 채팅방 멤버 수가 적어 메타데이터 UPDATE 수가 적음
- 실제 병목이 DB가 아니라 브로드캐스트 또는 클라이언트 렌더링에 있음

차이가 없다고 실패한 실험은 아니다. 현재 조건에서 Redis Write-Back이 큰 이점을 주지 않았다는 결과도 중요한 판단 근거다.

### Write-Back에서 Dirty Set이 계속 증가하는 경우

요청 응답은 빨라졌지만 Scheduler가 DB 반영량을 따라가지 못하는 상태다. 이 경우 다음을 추가로 검토한다.

- Scheduler 주기
- 한 번에 처리하는 Dirty Key 수
- Batch Update 실행시간
- 여러 서버 인스턴스에서 같은 Scheduler가 실행되는 문제
- Redis 장애 복구와 중복 반영의 멱등성

---

## 18. 포트폴리오용 증거 자료

사진을 많이 넣을 필요는 없다. 다음 세 가지가 가장 유용하다.

1. 동일 시간 범위의 Grafana 대시보드 캡처
   - active/max
   - pending
   - acquire P95/P99
2. 두 모드의 k6 요약 결과
   - E2E P95/P99/Max
   - Post-send tail Max
3. 정합성 검증 결과
   - DB 저장 수
   - 중복 0건
   - Dirty Set 최종 0건

로그 전체 화면보다 비교표와 그래프가 우선이다.

---

## 19. README에 반영하는 시점

실험 전에는 README에 개선 수치를 적지 않는다.

실험이 끝난 뒤 다음 순서로 정리한다.

```text
원본 k6/Grafana 결과 보관
→ 세 번 실행 결과 표 작성
→ 결과 해석과 한계 작성
→ README에는 핵심 결과만 요약
→ 상세 과정은 이 문서로 연결
```

README 예시:

```markdown
### DB 커넥션 풀 포화와 요청 적체 개선

현재 스키마에서 메타데이터를 요청마다 DB에 즉시 갱신하는 기준 구조와
Redis Write-Back 구조를 동일 부하 조건으로 비교했습니다.

- E2E P95: A ms → B ms
- Hikari pending Max: A → B
- 테스트 종료 후 tail latency: A ms → B ms
- 메시지 누락·중복: 0건

[상세 테스트 환경과 결과](docs/performance/redis-write-back-comparison.md)
```

실제 측정값이 없는 상태에서 감소율이나 개선율을 먼저 작성하지 않는다.

---

## 20. 주의할 점

- 이번 `sync-db` 모드는 과거 코드를 완전히 복원한 것이 아니다.
- 현재 스키마에서 메타데이터 저장 방식만 동기 DB 방식으로 재구성한 기준 모델이다.
- 두 모드의 로깅 수준, HikariCP 크기, DB 데이터, 채팅방 멤버 수를 동일하게 유지한다.
- Write-Back은 요청 지연을 줄일 수 있지만 DB와 Redis의 일시적 불일치를 허용한다.
- 부하 테스트 결과는 테스트 PC, Docker 자원, DB 상태에 따라 달라질 수 있다.
- 한 번의 최고 결과보다 반복 실행의 중앙값과 재현 가능한 절차가 더 중요하다.

---

## 21. 문제 해결

### WebSocket 연결이 101이 아닌 경우

- API 서버가 7070 포트에서 실행 중인지 확인
- `SOCKJS_ENDPOINT`가 `/api/ws-chat`인지 확인
- Docker에서 호스트 접근이 가능한지 확인

```powershell
$env:WS_BASE_URL="ws://host.docker.internal:7070"
```

### STOMP ERROR가 발생하는 경우

- JWT에 `Bearer `를 직접 붙이지 않았는지 확인
- JWT와 `USER_ID`가 같은 사용자인지 확인
- 사용자가 해당 채팅방 멤버인지 확인
- 토큰 만료 여부 확인

### 메시지는 저장되지만 수신되지 않는 경우

- 구독 destination 정책이 변경되지 않았는지 확인
- 현재 스크립트의 destination은 다음과 같다.

```text
/user/{userId}/api/sub/chat/rooms/{roomId}
```

### Prometheus target이 DOWN인 경우

API 서버에서 다음 주소가 열리는지 먼저 확인한다.

```text
http://localhost:7070/actuator/prometheus
```

Prometheus target 화면:

```text
http://localhost:9091/targets
```

### `all_own_messages_received`가 낮은 경우

`DrainSeconds`를 먼저 늘린다.

```powershell
-DrainSeconds 60
```

Drain 시간을 늘려 수신되면 유실이 아니라 처리 적체일 가능성이 높다. Drain 시간을 늘려도 수신되지 않으면 저장 실패, 브로드캐스트 오류, destination 문제를 확인한다.

---

## 22. 인프라 종료

데이터 볼륨을 유지하고 종료한다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\performance\stop-infra.ps1
```

컨테이너와 모든 성능 테스트 데이터를 삭제한다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\performance\stop-infra.ps1 -ResetVolumes
```
