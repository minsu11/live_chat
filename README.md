# 💬 Live Chat API Server

Spring Boot 기반 실시간 채팅 **API 서버**입니다.

이 저장소는 실시간 채팅 서비스의 핵심 도메인인 **채팅방, 멤버십, 메시지, 읽음 상태, 첨부파일, 알림, 메시지 검색**을 담당합니다.  
Front/Auth/API 서버를 분리하여 인증과 화면, 채팅 도메인의 책임을 나누고, WebSocket(STOMP)을 통해 실시간 메시지 송수신을 처리합니다.

---

## 📌 프로젝트 개요

- 1:1 채팅에서 시작해 GROUP/OPEN 채팅방까지 확장 가능한 구조로 설계
- Front / Auth / API 서버를 분리하여 책임 분리
- WebSocket STOMP 기반 실시간 메시지 송수신
- 사용자별 읽음 상태와 채팅방 목록 unread 상태 분리 관리
- Redis 기반 채팅 메타데이터 Write-Back 구조 적용
- 파일/이미지 메시지와 orphan attachment cleanup 처리
- MySQL Full-Text Search + LIKE fallback 기반 채팅 메시지 검색 구현

---

## 🧱 전체 구성

| 서버 | 역할 | Repository |
|---|---|---|
| Front Server | Vue 기반 채팅 UI, WebSocket 연결, 메시지 렌더링 | https://github.com/minsu11/live_chat_front |
| Auth Server | 로그인, 토큰 발급/재발급, 인증 처리 | https://github.com/minsu11/live_chat_auth |
| API Server | 채팅 도메인, WebSocket 처리, Redis/DB 동기화 | 현재 저장소 |

---

## 🛠 기술 스택

### Backend

- Java 21
- Spring Boot 3.4.3
- Spring Security
- Spring WebSocket / STOMP
- Spring Data JPA
- Spring Data Redis
- Spring Cloud OpenFeign
- Querydsl
- Resilience4j
- MySQL
- Redis
- Docker

### 주요 인프라/운영 요소

- JWT 기반 HTTP 인증
- STOMP CONNECT 시 JWT 검증
- Redis 메타데이터 캐싱 및 Write-Back
- 파일 업로드 디렉토리 분리
- 로그 파일 rolling 설정
- dev/prod profile 분리

---

## ✨ 주요 기능

### 1. 채팅방 / 멤버십

- 1:1 채팅방 생성 및 기존 방 조회
- 그룹 채팅방 생성
- 채팅방 멤버 조회
- 멤버 초대
- 채팅방 나가기
- 비멤버 접근 차단
- DM / GROUP / OPEN 타입 확장을 고려한 채팅방 모델 구성

---

### 2. 메시지 / 실시간 전송

- `@MessageMapping` 기반 WebSocket 메시지 송신
- TEXT / EMOJI / IMAGE / FILE / SYSTEM 메시지 처리
- 메시지 저장 후 수신자별 실시간 브로드캐스트
- 채팅방 목록 갱신 이벤트 전파
- 메시지 알림 이벤트 전파
- 중복 메시지 수신 방지를 위한 메시지 ID 기준 병합 처리

---

### 3. 메시지 조회 / 복구

- 채팅방 진입 시 최신 메시지 조회
- 커서 기반 과거 메시지 조회
- 스크롤 상단 도달 시 과거 메시지 추가 로딩
- `afterMessageId` 기반 누락 메시지 복구 API
- WebSocket 재연결 후 catch-up 처리
- 일반 페이지네이션 API와 재연결 복구 API 목적 분리

---

### 4. 읽음 / 안읽음 정합성

- 사용자별 `lastReadMessageId` 관리
- 채팅방 목록 unread count와 메시지별 unread count 분리
- 채팅방 진입 시 read 메타데이터 반영
- 메시지 수신 후 read 이벤트 전송
- `READ_UPDATED` 이벤트로 메시지 unread count 실시간 갱신
- 신규 멤버 초대 시점의 read 기준 초기화

---

### 5. 채팅방 표시 이름 / 설정

- 조회자 기준 채팅방 표시 이름 계산
  - 커스텀 방 이름 우선
  - DM: 상대 사용자 표시 이름 fallback
  - GROUP: 방 이름 또는 멤버 이름 조합 fallback
- 채팅방 이름 변경
- 채팅방 알림 on/off 설정
- 채팅방 나가기
- 채팅방 멤버 조회
- 멤버 초대

---

### 6. 첨부파일 처리

- 이미지 메시지 업로드
- 일반 파일 메시지 업로드
- 첨부파일 선업로드 후 메시지와 연결
- 파일 다운로드 API 제공
- 파일 크기 및 확장자 검증
- 메시지 미전송으로 연결되지 않은 orphan attachment 정리 스케줄러 운영

---

### 7. 메시지 검색

- 채팅방 단위 키워드 검색
- MySQL Full-Text Search + ngram parser 적용
- Full-Text Search 누락 보완을 위한 LIKE fallback 적용
- 검색 결과 cursor pagination
- 특정 검색 결과 기준 앞뒤 메시지 context 조회
- 검색 결과 간 이동을 위한 messageId 기반 jump 처리
- 프론트 검색어 하이라이트 및 스크롤 위치 보정 지원

#### 검색 API 역할 분리

| API | 역할 |
|---|---|
| `GET /api/v1/chat-room/{roomId}/messages/search` | 키워드 기반 검색 결과 조회 |
| `GET /api/v1/chat-room/{roomId}/messages/{messageId}/context` | 특정 메시지 기준 앞뒤 대화 조회 |

---

### 8. 친구 / 유저 검색 / 프로필

- 친구 목록 cursor 조회
- 친구 등록
- 유저 ID 기반 검색
- 내 프로필 요약 조회
- 내 프로필 상세 조회
- 타인 프로필 조회
- 프로필 이미지 업로드
- 프로필 수정

---

### 9. 인증 / 보안

- JWT 기반 HTTP 인증
- Auth 서버와 연동한 로그인 처리
- WebSocket 연결용 토큰 발급 API
- STOMP CONNECT 시 JWT 검증
- 인증 유저 Principal 설정
- `/user` destination 기반 사용자별 WebSocket 메시지 전송
- Refresh Token Redis 저장

---

## ⚡ Redis Write-Back 메타데이터 동기화

채팅 메시지 발송 핫패스에서 DB UPDATE 경합을 줄이기 위해 일부 메타데이터는 Redis에 먼저 반영하고, 주기적으로 DB에 동기화합니다.

### 처리 대상

#### 채팅방 메타데이터

- `lastMessageId`
- `lastMessagePreview`
- `lastMessageAt`

#### 사용자별 채팅방 메타데이터

- `unreadCount`
- `lastReadMessageId`
- `lastOpenedAt`

### 처리 방식

1. 메시지 송수신 시 Redis Hash에 메타데이터 우선 반영
2. 변경된 채팅방/사용자를 Dirty Set에 기록
3. 스케줄러가 주기적으로 Redis 변경분을 DB에 Bulk Update로 반영
4. 반영 완료 후 Dirty Set 정리
5. Redis 배치 동기화 실패 시 Circuit Breaker를 통해 DB 직접 반영 경로로 fallback
6. Redis 장애/재시작 상황에서 미반영 메타데이터 유실을 줄이기 위해 AOF 설정 적용

### 적용 의도

- 동일 채팅방 메타데이터에 대한 DB 동시 UPDATE 경합 완화
- 메시지 발송 핫패스의 DB 부하 감소
- Redis 장애 시에도 미반영 메타데이터가 복구 후 DB에 동기화될 수 있도록 보완
- 배치 동기화 실패 시 DB 직접 반영 fallback으로 서비스 안정성 보강

### 검증한 내용

- Redis에 Dirty 데이터가 남아 있는 상태에서 장애/재시작 상황 발생
- Redis 복구 후 AOF에 남아 있던 데이터가 다시 로드되는지 확인
- 복구 이후 스케줄러가 미반영 메타데이터를 DB에 추가 반영하는지 확인

### 남은 검증 과제

- Redis 복구 후 DB 반영 시점의 메시지 순서 보장 검증
- Redis AOF 복구 데이터와 DB PK 증가 순서 간 정합성 검증
- 장애 중복 복구 상황에서 idempotency 보장 검증

---

## 🔌 WebSocket 구조

### 개발 환경 기준

| 구분 | 경로 |
|---|---|
| WebSocket Endpoint | `/api/ws-chat` |
| Publish Prefix | `/api/pub` |
| Subscribe Prefix | `/api/sub` |
| 메시지 발송 | `/api/pub/chat/message` |
| 읽음 이벤트 발송 | `/api/pub/chat/read` |
| 채팅방 메시지 구독 | `/user/api/sub/chat/rooms/{roomId}` |
| 읽음 갱신 구독 | `/user/api/sub/chat/rooms/{roomId}/read` |

### 운영 환경 기준

운영 profile에서는 WebSocket prefix가 다음과 같이 단순화됩니다.

| 구분 | 경로 |
|---|---|
| WebSocket Endpoint | `/ws-chat` |
| Publish Prefix | `/pub` |
| Subscribe Prefix | `/sub` |

---

## 📡 주요 API

### 인증 / 유저

| Method | Endpoint | 설명 |
|---|---|---|
| POST | `/api/v1/users/login` | 로그인 |
| POST | `/api/v1/users/register` | 회원가입 |
| GET | `/api/v1/users/me/profile/summary` | 내 프로필 요약 조회 |
| GET | `/api/v1/users/me/profile/detail` | 내 프로필 상세 조회 |
| GET | `/api/v1/users/{userId}/profile/detail` | 타인 프로필 조회 |
| POST | `/api/v1/users/me/profile/image` | 프로필 이미지 업로드 |
| POST | `/api/v1/users/me/profile` | 프로필 수정 |

### 친구 / 검색

| Method | Endpoint | 설명 |
|---|---|---|
| GET | `/api/v1/friends` | 친구 목록 조회 |
| POST | `/api/v1/friends/register` | 친구 등록 |
| POST | `/api/v1/search/users` | 유저 검색 |

### 채팅방

| Method | Endpoint | 설명 |
|---|---|---|
| GET | `/api/v1/chat-room/{roomId}/enter` | 채팅방 진입 정보 조회 |
| GET | `/api/v1/chat-room/{roomId}/summary` | 채팅방 요약 조회 |
| GET | `/api/v1/chat-room/{userId}/register` | 1:1 채팅방 생성 또는 조회 |
| POST | `/api/v1/chat-room/group` | 그룹 채팅방 생성 |
| GET | `/api/v1/chat-room/{roomId}/messages` | 과거 메시지 cursor 조회 |
| GET | `/api/v1/chat-room/{roomId}/messages/after` | 누락 메시지 catch-up 조회 |
| GET | `/api/v1/chat-room/{roomId}/messages/search` | 채팅방 메시지 검색 |
| GET | `/api/v1/chat-room/{roomId}/messages/{messageId}/context` | 특정 메시지 기준 context 조회 |

### 채팅방 설정

| Method | Endpoint | 설명 |
|---|---|---|
| PATCH | `/api/v1/chat-room/{roomId}/settings/name` | 채팅방 이름 변경 |
| PATCH | `/api/v1/chat-room/{roomId}/settings/notification` | 채팅방 알림 설정 변경 |
| DELETE | `/api/v1/chat-room/{roomId}/settings/leave` | 채팅방 나가기 |
| GET | `/api/v1/chat-room/{roomId}/settings/members` | 채팅방 멤버 조회 |
| POST | `/api/v1/chat-room/{roomId}/settings/invite` | 채팅방 멤버 초대 |

### 첨부파일

| Method | Endpoint | 설명 |
|---|---|---|
| POST | `/api/v1/chat-attachments/upload` | 채팅 첨부파일 업로드 |
| GET | `/api/v1/chat-attachments/{attachmentId}/download` | 첨부파일 다운로드 |
| POST | `/api/v1/chat-files/images` | 채팅 이미지 업로드 |
| POST | `/api/v1/chat-files/files` | 채팅 파일 업로드 |

### 채팅 목록

| Method | Endpoint | 설명 |
|---|---|---|
| GET | `/api/v1/chat-list` | 채팅방 목록 cursor 조회 |

### WebSocket Token

| Method | Endpoint | 설명 |
|---|---|---|
| GET | `/api/ws/token` | WebSocket 연결용 토큰 발급 |

---

## 📁 패키지 구조

```text
com.chat_server
├── chatroom              # 채팅방 도메인
├── chatroommember        # 채팅방 멤버십
├── chatroomsetting       # 채팅방 이름/알림/나가기/초대 설정
├── chatmessage           # 채팅 메시지 저장/조회/검색/WebSocket 발송
├── chatread              # 읽음 처리
├── chatlist              # 채팅방 목록 조회
├── chatattachment        # 첨부파일 업로드/다운로드/cleanup
├── chatnotification      # 채팅 알림 이벤트
├── friend                # 친구 관계
├── search                # 유저 검색
├── user                  # 유저 도메인
├── userprofile           # 프로필
├── security              # HTTP 인증/JWT/WebSocket token
├── websocket             # STOMP 설정/인터셉터
├── redis                 # Redis 메타데이터/배치 동기화
├── file                  # 파일 업로드 공통 처리
├── common                # 공통 DTO, cursor, config, exception
└── error                 # 공통 에러 응답/핸들러
```

---

## 🚀 로컬 실행 방법

### 1. 필수 환경

- Java 21
- MySQL 8.x
- Redis
- Gradle Wrapper
- Auth Server 실행 필요
- Front Server 실행 선택

---

### 2. MySQL 준비

`application-dev.yml` 기준으로 API 서버는 MySQL을 사용합니다.

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/chat_server?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
```

로컬 DB 예시:

```sql
CREATE DATABASE chat_server
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

> 실제 테이블은 프로젝트 DDL 또는 JPA validate 기준 스키마에 맞춰 준비해야 합니다.

---

### 3. Redis 실행

로컬 개발 profile 기준 Redis는 다음 설정을 사용합니다.

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
```

Docker로 Redis를 실행하는 예시:

```bash
docker run -d \
  --name chat-redis \
  -p 6379:6379 \
  redis:7
```

---

### 4. 파일 업로드 경로 설정

개발 환경에서는 업로드 파일을 로컬 프로젝트 내부 디렉토리에 저장합니다.

```yaml
file:
  upload:
    profile-dir: C:\chat-server-uploads\image\profile
    chat-image-dir: C:\chat-server-uploads\image\chat
    chat-file-dir: C:\chat-server-uploads\image\file
    chat-file-max-size: 5MB
    chat-file-allowed-extensions:
      - pdf
      - txt
      - doc
      - docx
      - xls
      - xlsx
      - ppt
      - pptx
      - zip
      - hwp
      - hwpx
      - jpg
      - jpeg
      - png
```

업로드 경로는 용도별로 분리되어 있습니다.

| 설정 | 용도 |
|---|---|
| `profile-dir` | 사용자 프로필 이미지 저장 |
| `chat-image-dir` | 채팅 이미지 메시지 저장 |
| `chat-file-dir` | 채팅 일반 파일 저장 |
| `chat-file-max-size` | 채팅 파일 최대 업로드 크기 |
| `chat-file-allowed-extensions` | 업로드 허용 확장자 목록 |

```text
개발 환경: C:\project_file\live_chat\src\main\resources\image\...
운영/Docker 환경: 서버 경로 또는 volume mount 경로로 분리 필요
```

---

### 5. Auth Server 실행

API 서버는 로그인 및 토큰 검증을 위해 Auth Server와 연동합니다.

개발 profile 기준:

```yaml
auth:
  server:
    url: http://localhost:9090
```

Auth Server를 먼저 실행한 뒤 API 서버를 실행해야 합니다.

---

### 6. API 서버 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

Windows 환경:

```bash
gradlew.bat bootRun --args="--spring.profiles.active=dev"
```

기본 포트:

```text
http://localhost:7070
```

---

## 🐳 Docker 실행

Dockerfile은 빌드된 jar 파일을 실행하는 구조입니다.

```dockerfile
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
```

### 1. jar 빌드

```bash
./gradlew clean build
```

### 2. Docker image build

```bash
docker build -t live-chat-api .
```

### 3. Docker container run 예시

```bash
docker run -d \
  --name live-chat-api \
  -p 7070:7070 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -v /host/uploads:/app/uploads \
  live-chat-api
```

---

## ⚙️ 주요 설정

### Profile group

```yaml
spring:
  profiles:
    group:
      dev:
        - custom
        - web-dev
      prod:
        - custom
        - web-prod
```

### 파일 업로드 제한

개발 profile 기준:

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

file:
  upload:
    chat-file-max-size: 5MB
    chat-file-allowed-extensions:
      - pdf
      - txt
      - doc
      - docx
      - xls
      - xlsx
      - ppt
      - pptx
      - zip
      - hwp
      - hwpx
      - jpg
      - jpeg
      - png
```

운영 profile에서는 파일 업로드 크기를 더 제한할 수 있습니다.

```yaml
file:
  upload:
    chat-file-max-size: 1MB
```

---

## 🔍 메시지 검색 설계

메시지 검색은 다음 흐름으로 동작합니다.

1. 검색어 입력
2. 채팅방 단위 메시지 검색
3. 검색 결과 최신순 반환
4. 검색 결과가 많으면 cursor로 다음 페이지 조회
5. 검색 결과 선택 시 해당 메시지 기준 앞뒤 메시지 context 조회
6. 프론트에서 검색어 하이라이트 및 스크롤 위치 보정

### 검색 방식

- 기본 검색: MySQL Full-Text Search
- 한글 검색 대응: ngram parser
- 검색 누락 보완: LIKE fallback

```sql
WHERE chat_room_id = :roomId
  AND message_content IS NOT NULL
  AND (
        MATCH(message_content) AGAINST(:booleanKeyword IN BOOLEAN MODE)
        OR message_content LIKE CONCAT('%', :rawKeyword, '%')
  )
```

### 적용 이유

Full-Text Search는 성능상 유리하지만, parser/token 설정에 따라 일부 키워드가 누락될 수 있습니다.  
채팅 검색에서는 “사용자가 입력한 문자열이 포함된 메시지를 빠짐없이 찾는 것”이 중요하므로, Full-Text Search를 기본으로 사용하되 LIKE fallback을 함께 적용했습니다.

---

## 🔥 Trouble Shooting 요약

### 1. DB 메타데이터 경합과 Redis Write-Back

- 문제: 메시지 발송 시 동일 채팅방/사용자 메타데이터를 DB에서 즉시 갱신하면서 lock 경합과 데드락 가능성이 발생
- 해결:
  - 채팅방/사용자 메타데이터를 Redis에 우선 반영
  - Dirty Set으로 변경 대상을 추적
  - 스케줄러에서 Redis 변경분을 DB에 Bulk Update로 반영
- 결과:
  - 메시지 발송 핫패스의 DB UPDATE 경합 완화
  - 배치 동기화 방식으로 DB 반영 비용 분산

---

### 2. Redis 장애와 미반영 메타데이터 유실 위험

- 문제: Redis에만 반영되고 DB에 아직 동기화되지 않은 Dirty 데이터가 Redis 장애 시 유실될 수 있음
- 해결:
  - Redis AOF 설정을 적용하여 장애/재시작 후 미반영 데이터 복구 가능성 확보
  - 배치 동기화 실패 시 Circuit Breaker를 통해 DB 직접 반영 경로로 fallback
- 결과:
  - Redis 장애 복구 후 AOF에 남아 있던 Dirty 데이터가 DB에 추가 동기화되는 것을 확인
  - 단, DB PK 순서와 메시지 순서 보장은 추가 검증 과제로 분리

---


### 3. unread count 정합성

- 문제: 채팅방 목록 unread와 메시지별 unread를 하나의 값처럼 다루면 재접속/재조회 시 불일치 발생
- 해결: `lastReadMessageId`와 `unreadCount`를 분리하고 read 이벤트 기준으로 명시적 갱신
- 결과: 목록 unread와 메시지 unread의 역할 분리

---

### 4. WebSocket 재연결 후 누락 메시지 복구

- 문제: WebSocket 연결이 끊긴 동안 발생한 메시지를 클라이언트가 놓칠 수 있음
- 해결: 클라이언트가 마지막으로 받은 `lastReceivedMessageId` 이후 메시지를 catch-up API로 조회
- 결과: 재연결 후 누락 구간 복구 가능

---

### 5. orphan attachment 누적

- 문제: 첨부파일 선업로드 후 메시지 전송이 완료되지 않으면 고아 파일/메타데이터가 누적됨
- 해결: 메시지 연결 전/후 상태를 분리하고 cleanup scheduler로 미연결 첨부 정리
- 결과: 파일 스토리지와 DB 메타데이터 누수 리스크 감소

---

### 6. 메시지 검색 누락

- 문제: MySQL Full-Text Search + ngram parser 적용 후에도 일부 영문 키워드가 검색되지 않는 케이스 발생
- 해결: Full-Text Search를 유지하되 LIKE fallback 추가
- 결과: 검색 정확도 보완 및 카카오톡식 검색 UX 구현

---

## 🎯 향후 개선 과제

### 단기

- [ ] Redis Write-Back 장애 시나리오 검증 강화
  - Redis 복구 후 DB 반영 순서 검증
  - DB PK 증가 순서와 메시지 시간 순서 정합성 검증
  - 중복 동기화 방지를 위한 idempotency 검증
  - Circuit Breaker open/half-open/close 전환 로그 정리
- [ ] 메시지 검색 필터 고도화
  - 기간 필터
  - 발신자 필터
  - 파일명 검색
- [ ] 메시지 삭제 기능
  - 소프트 삭제
  - 실시간 삭제 이벤트
  - 검색 결과 제외 처리

### 중기

- [ ] 관리자 기능
  - 유저 제재
  - 신고 처리
  - 채팅방 강제 퇴장
  - 운영 감사 로그 조회
- [ ] 관측성 강화
  - Micrometer
  - Prometheus
  - Grafana
  - TPS / 지연시간 / 에러율 / 배치 처리량 시각화
- [ ] 메시지 검색 고도화
  - OpenSearch / Elasticsearch 검토
  - 형태소 분석
  - 오타 허용
  - 랭킹 기반 검색

### 확장

- [ ] 모바일 환경 최적화
  - 백그라운드 복귀 후 catch-up
  - read ACK 튜닝
  - 푸시 알림 토큰 연동
- [ ] 멀티 디바이스 동기화
  - 동일 계정 다중 세션 읽음 상태 동기화
  - 디바이스별 알림 정책
- [ ] 메시지 전달 보장 강화
  - client message id
  - idempotency key
  - ACK / 재전송 정책

---

## 🔗 연관 저장소

- Front Server: https://github.com/minsu11/live_chat_front
- Auth Server: https://github.com/minsu11/live_chat_auth
- API Server: https://github.com/minsu11/live_chat

---

## 📚 참고 문서

현재 저장소의 `docs` 디렉토리에 요구사항 및 테스트 관련 문서를 함께 관리합니다.

```text
docs/
├── Requirements.md
├── task-list.md
├── test-result.md
└── chat-list-testcase.md
```

---

## 🧪 테스트 및 품질 리포트

### 테스트 실행

```bash
./gradlew test
```

JaCoCo 커버리지 리포트까지 생성하려면 다음 명령을 사용합니다.

```bash
./gradlew test jacocoTestReport
```

### JaCoCo 리포트 위치

- HTML: `build/reports/jacoco/test/html/index.html`
- XML: `build/reports/jacoco/test/jacocoTestReport.xml`

### SonarQube 분석 실행

이 프로젝트는 SonarQube 분석 기반을 구성해 두었습니다. Java 커버리지는 SonarQube가 JaCoCo XML 리포트를 읽는 방식으로 연동합니다.

Local SonarQube를 사용할 수도 있고, 팀 공용 SonarQube 또는 SonarCloud를 사용할 수도 있습니다. 기본 host는 `http://localhost:9000`이며, CI/팀 서버에서는 환경변수로 변경합니다.

```bash
SONAR_HOST_URL=http://localhost:9000 \
SONAR_TOKEN=<your-token> \
./gradlew test jacocoTestReport sonar
```

### 주요 테스트 코드

JUnit 5와 Mockito 기반 단위 테스트를 추가했고, 핵심 Facade/Service/Controller 흐름을 중심으로 검증합니다.

| 영역 | 테스트 파일 | 주요 검증 |
|---|---|---|
| 메시지 전송 Facade | `src/test/java/com/chat_server/chatmessage/service/impl/ChatMessageFacadeServiceImplTest.java` | 메시지 저장, Redis 메타 갱신, WebSocket 브로드캐스트, 채팅 목록 upsert, 알림 전파, 파일 첨부 연결, 차단 사용자 제외 |
| 채팅방 진입/복구 Facade | `src/test/java/com/chat_server/chatroom/service/impl/ChatRoomFacadeServiceImplTest.java` | 채팅방 진입 읽음 처리, cursor 처리, unread count 계산, `afterMessageId` 기반 누락 메시지 복구 |
| 메시지 도메인 Service | `src/test/java/com/chat_server/chatmessage/service/impl/ChatMessageServiceImplTest.java` | 메시지 생성, 발신자 검증, 메시지별 unread count 계산, context 메시지 조합 |
| 읽음 처리 | `src/test/java/com/chat_server/chatread/service/impl/ChatReadFacadeServiceImplTest.java`, `src/test/java/com/chat_server/chatread/service/impl/ChatReadServiceImplTest.java`, `src/test/java/com/chat_server/chatlist/service/impl/ChatListServiceImplTest.java`, `src/test/java/com/chat_server/redis/service/ChatMetadataRedisServiceTest.java` | lastReadMessageId 갱신, 채팅방 목록 unreadCount 초기화, READ_UPDATED 이벤트 전파, Redis fallback 흐름 |
| 첨부파일 cleanup | `src/test/java/com/chat_server/chatattachment/service/impl/ChatAttachmentCleanupServiceImplTest.java`, `src/test/java/com/chat_server/chatattachment/scheduler/ChatAttachmentCleanupSchedulerTest.java` | orphan attachment 조회, 실제 파일 삭제, metadata 삭제, 삭제 실패 시 흐름 유지 |
| 메시지 검색 | `src/test/java/com/chat_server/chatmessage/service/impl/ChatMessageSearchServiceImplTest.java`, `src/test/java/com/chat_server/chatmessage/service/impl/ChatMessageSearchFacadeServiceImplTest.java` | 검색어 정규화, 채팅방 단위 검색 위임, 빈 검색어 처리, cursor 기반 nextCursor 생성 |
| Redis Repository | `src/test/java/com/chat_server/redis/repository/impl/RedisRepositoryImplTest.java` | Redis key/value/ttl validation, save/find/delete/list operation 위임 |
| 친구/유저 Service | `src/test/java/com/chat_server/friend/service/impl/FriendServiceImplTest.java`, `src/test/java/com/chat_server/user/service/impl/UserServiceImplTest.java` | 친구 등록 성공/실패, 회원가입 성공/실패, 사용자 조회 실패, 아이디 중복 검증 |

### 현재 테스트의 한계와 향후 보강 과제

- 실제 DB 기반 Querydsl Repository 통합 테스트는 아직 제한적입니다. MySQL Full-Text Search는 H2와 동작 차이가 있어 `@DataJpaTest` 또는 Testcontainers 기반 테스트로 보강하는 것이 적절합니다.
- 실제 WebSocket/STOMP 연결 테스트는 아직 완료된 상태가 아닙니다. 현재는 Controller/Facade 단위에서 메시지 전송 위임과 브로드캐스트 호출 여부를 검증합니다.
- 실제 Redis 장애 복구를 완전히 검증한 것은 아닙니다. 현재는 Redis fallback 메서드가 의도한 repository 동기화 호출을 수행하는지 단위 테스트로 확인합니다.
- 전체 사용자 E2E 시나리오 테스트는 향후 보강 과제로 남겨 두었습니다.
