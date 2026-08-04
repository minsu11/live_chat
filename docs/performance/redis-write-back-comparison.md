# Redis Write-Back 적용에 따른 채팅 메타데이터 처리 개선

## 1. 문서 목적

Chatalk은 메시지를 전송할 때 메시지 원문뿐 아니라 다음과 같은
채팅방 및 사용자별 메타데이터도 함께 갱신한다.

- 채팅방의 마지막 메시지 ID
- 마지막 메시지 미리보기
- 마지막 메시지 시각
- 사용자별 안 읽은 메시지 수
- 마지막으로 읽은 메시지 ID
- 마지막 채팅방 진입 시각

기존 구조에서는 이러한 메타데이터를 메시지 요청 트랜잭션 안에서
MySQL에 즉시 반영했다.

동일 그룹 채팅방에 여러 사용자가 동시에 메시지를 전송하자
`chat_room`과 `chat_list`에 대한 갱신이 집중됐고,
데드락과 트랜잭션 롤백, DB 커넥션 풀 포화가 발생했다.

이 문서는 해당 문제를 재현하고 Redis Write-Back 구조 적용 전후를
동일한 조건에서 비교한 결과를 정리한다.

---

## 2. 비교 대상

메시지 원문은 두 모드 모두 MySQL의 `chat_message` 테이블에
즉시 저장한다.

비교 대상은 메시지 원문 저장 방식이 아니라,
채팅 메타데이터를 반영하는 방식이다.

### sync-db

```text
메시지 요청
→ chat_message 저장
→ chat_room 메타데이터 UPDATE
→ 사용자별 chat_list UPDATE
→ 트랜잭션 종료
```

메시지 요청 트랜잭션이 완료되기 위해
채팅방 메타데이터와 참여자별 메타데이터 DB 갱신까지 기다린다.

### Redis Write-Back

```text
메시지 요청
→ chat_message 저장
→ Redis Hash 메타데이터 갱신
→ Redis Dirty Set 등록
→ 요청 처리 종료

Scheduler
→ Dirty Set 조회
→ JdbcTemplate Batch Update
→ DB 반영 성공 항목만 Dirty Set에서 제거
```

반복적으로 갱신되는 메타데이터를 요청 경로에서 분리하고,
스케줄러가 모아서 DB에 반영하도록 변경했다.

---

## 3. 검증 가설

이번 테스트에서는 다음 내용을 확인하고자 했다.

1. `sync-db` 방식에서 동일 채팅방의 동시 메타데이터 갱신이
   DB 락 경합과 데드락을 유발하는가
2. 데드락과 트랜잭션 적체가 메시지 저장 및 실시간 수신에
   영향을 주는가
3. Redis Write-Back으로 메타데이터 DB 쓰기를 요청 경로에서
   분리했을 때 동일 조건에서 메시지를 모두 처리할 수 있는가
4. Redis Dirty Set에 기록된 메타데이터가 최종적으로 DB에 반영되고
   Dirty Set이 비워지는가

---

## 4. 테스트 환경

| 구분 | 설정 |
| --- | --- |
| API 서버 | Spring Boot |
| 실시간 통신 | WebSocket / STOMP / SockJS |
| 데이터베이스 | MySQL 8 |
| 메타데이터 저장소 | Redis |
| 부하 도구 | k6 |
| 테스트 프로필 | `perf` |
| MySQL 포트 | `3307` |
| Redis 포트 | `6380` |
| HikariCP 최대 크기 | 10 |
| 테스트 채팅방 | 동일 그룹 채팅방 1개 |
| 테스트 사용자 | 서로 다른 사용자 3명 |
| 사용자당 메시지 | 20건 |
| 총 전송 메시지 | 60건 |
| 메시지 전송 간격 | 100ms |
| 전송 종료 후 대기 | 30초 |

각 VU는 서로 다른 사용자 토큰을 사용했다.

메시지 내용에는 Run ID, VU 번호, 메시지 순서를 포함해
각 메시지를 고유하게 구분했다.

```text
perf-message-{runId}-vu{vu}-msg{index}
```

---

## 5. 측정 지표

### 전송 및 수신 정합성

- `chat_messages_sent`
   - k6가 STOMP SEND로 전송한 메시지 수
- `chat_own_messages_received`
   - 각 VU가 자신이 보낸 메시지를 다시 수신한 수
- `all_own_messages_received`
   - 각 VU가 자신이 전송한 메시지를 모두 받았는지 여부

### 지연시간

- `chat_message_e2e_latency_ms`
   - 메시지를 전송한 시점부터 동일 메시지를 WebSocket으로
     다시 수신할 때까지의 시간
- `chat_post_send_receive_delay_ms`
   - 해당 VU가 마지막 메시지를 보낸 후에도 뒤늦게 도착한 메시지의 지연

### DB 정합성

- 테스트 Run ID에 해당하는 `chat_message` 저장 건수
- 고유 `message_content` 개수
- VU별 저장 건수

### 장애 여부

- MySQL 데드락 발생 여부
- 데드락 누적 카운터의 테스트 전후 차이
- `UnexpectedRollbackException`
- HikariCP 커넥션 획득 실패
- Redis Dirty Set 최종 크기
- 서버 오류 로그

---

## 6. sync-db 문제 재현 결과

### 실행 정보

```text
Run ID: sync-db-20260730-150406
VU: 3
사용자당 메시지: 20
총 전송: 60
전송 간격: 100ms
Drain: 30초
```

### k6 결과

| 지표 | 결과 |
| --- | ---: |
| 전송 메시지 | 60 |
| 자기 메시지 수신 | 56 |
| 모든 메시지 수신 성공 VU | 0 / 3 |
| E2E 평균 | 3,631.73ms |
| E2E p95 | 7,903.50ms |
| E2E p99 | 7,945.15ms |
| E2E 최대 | 7,960ms |
| Post-send tail 최대 | 6,459ms |
| STOMP ERROR 프레임 | 0 |

전송 종료 이후에도 최대 6.4초 동안 메시지가 뒤늦게 도착했다.

### DB 저장 결과

| 항목 | 결과 |
| --- | ---: |
| 총 전송 | 60 |
| DB 저장 | 46 |
| 고유 메시지 | 46 |
| 저장 성공률 | 76.7% |

VU별 저장 건수는 다음과 같았다.

| VU | 전송 | DB 저장 |
| ---: | ---: | ---: |
| 1 | 20 | 16 |
| 2 | 20 | 17 |
| 3 | 20 | 13 |

특정 사용자 하나의 문제라기보다 동일 채팅방에 동시에 접근한
모든 사용자에게 저장 누락이 분산되어 나타났다.

---

## 7. 장애 증거

### 7.1 MySQL 데드락

테스트가 실행된 `15:04` 시각의 애플리케이션 로그에서
MySQL 오류 1213이 반복해서 발생했다.

```text
SQL Error: 1213
SQLState: 40001
Deadlock found when trying to get lock
```

데드락 이후에는 트랜잭션이 rollback-only 상태로 전환돼
다음 예외가 발생했다.

```text
UnexpectedRollbackException
Transaction silently rolled back because it has been marked as rollback-only
```

### 7.2 잠금 순서 충돌

`sync-db-20260730-150406` 실행에서 데드락이 발생했다는 사실은
테스트 시각인 `15:04`의 애플리케이션 로그를 통해 확인했다.

이후 후속 재현에서 `SHOW ENGINE INNODB STATUS`로 조회한
최신 데드락 상세에서는 다음과 같은 잠금 순서 충돌이 확인됐다.

```text
Transaction A
chat_list 레코드 X Lock 보유
→ chat_room 레코드 X Lock 대기

Transaction B
chat_room 레코드 Lock 보유
→ chat_list 레코드 X Lock 대기

MySQL
→ Transaction A rollback
```

두 트랜잭션이 서로 상대방이 보유한 락을 기다리면서
순환 대기 상태가 만들어졌다.

### 7.3 DB 커넥션 풀 포화

데드락과 락 대기가 이어진 뒤 HikariCP에서도
커넥션 획득 실패가 발생했다.

```text
ChatalkPerfPool - Connection is not available
request timed out after 3000ms

total=10
active=10
idle=0
waiting=20~21
```

모든 커넥션이 사용 중인 상태에서 뒤에 들어온 요청들이
커넥션 풀에서 대기했고, 일부 요청은 제한 시간 안에
커넥션을 얻지 못했다.

### 장애 흐름

```text
동일 그룹방에 동시 메시지 요청
→ chat_room / chat_list UPDATE 집중
→ 서로 다른 잠금 획득 순서
→ MySQL 데드락
→ 일부 트랜잭션 롤백
→ 락 대기와 요청 적체
→ HikariCP active 커넥션 최대치 도달
→ 커넥션 획득 시간 초과
→ 메시지 저장 및 실시간 처리 실패
```

누락된 14건 모두가 단일 원인으로 실패했다고 구분할 수는 없다.

다만 동일 실행 안에서 다음 현상이 함께 발생한 것은 확인했다.

- MySQL 데드락
- 트랜잭션 롤백
- DB 커넥션 풀 포화
- 메시지 DB 저장 누락
- 실시간 수신 누락 및 지연

---

## 8. 개선 내용

요청 경로에서 반복적으로 실행되던 메타데이터 DB UPDATE를
Redis Write-Back으로 분리했다.

### 채팅방 메타데이터

- `lastMessageId`
- `lastMessagePreview`
- `lastMessageAt`

### 사용자별 메타데이터

- `unreadCount`
- `lastReadMessageId`
- `lastOpenedAt`

### Redis 자료구조

```text
Redis Hash
└─ 실제 메타데이터 값 저장

Redis Dirty Set
└─ DB 반영이 필요한 roomId 또는 roomId:userId 저장
```

### Scheduler 처리 원칙

1. Dirty Set을 조회한다.
2. Redis Hash에서 최신 메타데이터를 읽는다.
3. JdbcTemplate Batch Update로 DB에 반영한다.
4. DB 반영에 성공한 Dirty Key만 제거한다.
5. DB 반영이 실패한 항목은 Dirty Set에 유지해 다음 주기에 재시도한다.

같은 채팅방에 짧은 시간 동안 여러 메시지가 들어오더라도
DB에는 매 요청마다 UPDATE하지 않고, Redis에 저장된 최신 값을
배치 단위로 반영한다.

---

## 9. Redis Write-Back 결과

### 1회차 실행 결과

```text
Run ID: redis-write-back-20260730-173658
VU: 3
사용자당 메시지: 20
총 전송: 60
전송 간격: 100ms
Drain: 30초
```

| 지표 | 결과 |
| --- | ---: |
| 전송 메시지 | 60 |
| 자기 메시지 수신 | 60 |
| 모든 메시지 수신 성공 VU | 3 / 3 |
| E2E 평균 | 608.85ms |
| E2E p95 | 969.55ms |
| E2E p99 | 1,221.76ms |
| E2E 최대 | 1,243ms |
| Post-send tail 최대 | 435ms |
| STOMP 연결 성공 | 3 / 3 |

### DB 및 Write-Back 검증

| 검증 항목 | 결과 |
| --- | ---: |
| DB 저장 | 60 |
| 고유 메시지 | 60 |
| VU 1 저장 | 20 |
| VU 2 저장 | 20 |
| VU 3 저장 | 20 |
| 데드락 테스트 전 | 44 |
| 데드락 테스트 후 | 44 |
| 데드락 증가량 | 0 |
| `chat:user:dirty` | 0 |
| `chat:room:dirty` | 0 |
| 오류 로그 | 없음 |

`chat_room.last_message_id`, `last_message_preview`,
`last_message_at`도 테스트의 마지막 메시지 기준으로 DB에 반영됐다.

`44`는 Redis Write-Back 실행에서 발생한 데드락 수가 아니라,
MySQL 컨테이너가 시작된 이후의 누적 카운터다.

이번 실행에서는 테스트 직전과 직후 값이 모두 44였으므로
평가에 사용한 값은 누적 수 자체가 아니라 증가량 `0`이다.

### 반복 실행 결과

Redis Write-Back 구조는 동일한 조건에서 총 3회 반복해 검증했다.

각 실행 조건은 다음과 같다.

- 동시 사용자: 3명
- 사용자당 메시지: 20건
- 총 전송 메시지: 60건
- 전송 간격: 100ms
- 전송 종료 후 대기: 30초

| 회차 | Run ID | 전송 | 수신 | E2E 평균 | E2E p95 | E2E p99 | E2E 최대 | Tail 최대 |
| ---: | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | `redis-write-back-20260730-173658` | 60 | 60 | 608.85ms | 969.55ms | 1,221.76ms | 1,243ms | 435ms |
| 2 | `redis-write-back-20260730-195841` | 60 | 60 | 1,130.63ms | 1,392.15ms | 1,482.31ms | 1,536ms | 937ms |
| 3 | `redis-write-back-20260730-225803` | 60 | 60 | 1,864.67ms | 2,168.10ms | 2,506.82ms | 2,921ms | 1,962ms |

세 실행 모두 k6 결과에서 다음 조건을 만족했다.

- 전송 메시지 60건
- 자기 메시지 60건 전체 수신
- `all_own_messages_received = 1`

이 중 별도 검증 명령을 수행한
`redis-write-back-20260730-173658`,
`redis-write-back-20260730-225803` 실행에서는
다음 항목도 확인했다.

- DB 저장 60건
- 고유 메시지 60건
- VU별 20건씩 저장
- 데드락 증가량 0
- 사용자 Dirty Set 최종 0
- 채팅방 Dirty Set 최종 0
- 데드락, 트랜잭션 롤백, 커넥션 풀 포화 로그 없음

실행마다 지연시간 편차는 있었지만,
세 번 모두 k6 기준으로 전송한 메시지를 전체 수신했다.

별도 DB 및 Redis 검증을 수행한 실행에서도 메시지 저장 누락이나
데드락 증가 없이 최종 DB 반영까지 완료됐다.

### 반복 실행 대표값

실행별 환경 편차의 영향을 줄이기 위해 세 번의 최솟값이 아닌
중앙값을 대표값으로 사용했다.

| 지표 | 3회 중앙값 |
| --- | ---: |
| E2E 평균 | 1,130.63ms |
| E2E p95 | 1,392.15ms |
| E2E p99 | 1,482.31ms |
| E2E 최대 | 1,536ms |
| Post-send tail 최대 | 937ms |


---

## 10. 결과 비교

| 항목 | sync-db | Redis Write-Back |
| --- | ---: | ---: |
| 동시 사용자 | 3 | 3 |
| 총 전송 | 60 | 60 |
| 자기 메시지 수신 | 56 | 60 |
| DB 저장 | 46 | 60 |
| DB 고유 메시지 | 46 | 60 |
| 저장 성공률 | 76.7% | 100% |
| 모든 메시지 수신 VU | 0 / 3 | 3 / 3 |
| E2E 평균 | 3,631.73ms | 1,130.63ms |
| E2E p95 | 7,903.50ms | 1,392.15ms |
| E2E p99 | 7,945.15ms | 1,482.31ms |
| E2E 최대 | 7,960ms | 1,536ms |
| Post-send tail 최대 | 6,459ms | 937ms |
| MySQL 데드락 | 발생 | 테스트 중 증가 없음 |
| 트랜잭션 롤백 | 발생 | 확인되지 않음 |
| 커넥션 풀 포화 | 발생 | 확인되지 않음 |
| Dirty Set 최종 상태 | 해당 없음 | 상세 검증 실행에서 0 |

Redis Write-Back 열의 지연시간은 세 번의 반복 실행에서 구한
중앙값을 사용했다.

sync-db보다 낮은 지연시간을 보였지만,
sync-db 실행은 일부 요청이 실패한 상태다.

따라서 이 결과를 단순히
“Redis가 DB보다 몇 배 빠르다”라고 해석하지 않는다.

이번 비교에서 더 중요한 결과는 다음과 같다.

- sync-db는 동일 조건에서 일부 메시지를 처리하지 못했다.
- Redis Write-Back은 반복 실행에서 메시지를 모두 처리했다.
- 별도 검증 명령을 수행한 Redis Write-Back 실행에서는
  데드락 누적 수가 증가하지 않았다.
- 해당 실행에서 Dirty Set이 최종적으로 비워져
  메타데이터의 DB 반영까지 완료됐다.
- 요청 종료 후 남아 있던 tail latency가 크게 감소했다.

---

## 11. 결론

기존 sync-db 구조에서는 동일 채팅방의 메타데이터를
메시지 요청마다 DB에 즉시 반영하면서
`chat_room`과 `chat_list`에 쓰기 경합이 집중됐다.

그 결과 동일 조건의 테스트에서 다음 문제가 발생했다.

- MySQL 데드락
- 트랜잭션 롤백
- HikariCP 커넥션 풀 포화
- 메시지 DB 저장 누락
- WebSocket 수신 누락 및 지연

Redis Write-Back 적용 후에는 메타데이터 DB 쓰기를
메시지 요청 경로에서 분리했다.

동일 조건으로 수행한 세 번의 반복 테스트에서는
모두 전송한 메시지 60건 전체 수신을 확인했다.

이 중 별도 DB 및 Redis 검증을 수행한 실행에서는
다음 항목도 확인했다.

- 60건 전체 DB 저장
- VU별 20건씩 저장
- 데드락 증가량 0
- Dirty Set 최종 0
- 채팅방 메타데이터 최종 DB 반영
- 롤백 및 커넥션 풀 포화 로그 없음

이를 통해 해당 테스트 조건에서 Redis Write-Back 구조가
요청 경로의 DB 락 경합을 줄이고, 반복 실행에서 메시지 수신 안정성을
확보했으며, 상세 검증 실행에서는 최종 DB 반영까지 완료한 것을 확인했다.
---

## 12. 결과 해석의 한계

이번 결과에는 다음과 같은 한계가 있다.

1. 로컬 단일 API 서버 환경에서 수행했다.
2. 동시 사용자는 3명으로 실제 운영 트래픽보다 작다.
3. sync-db 실행은 일부 메시지가 실패했으므로
   순수한 지연시간만 비교한 테스트는 아니다.
4. Redis 장애 또는 네트워크 단절 상황은 이번 비교에 포함하지 않았다.
5. 데드락과 커넥션 풀 포화가 각각 몇 건의 메시지 실패에
   직접 연결됐는지는 요청별 추적 ID가 없어 구분하지 못했다.
6. Redis Write-Back은 즉시 일관성 대신 최종 일관성을 사용하므로
   서버 종료 및 Redis 장애 시 복구 전략을 별도로 검증해야 한다.

따라서 이번 결과는 모든 환경에서 데드락이 제거됐다는 의미가 아니라,
**정의한 동일 조건의 테스트에서 요청 경로의 메타데이터 DB 쓰기를
분리한 뒤 데드락 증가 없이 메시지를 모두 처리했다는 결과**로 한정한다.

---

## 13. 실행 방법

비교 재현 코드는 다음 브랜치에 보관한다.

```text
perf/redis-write-back-comparison
```

### 인프라 실행

```powershell
& .\scripts\performance\start-infra.ps1
```

### sync-db 모드

```powershell
$env:CHAT_METADATA_WRITE_MODE = "sync-db"

.\gradlew.bat bootRun `
  --args="--spring.profiles.active=perf" `
  --console=plain
```

### Redis Write-Back 모드

```powershell
$env:CHAT_METADATA_WRITE_MODE = "redis-write-back"

.\gradlew.bat bootRun `
  --args="--spring.profiles.active=perf" `
  --console=plain
```

### 부하 테스트

```powershell
& .\scripts\performance\run-k6.ps1 `
  -Mode redis-write-back `
  -Vus 3 `
  -MessagesPerVu 20 `
  -SendIntervalMs 100 `
  -DrainSeconds 30
```

`run-k6.ps1`의 `Mode` 값은 결과 파일과 테스트 메타데이터를
구분하기 위한 값이다.

실제 API 서버의 메타데이터 처리 방식은 서버 시작 전에 설정한
`CHAT_METADATA_WRITE_MODE`에 의해 결정된다.

---

## 14. 관련 자료

```text
docs/performance/redis-write-back-comparison.md
docs/performance/redis-write-back-3runs.csv
scripts/performance/
performance/docker-compose.yml
performance/prometheus/
performance/grafana/
src/main/resources/application-perf.yml
```

전체 원본 로그와 DB snapshot에는 테스트 계정 정보가 포함될 수 있어
Git 저장소에는 필요한 발췌본과 요약 결과만 보관한다.