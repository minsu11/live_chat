# Chatalk API Server

Spring Boot와 WebSocket/STOMP로 구현한 실시간 채팅 API 서버입니다.

처음에는 1:1 메시지 송수신 기능부터 시작했지만, 실제로 서비스를 운영하려면 연결이 끊겼을 때의 메시지 복구, 사용자별 읽음 상태, 채팅방 목록 정합성, 파일 업로드 실패, Redis 장애처럼 정상 흐름 밖의 문제를 함께 다뤄야 했습니다. 현재는 이러한 문제를 기능 구현과 테스트 코드로 확인하면서 안정성을 보강하고 있습니다.

## 목차

1. 프로젝트 구성
2. 기술 스택
3. 주요 기능
4. 핵심 처리 흐름
5. Redis Write-Back
6. 테스트 전략
7. Troubleshooting
8. WebSocket / STOMP 경로
9. 주요 API
10. 로컬 실행
11. 패키지 구조
12. 향후 개선
13. 관련 문서

## 프로젝트 링크

- 배포 서비스: https://chatalk.store
- Front Repository: https://github.com/minsu11/live_chat_front
- Auth Server: https://github.com/minsu11/live_chat_auth
- API Server: https://github.com/minsu11/live_chat
- [요약 포트폴리오](docs/portfolio/ParkMinsu_Chatalk_Summary_Portfolio.pdf)
- [상세 포트폴리오](docs/portfolio/ParkMinsu_Chatalk_Detail_Portfolio.pdf)
- Notion 상세 정리: https://ms-pt.notion.site/343b77b258e780bfac21d407cc70ac72?pvs=74

---

## 핵심 성과

| 항목 | 결과 |
| --- | --- |
| `sync-db` 문제 재현 | 수신 56/60 · DB 저장 46/60 |
| Redis Write-Back | 3회 모두 수신 60/60 |
| DB·Redis 상세 검증 | DB 60/60 · Deadlock 증가량 0 · Dirty Set 0 |
| Redis Pub/Sub 멀티 인스턴스 | 서로 다른 API 인스턴스에 연결된 STOMP 사용자 간 실시간 전달 검증 |
| 테스트 | 326개 / 실패 0개 |
| Coverage | Line 64.36% · Branch 65.23% |

---



## 전체 아키텍처

![project_architecture.png](docs%2Fimage%2Fproject_architecture.png)

## 1. 프로젝트 구성

Chatalk은 화면, 인증, 채팅 도메인의 책임을 나누기 위해 세 저장소로 구성했습니다.

| 서버 | 역할 | 저장소 |
| --- | --- | --- |
| Vue SPA / Front Repository | Vue 기반 채팅 UI, WebSocket 연결, 메시지 렌더링 | `live_chat_front` |
| Auth Server | 로그인, JWT 발급·재발급, OAuth2 인증 | `live_chat_auth` |
| API Server | 채팅방, 메시지, 읽음, 파일, 검색, Redis 동기화 | 현재 저장소 |

### 인증 요청 흐름

**일반 로그인**
```text
Client
→ API Server
→ Feign Client
→ Auth Server
→ 로그인 검증 및 Access/Refresh Token 발급
→ API Server
→ Client
```

**OAuth2**
```text
Client
→ Nginx `/a/**`
→ Auth Server
→ OAuth2 Provider
```

### API 서버의 주요 책임

- 1:1·그룹 채팅방과 멤버십 관리
- STOMP 기반 실시간 메시지 송수신
- 사용자별 읽음 상태와 안 읽은 메시지 수 관리
- WebSocket 재연결 이후 누락 메시지 복구
- 이미지·파일 메시지 업로드와 고아 첨부파일 정리
- Redis 메타데이터 Write-Back 및 Pub/Sub
- 채팅방 내 메시지 검색과 검색 결과 문맥 조회

---

## 2. 기술 스택

### Backend

- Java 21
- Spring Boot 3.4.3
- Spring Security
- Spring WebSocket / STOMP
- Spring Cloud OpenFeign
- Spring Data JPA
- Querydsl
- Spring Data Redis
- Resilience4j
- MySQL 8
- Redis

### Test / Quality

- JUnit 5
- Mockito
- AssertJ
- JaCoCo
- SonarQube 연동 설정
- GitHub Actions

### Deployment

- Docker
- Nginx
- Cloudflare - DNS / Proxy / SSL
- GitHub Actions

---

## 3. 주요 기능

### 채팅방과 멤버십

- 기존 1:1 채팅방 조회 또는 신규 생성
- 그룹 채팅방 생성과 멤버 초대
- 채팅방 멤버 목록 조회
- 채팅방 나가기
- 비멤버의 채팅방 접근 차단
- DM / GROUP / OPEN 타입 확장을 고려한 모델 구성

### 메시지와 실시간 전송

- `@MessageMapping`을 통한 메시지 수신
- TEXT / EMOJI / IMAGE / FILE / SYSTEM 메시지 처리
- 메시지 DB 저장 후 사용자별 WebSocket destination으로 전파
- 채팅방 목록 갱신 이벤트와 알림 이벤트 전파
- 차단된 사용자에게는 메시지 알림을 보내지 않도록 분기

### 메시지 조회와 재연결 복구

- 채팅방 진입 시 최근 메시지 조회
- 커서 기반 과거 메시지 조회
- `afterMessageId` 이후의 메시지만 조회하는 catch-up API
- WebSocket 재연결 시 마지막 수신 메시지 이후 구간 복구
- 일반 페이지네이션과 재연결 복구 API의 목적 분리

### 읽음과 안 읽음 상태

- 사용자별 `lastReadMessageId` 관리
- 채팅방 목록의 `unreadCount`와 메시지별 unread count 분리
- 채팅방 진입 시 읽음 상태 반영
- `READ_UPDATED` 이벤트로 메시지별 unread count 갱신
- 신규 멤버 초대 시점에 읽음 기준 초기화

### 채팅방 표시 이름과 설정

조회자에 따라 채팅방 제목이 달라질 수 있어 표시 이름 계산을 별도 Resolver로 분리했습니다.

1. 사용자가 직접 지정한 커스텀 이름
2. 채팅방에 저장된 기본 이름
3. DM은 상대방 표시 이름
4. GROUP은 다른 참여자의 이름 조합
5. OPEN은 기본 문구

추가로 채팅방 이름 변경, 알림 음소거, 멤버 조회, 초대, 나가기 기능을 제공합니다.

### 첨부파일

- 프로필 이미지 업로드
- 채팅 이미지와 일반 파일 업로드
- 첨부파일을 먼저 업로드한 뒤 메시지와 연결
- 파일 크기, MIME type, 확장자 검증
- 메시지와 연결되지 않은 오래된 첨부파일 cleanup

### 메시지 검색

- 채팅방 단위 키워드 검색
- MySQL Full-Text Search와 ngram parser 사용
- 검색 누락을 보완하기 위한 LIKE fallback
- 검색 결과 커서 페이지네이션
- 특정 검색 결과를 기준으로 앞뒤 메시지 문맥 조회

---

## 4. 핵심 처리 흐름

### 메시지 전송

```text
STOMP 메시지 수신
→ 채팅방 멤버 검증
→ 메시지 DB 저장
→ 첨부파일 연결
→ Redis 메타데이터 갱신
→ Redis Publish
→ Redis Subscriber
→ STOMP 브로드캐스트
→ 채팅방 목록 / 알림 이벤트 전파
```

메시지가 DB에 저장되기 전에 WebSocket 전파가 먼저 실행되면 클라이언트에는 보이지만 재조회할 수 없는 메시지가 생길 수 있습니다. 그래서 저장 실패 시 브로드캐스트와 후속 메타데이터 갱신이 실행되지 않는지 테스트로 확인했습니다.

### Redis Pub/Sub 멀티 인스턴스 검증

Redis Pub/Sub 기반 메시지 전파가 단일 API 인스턴스 내부에서만 동작하는지 확인하는 데 그치지 않고,
로컬에서 API 서버 2개 인스턴스를 실행해 실제 인스턴스 간 메시지 전달을 검증했습니다.

```text
Front B
→ API B
→ Redis Publish
→ Redis `chatroom`
→ API A Redis Subscriber
→ STOMP Broadcast
→ Front A

```
**검증 환경**

- API A: `localhost:7070`
- API B: `localhost:7071`
- Front A: `localhost:8080`
- Front B: `localhost:8081`
- Shared Redis / MySQL
- Redis Channel: `chatroom`

**검증 결과**

- `PUBSUB NUMSUB chatroom`: Subscriber 2개 확인
- 서로 다른 API 인스턴스에 연결된 사용자 간 실시간 메시지 전달 확인
- 송신 인스턴스의 Redis Publish와 수신 인스턴스의 Redis Subscriber 동작 확인
- 동일 메시지 ID `252693`이 수신 인스턴스에서 STOMP 세션으로 브로드캐스트됨을 확인
- DB에 동일 메시지 ID `252693` 1건 저장 확인

[멀티 인스턴스 검증 상세 결과](docs%2Ftesting%2Fredis-pubsub-multi-instance-result.md)

### 읽음 처리

```text
채팅방 멤버 검증
→ 사용자 읽음 메타데이터 저장
→ 방 참여자 조회
→ 사용자별 lastReadMessageId 조회
→ 메시지별 unread count 계산
→ READ_UPDATED 이벤트 전파
→ 채팅방 목록 upsert 이벤트 전파
```

읽음 상태는 한 값으로 관리하지 않았습니다. 채팅방 목록에서 보여 주는 안 읽은 개수와 각 메시지에 표시하는 unread count의 계산 기준이 다르기 때문입니다.

### 재연결 이후 누락 메시지 복구

실시간 연결만으로는 네트워크가 끊긴 동안의 메시지를 보장할 수 없습니다. 클라이언트가 마지막으로 수신한 메시지 ID를 보관하고, 재연결 후 `afterMessageId`를 전달해 누락된 구간을 다시 조회하도록 구성했습니다.

---

## 5. Redis 메타데이터 Write-Back

메시지 한 건을 보낼 때마다 동일한 채팅방 row와 여러 사용자의 채팅 목록 row를 즉시 UPDATE하면 DB 경합이 커질 수 있습니다. 채팅 메시지 저장과 직접 관련이 없는 일부 메타데이터는 Redis에 먼저 반영한 뒤 주기적으로 DB에 동기화하도록 변경했습니다.

### Redis에 먼저 반영하는 값

#### 채팅방 메타데이터

- `lastMessageId`
- `lastMessagePreview`
- `lastMessageAt`

#### 사용자별 채팅방 메타데이터

- `unreadCount`
- `lastReadMessageId`
- `lastOpenedAt`

### 처리 방식

```text
메타데이터 Redis Hash 갱신
→ Dirty Set에 roomId 또는 roomId:userId 기록
→ Scheduler가 Dirty Set 조회
→ JdbcTemplate batchUpdate로 DB 반영
→ DB 반영에 성공한 Dirty Key만 제거
```

Redis 쓰기 자체가 실패하는 경우에는 Resilience4j Circuit Breaker fallback에서 DB에 직접 반영합니다. 반면 스케줄러의 DB batchUpdate가 실패한 경우에는 Dirty Key를 지우지 않고 다음 배치에서 다시 시도합니다. 두 실패 상황을 같은 방식으로 처리하지 않고 역할을 나눴습니다.

### 캐시 미스 시 unread count 보정

Redis에 사용자 메타데이터가 없는 상태에서 바로 `increment`하면 DB에는 5가 있는데 Redis는 1부터 시작하는 문제가 생길 수 있습니다.

```text
Redis key 없음
→ DB unreadCount 조회
→ Redis에 현재 값 저장
→ increment 수행
```

이 순서가 유지되는지 단위 테스트로 확인했습니다.

### 동일 채팅방 동시 요청 성능 검증

기존에는 메시지 요청 트랜잭션 안에서 `chat_room`과
사용자별 `chat_list` 메타데이터를 즉시 갱신했습니다.

동일 그룹 채팅방에 여러 사용자가 동시에 메시지를 보내자
MySQL 데드락과 트랜잭션 롤백, HikariCP 커넥션 풀 포화가
같은 실행에서 발생했습니다.

이후 후속 재현에서 `chat_room`과 `chat_list`를 갱신하는
트랜잭션 사이의 잠금 순서 충돌 구조를 확인했습니다.

이를 재현하기 위해 현재 스키마와 메시지 저장 방식은 유지하고,
메타데이터 처리 방식만 다음 두 모드로 비교했습니다.

| 모드 | 메타데이터 처리 |
| --- | --- |
| `sync-db` | 메시지 요청 트랜잭션 안에서 DB에 즉시 반영 |
| Redis Write-Back | Redis Hash와 Dirty Set에 반영한 뒤 Scheduler가 DB에 배치 반영 |

테스트 조건은 서로 다른 사용자 3명이 같은 그룹 채팅방에
각각 20건씩, 총 60건의 메시지를 100ms 간격으로 전송하는 방식입니다.

| 지표 | `sync-db` | Redis Write-Back |
| --- | ---: | ---: |
| 전송 메시지 | 60 | 60 |
| 자기 메시지 수신 | 56 | 60 |
| DB 저장 | 46 | 상세 검증 실행에서 60 |
| 저장 성공률 | 76.7% | 상세 검증 실행에서 100% |
| 모든 메시지를 수신한 VU | 0 / 3 | 3 / 3 |
| E2E p99 | 7,945.15ms | 1,482.31ms |
| Post-send tail 최대 | 6,459ms | 937ms |
| MySQL 데드락 | 발생 | 상세 검증 실행에서 증가량 0 |
| Dirty Set 최종 상태 | 해당 없음 | 상세 검증 실행에서 0 |

`sync-db` 지연시간은 문제 재현 실행 1회의 값이며,
Redis Write-Back 지연시간은 동일 조건으로 수행한 3회 실행의
중앙값을 사용했습니다. 세 실행 모두 k6 기준으로 60건 전체 수신을
확인했으며, 별도 DB·Redis 검증을 수행한 실행에서는 60건 전체 저장,
데드락 증가량 0, Dirty Set 최종 0을 확인했습니다.

`sync-db` 실행은 일부 요청이 실패한 상태이므로 이 결과를
단순히 Redis가 DB보다 몇 배 빠르다는 의미로 해석하지 않았습니다.
이번 검증의 핵심은 요청 경로에서 메타데이터 DB 쓰기를 분리한 뒤
동일 조건에서 메시지 처리 안정성과 tail latency가 개선됐다는 점입니다.

- [상세 성능 비교 문서](docs/performance/redis-write-back-comparison.md)
- [반복 실행 결과 CSV](docs/performance/redis-write-back-3runs.csv)
- [성능 비교 재현 브랜치](https://github.com/minsu11/live_chat/tree/perf/redis-write-back-comparison)


### 남은 검증 과제

- Redis AOF 복구 데이터와 DB 반영 순서의 정합성
- 동일 데이터가 여러 번 복구되는 상황의 멱등성
- Circuit Breaker open / half-open / close 상태의 운영 지표 수집

---

## 6. 테스트 전략

테스트 개수나 커버리지 수치만 높이기보다, 실패했을 때 데이터가 잘못 저장되거나 사용자에게 잘못 전파될 수 있는 흐름을 우선했습니다.

### 테스트 분류

| 분류 | 확인한 내용 |
| --- | --- |
| Controller | 인증 사용자와 path/query/body 값이 Service로 정확히 전달되는지 확인 |
| Service | 성공, 입력 검증, 조회 실패, 저장 실패, 경계값 확인 |
| Facade | 여러 Service·Redis·브로드캐스터의 호출 순서와 실패 이후 중단 여부 확인 |
| Redis / Scheduler | 캐시 미스, DB fallback, Dirty Key 유지, batchUpdate 변환 확인 |
| Security / STOMP | JWT 쿠키 누락·검증 실패, STOMP Bearer 형식, Principal 설정 확인 |
| File | MIME type, 확장자, 정확한 제한 크기와 1byte 초과, 디스크 저장 실패 확인 |
| WebSocket Broadcaster | 사용자별 destination 문자열과 멤버별 이벤트 전파 확인 |

### 대표 테스트 시나리오

| 영역 | 시나리오 | 확인한 내용 |
| --- | --- | --- |
| 메시지 전송 | DB 저장 실패 | 실패한 메시지가 WebSocket으로 전파되지 않음 |
| Redis 배치 | DB batchUpdate 실패 | Dirty Key를 유지해 다음 배치에서 재시도 |
| DM 생성 | 동일 사용자 조합 동시 생성 | Unique Key 충돌 후 기존 방 재조회 |
| 파일 업로드 | 20MB / 20MB + 1byte | 제한값은 허용하고 초과값은 거부 |
| 채팅 목록 | 다음 Cursor 생성 | Cursor를 디코딩해 마지막 시간과 roomId 확인 |
| 읽음 처리 | 중간 계산 실패 | `READ_UPDATED`와 채팅 목록 이벤트 전파 중단 |
| Redis unread | 캐시 미스 후 증가 | DB 값으로 캐시를 워밍한 뒤 증가 |
| HTTP 인증 | 쿠키 누락·잘못된 토큰 | SecurityContext 미설정과 오류 코드 기록 |
| STOMP 인증 | Authorization 형식·JWT 검증 | 유효한 CONNECT에서만 Principal 설정 |
| 프로필 수정 | 부분 수정과 이미지 저장 실패 | 전달된 필드만 수정하고 파일 실패 시 URL 갱신 중단 |
| 채팅방 나가기 | 목록·멤버 처리 실패 | 실패 지점 이후 참여자 수와 시스템 메시지 변경 중단 |

### JaCoCo 기준

GitHub Actions에서 테스트와 JaCoCo 검증을 함께 실행합니다.

```gradle
LINE >= 0.60
BRANCH >= 0.40
```

테스트 코드 정리 당시 GitHub Actions 검증 결과는 다음과 같습니다.

- 전체 테스트: **326개**
- 실패: **0개**
- Line Coverage: **64.36%** (`1,647 / 2,559`)
- Branch Coverage: **65.23%** (`424 / 650`)
- JaCoCo 품질 기준: **통과**

JaCoCo 측정에서는 DTO, 설정 바인딩, enum, 단순 예외, Querydsl 자동 생성 클래스와 애플리케이션 진입점을 제외했습니다. Service, Facade, Controller, Repository 구현체, Scheduler, Security Filter와 도메인 로직이 있는 Entity는 측정 대상에 남겼습니다.

### 테스트 실행

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification
```

### 리포트 위치:

```text
build/reports/tests/test/index.html
build/reports/jacoco/test/html/index.html
build/reports/jacoco/test/jacocoTestReport.xml
```

### Integration Test

- 실제 WebSocket/STOMP 연결 기반으로 CONNECT, 인증, SUBSCRIBE, SEND, MESSAGE 수신 및 사용자별 destination 라우팅 검증
- Testcontainers 기반 MySQL 8 환경에서 실제 DDL 적용 후 QueryDSL 쿼리 검증
- MySQL Full-Text Search BOOLEAN MODE, LIKE fallback, createdAt + messageId 복합 Cursor 동작 검증

### 현재 테스트의 한계

- Redis 장애 fallback은 단위 테스트 중심이며 Redis 프로세스를 실제로 내렸다 복구하는 자동화 테스트는 아직 없습니다.
- Testcontainers 기반 MySQL 테스트는 실제 DB 동작 검증에 초점을 두며 운영 환경 전체 구성을 재현하지는 않습니다.
- 회원가입부터 채팅방 생성, 메시지 전송, 재연결까지 이어지는 전체 E2E 테스트는 향후 과제입니다.
테스트 전략과 면접 대비용 상세 정리는 [`docs/testing/chatalk-test-study-notes.md`](docs/testing/chatalk-test-study-notes.md)에 별도로 작성했습니다.

---

## 7. Troubleshooting

### 1. 동일 채팅방 메타데이터 갱신 시 데드락과 커넥션 풀 포화

**문제**

메시지를 보낼 때마다 채팅방의 마지막 메시지 정보와
사용자별 unread count를 DB에 즉시 반영했습니다.

동일 그룹 채팅방에서 여러 사용자가 동시에 메시지를 보내자
MySQL 데드락과 트랜잭션 롤백이 발생했습니다.

후속 재현에서 `chat_room`과 `chat_list`를 갱신하는
트랜잭션 사이의 잠금 순서 충돌 구조를 확인했습니다.

락 대기와 처리되지 못한 트랜잭션이 누적되면서 HikariCP도
`active=10`, `idle=0`, `waiting=20~21` 상태까지 포화됐습니다.

**변경**

- 채팅방과 사용자별 메타데이터를 Redis Hash에 우선 반영
- Dirty Set으로 DB 반영 대상을 추적
- Scheduler에서 JDBC `batchUpdate` 수행
- DB 반영에 성공한 Dirty Key만 제거
- DB 반영 실패 시 Dirty Key를 유지해 다음 배치에서 재시도

**검증**

동일 조건에서 사용자 3명이 각각 20건씩 총 60건을 전송했습니다.

- `sync-db`: 56건 수신, 46건 DB 저장
- Redis Write-Back: 3회 모두 60건 전체 수신
- 상세 검증 실행: 60건 전체 DB 저장
- 상세 검증 실행: 데드락 증가량 0
- 상세 검증 실행: 사용자·채팅방 Dirty Set 최종 0
- E2E p99: 7,945.15ms (`sync-db` 재현 1회)
  → 1,482.31ms (Redis Write-Back 3회 중앙값)
- Post-send tail 최대: 6,459ms (`sync-db` 재현 1회)
  → 937ms (Redis Write-Back 3회 중앙값)

`sync-db` 지연시간은 문제 재현 실행 1회의 값이며,
Redis Write-Back 지연시간은 동일 조건으로 수행한 3회 실행의
중앙값을 사용했습니다.

[상세 성능 비교 결과](docs/performance/redis-write-back-comparison.md)

### 2. Redis 캐시 미스 이후 unread count가 1부터 시작하는 문제

**문제**

DB에는 기존 unread count가 있지만 Redis key가 사라진 상태에서 `increment`를 호출하면 Redis 값이 1부터 시작할 수 있습니다.

**변경**

Redis key가 없으면 DB 값을 먼저 읽어 캐시를 워밍한 뒤 증가하도록 처리했습니다.

### 3. WebSocket 재연결 동안의 메시지 누락

**문제**

WebSocket은 연결이 유지되는 동안의 실시간 전송에는 적합하지만 끊어진 시간의 메시지를 자동으로 복구하지 않습니다.

**변경**

- 마지막 수신 메시지 ID를 클라이언트가 보관
- 재연결 후 `afterMessageId` 기반 catch-up API 호출
- 실시간 수신 데이터와 catch-up 응답을 메시지 ID 기준으로 병합

### 4. 동일 DM 채팅방의 중복 생성 가능성

**문제**

두 요청이 동시에 기존 DM을 조회하면 모두 “채팅방 없음”으로 판단할 수 있습니다.

**변경**

- 두 사용자 ID로 순서와 관계없는 DM hash 생성
- DB Unique Constraint를 최종 방어선으로 사용
- 저장 충돌이 발생하면 기존 채팅방을 다시 조회해 반환

### 5. 첨부파일 선업로드 후 고아 파일 누적

**문제**

파일 업로드에는 성공했지만 메시지 전송이 취소되면 실제 파일과 메타데이터가 남습니다.

**변경**

메시지 연결 여부와 생성 시간을 기준으로 오래된 미연결 첨부파일을 정리하는 Scheduler를 추가했습니다.

### 6. Full-Text Search 검색 누락

**문제**

ngram parser를 적용해도 토큰 조건에 따라 일부 문자열이 Full-Text Search 결과에서 빠졌습니다.

**변경**

Full-Text Search를 기본 검색으로 사용하면서, 사용자가 입력한 문자열을 빠짐없이 찾기 위해 LIKE 조건을 fallback으로 추가했습니다.

---

## 8. WebSocket / STOMP 경로

`dev`와 `prod` 환경 모두 동일한 WebSocket/STOMP 경로 설정을 사용합니다.

| 구분 | 경로 |
| --- | --- |
| WebSocket Endpoint | `/api/ws-chat` |
| STOMP Publish Prefix | `/api/pub` |
| STOMP Subscribe Prefix | `/api/sub` |
| 메시지 발송 | `/api/pub/chat/message` |
| 읽음 이벤트 발송 | `/api/pub/chat/read` |
| 채팅방 메시지 구독 | `/user/api/sub/chat/rooms/{roomId}` |
| 읽음 이벤트 구독 | `/user/api/sub/chat/rooms/{roomId}/read` |

운영 환경에서는 Client가 `/api/ws-chat`으로 WebSocket/SockJS 연결을 요청하고,
Nginx가 해당 요청을 API Server로 프록시합니다.

WebSocket 연결 이후 사용하는 `/api/pub/**`, `/api/sub/**`는
HTTP API 경로가 아니라 STOMP frame의 destination입니다.

---

## 9. 주요 API

### 채팅방과 메시지

| Method | Endpoint | 설명 |
| --- | --- | --- |
| GET | `/api/v1/chat-room/{roomId}/enter` | 채팅방 진입 정보 조회 |
| GET | `/api/v1/chat-room/{roomId}/summary` | 채팅방 요약 조회 |
| GET | `/api/v1/chat-room/{userId}/register` | 1:1 채팅방 생성 또는 조회 |
| POST | `/api/v1/chat-room/group` | 그룹 채팅방 생성 |
| GET | `/api/v1/chat-room/{roomId}/messages` | 과거 메시지 커서 조회 |
| GET | `/api/v1/chat-room/{roomId}/messages/after` | 누락 메시지 catch-up 조회 |
| GET | `/api/v1/chat-room/{roomId}/messages/search` | 메시지 검색 |
| GET | `/api/v1/chat-room/{roomId}/messages/{messageId}/context` | 검색 결과 주변 메시지 조회 |

### 채팅방 설정

| Method | Endpoint | 설명 |
| --- | --- | --- |
| PATCH | `/api/v1/chat-room/{roomId}/settings/name` | 사용자별 채팅방 이름 변경 |
| PATCH | `/api/v1/chat-room/{roomId}/settings/notification` | 알림 음소거 변경 |
| DELETE | `/api/v1/chat-room/{roomId}/settings/leave` | 채팅방 나가기 |
| GET | `/api/v1/chat-room/{roomId}/settings/members` | 채팅방 멤버 조회 |
| POST | `/api/v1/chat-room/{roomId}/settings/invite` | 멤버 초대 |

### 프로필과 파일

| Method | Endpoint | 설명 |
| --- | --- | --- |
| GET | `/api/v1/users/me/profile/summary` | 내 프로필 요약 조회 |
| GET | `/api/v1/users/me/profile/detail` | 내 프로필 상세 조회 |
| GET | `/api/v1/users/{userId}/profile/detail` | 다른 사용자 프로필 조회 |
| POST | `/api/v1/users/me/profile/image` | 프로필 이미지 업로드 |
| POST | `/api/v1/users/me/profile` | 프로필 수정 |
| POST | `/api/v1/chat-attachments/upload` | 채팅 첨부파일 업로드 |
| GET | `/api/v1/chat-attachments/{attachmentId}/download` | 첨부파일 다운로드 |

---

## 10. 로컬 실행

### 필요 환경

- Java 21
- MySQL 8
- Redis
- Auth Server

### MySQL

```sql
CREATE DATABASE chat_server
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

### Redis

```bash
docker run -d \
  --name chat-redis \
  -p 6379:6379 \
  redis:7
```

### 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

Windows:

```bash
gradlew.bat bootRun --args="--spring.profiles.active=dev"
```

기본 API 주소는 `http://localhost:7070`입니다.

---

## 11. 패키지 구조

```text
com.chat_server
├── chatroom              # 채팅방
├── chatroommember        # 채팅방 멤버십
├── chatroomsetting       # 이름·알림·나가기·초대
├── chatmessage           # 메시지 저장·조회·검색·발송
├── chatread              # 읽음 처리
├── chatlist              # 채팅방 목록과 unread
├── chatattachment        # 첨부파일과 cleanup
├── chatnotification      # 알림 이벤트
├── friend                # 친구 관계
├── search                # 사용자 검색
├── user                  # 사용자
├── userprofile           # 프로필
├── security              # HTTP JWT 인증
├── websocket             # STOMP 설정과 인증
├── redis                 # 캐시·Pub/Sub·Write-Back
├── file                  # 파일 저장 공통 처리
└── error                 # 공통 예외 응답
```

---

## 12. 향후 개선

### 테스트

- [ ] Testcontainers MySQL 기반 Querydsl Repository 통합 테스트
- [ ] 실제 STOMP 연결·구독·발행 통합 테스트
- [ ] Redis 장애·복구 자동화 테스트
- [ ] 주요 사용자 흐름 E2E 테스트

### 안정성

- [ ] 메시지 전송 `clientMessageId`와 멱등성 처리
- [ ] Redis 복구 이후 DB 반영 순서 검증
- [ ] 멀티 디바이스 읽음 상태 동기화
- [ ] 메시지 ACK와 재전송 정책 정리

### 운영

- [ ] Micrometer / Prometheus / Grafana 연동
- [ ] 메시지 처리 지연, 오류율, Redis fallback, 배치 처리량 지표화
- [ ] 로그에 남아 있는 토큰 관련 정보 마스킹 범위 점검

---

## 13. 관련 문서

```text
docs/
├── performance/
│   ├── redis-write-back-comparison.md
│   └── redis-write-back-3runs.csv
├── portfolio/
│   ├── ParkMinsu_Chatalk_Summary_Portfolio.pdf
│   └── ParkMinsu_Chatalk_Detail_Portfolio.pdf
├── testing/
    ├── redis-pubsub-multi-instance-result.md
    └── redis-pubsub-multi-instance/
        ├── 01-redis-numsub.png
        ├── 02-sender-front.png
        ├── 03-sender-log.png
        ├── 04-receiver-log.png
        ├── 05-receiver-front.png
        └── 06-db-result.png
│   ├── test-strategy-and-coverage.md
│   └── chatalk-test-study-notes.md
├── Requirements.md
├── test-result.md
└── chat-list-testcase.md
```
