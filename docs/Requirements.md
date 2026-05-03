# chat server 요구사항 정의서

## 📌 개요

### 목적

사용자가 **실시간으로 채팅**하고, 메시지를 **안정적이고 보안성 있게 처리**할 수 있는 시스템을 구축한다.  
구조는 **Frontend + API Server + Auth Server** 로 분리하며, **채팅 기능은 API Server에서 통합 처리**한다.

현재 목표는 단순 메시지 송수신을 넘어서, 아래 핵심 흐름을 **실사용 가능한 수준으로 안정화**하는 것이다.

- 1:1 채팅
- 그룹 채팅
- 읽음 처리 / unread count
- 재연결 및 누락 메시지 복구
- 이미지 / 파일 메시지 전송
- 채팅방 개인 설정

---

## 0. 현재 시스템 구조

### 서버 구조

- **Frontend**: Vue.js
- **API Server**: Spring Boot
- **Auth Server**: 별도 분리
- **실시간 통신**: WebSocket + STOMP + SockJS
- **DB**: MySQL
- **캐시 / 보조 저장소**: Redis
- **인증**: JWT 기반

### 인증 구조

- 로그인은 Auth Server에서 수행
- API Server는 Auth Server와 연동하여 인증 결과를 처리
- WebSocket 연결 전 `/ws/token` 으로 WebSocket 전용 토큰을 발급받아 사용
- STOMP CONNECT 시 `Authorization: Bearer <token>` 헤더로 인증
- API Server의 HTTP 필터 및 STOMP interceptor 에서 JWT 검증 수행

---

## 1. 기능 요구사항

### 1-1. 기본 채팅 기능

- 1:1 채팅 지원
- 그룹 채팅 지원
- WebSocket 기반 메시지 실시간 송수신
- 클라이언트는 JWT 인증 후 WebSocket 연결
- 메시지 보낸 시간 표시
- 읽음 처리 지원
- 채팅방 목록은 **최근 메시지 기준 정렬**
- 채팅방 진입 시 메시지 목록 조회 지원
- 과거 메시지 **커서 기반 페이지네이션** 지원

#### 현재 구현 범위

- 1:1 채팅
- 그룹 채팅
- 실시간 메시지 송수신
- unread count
- 읽음 처리
- `READ_UPDATED` 이벤트 기반 unread 갱신
- 채팅방 진입 시 메시지 조회
- 메시지 커서 기반 조회

---

### 1-2. 메시지 타입

#### 현재 지원 메시지 타입

- TEXT
- EMOJI
- IMAGE
- FILE

#### 확장 고려 메시지 타입

- SYSTEM
- THREAD
- TEMP(자동 삭제 메시지)

---

### 1-3. 메시지 조회 및 복구

#### 과거 메시지 조회

- 채팅방 진입 및 과거 내역 더보기를 위해 **cursor 기반 조회** 사용
- 정렬 기준은 DB 조회 시 DESC, 프론트 표시 시 ASC 정렬

#### reconnect 후 누락 메시지 복구

- WebSocket 재연결 후, 끊긴 동안 놓친 메시지를 복구해야 함
- 복구는 cursor 재사용이 아니라 **`afterMessageId` 기반 전용 API** 사용
- 예시:
    - `GET /v1/chat-room/{roomId}/messages/after?afterMessageId=...&limit=100`
- 목적:
    - 마지막으로 받은 messageId 이후의 메시지만 복구
- 정렬:
    - `createdAt ASC`, `messageId ASC`

#### 설계 원칙

- `cursor` = 과거 페이지네이션
- `afterMessageId` = reconnect 복구
- 두 용도는 분리 유지

---

### 1-4. 읽음 / unread 처리

#### 요구사항

- 채팅방 목록 unread count 제공
- 메시지별 unread count 제공
- 읽음 처리 시 관련 값 갱신
- 현재 열려 있는 방은 unread 가 즉시 정리되어야 함
- reconnect 후 복구된 메시지에 대해서도 read 정합성이 유지되어야 함

#### 개념 분리

- **채팅방 unread count**: 내가 아직 읽지 않은 메시지 수
- **메시지 unread count**: 이 메시지를 아직 읽지 않은 사람 수

#### 현재 구현 방향

- 읽음 처리 후 `READ_UPDATED` 이벤트 발행
- 활성 방 / hidden 탭 / reconnect 이후 read 재전송까지 고려
- hidden 상태에서는 즉시 read 전송하지 않고 visible 복귀 시 처리 가능해야 함

---

## 2. 재연결 및 복구 요구사항

### 2-1. WebSocket 재연결

- WebSocket 연결이 끊겼을 때 자동 재연결 지원
- 단순 reconnectDelay 만으로 끝내지 않고, **구독 복구**까지 수행해야 함
- 기존 구독 destination / callback 정보를 저장하고, reconnect 후 자동 재구독 수행

#### 현재 구현 방향

- `subscriptionRegistry` 로 구독 메타데이터 저장
- reconnect 후 `resubscribeAll()` 수행
- reconnect listener 지원
- disconnect listener 지원
- 정상 종료와 비정상 종료를 구분

---

### 2-2. 누락 메시지 catch-up

- reconnect 후 끊긴 동안 놓친 메시지를 자동 복구
- `lastReceivedMessageId` 기준 복구
- 메시지 한 건뿐 아니라 다건 복구 지원
- 필요 시 hasMore 기반 반복 복구 가능해야 함
- 빈 방에서 첫 메시지를 놓친 경우도 복구 가능해야 함 (`afterMessageId == null` 대응)

#### 현재 중요 포인트

- `lastReceivedMessageId ?? 0` 처리 필요
- catch-up 후 read 재전송 필요
- 중복 메시지 append 방지 필요
- FILE / IMAGE 타입도 동일하게 복구 가능해야 함

---

## 3. 파일 / 이미지 메시지 기능

### 3-1. 업로드 및 다운로드

- 이미지 업로드 지원
- 파일 업로드 지원
- 업로드 후 IMAGE / FILE 메시지로 전송 가능해야 함
- FILE 메시지는 다운로드 가능해야 함
- 업로드 실패 시 사용자에게 적절한 에러 메시지 제공

### 3-2. 첨부파일 관리

- 채팅 메시지용 파일은 attachment 단위로 관리
- attachment 와 message 를 연결할 수 있어야 함
- room membership / uploader 검증이 필요함
- 메시지에 연결되지 않은 attachment 는 orphan 으로 간주

### 3-3. orphan cleanup

- 업로드는 되었지만 메시지 전송이 되지 않은 파일 정리 필요
- `chat_message_id is null` 인 attachment 를 일정 시간 후 정리
- 파일 + DB 레코드 함께 삭제

### 3-4. 파일 검증

#### 프론트 검증

- 빈 파일 검사
- 크기 검사
- 확장자 검사

#### 백엔드 검증

- 최대 크기
- 허용 확장자
- 경로 검증
- 채팅방 멤버 여부 검증

---

## 4. 채팅방 관리 기능

### 4-1. 현재 범위

- 채팅방 생성 (1:1 / 그룹)
- 채팅방 목록 조회
- 채팅방 진입
- 채팅방 나가기
- 채팅방 개인 설정

### 4-2. 채팅방 설정 (현재 단계)

현재 단계의 채팅방 설정은 **공용 설정이 아니라 개인 설정**으로 본다.

#### 현재 필요 기능

- 내가 보는 채팅방 이름 변경
- 알림 끄기 / 켜기
- 채팅방 나가기

#### 설계 원칙

- 현재 일반채팅 / 그룹채팅에서는 **모두에게 보이는 방 설정은 필요 없음**
- 그룹방 이름은 공용 이름이 아니라 **내가 보는 커스텀 이름**으로 처리
- 다른 사용자에게 실시간 전파할 필요 없음
- **REST API + 프론트 local patch** 방식으로 처리
- 성공 후 API 재조회보다는 응답값 기반 즉시 반영 우선

#### 저장 위치 방향

- 공용 `chat_room` 이 아니라 사용자-방 관계 테이블에 저장
- 예:
    - `custom_room_name`
    - `notification_enabled`

#### 제목 표시 우선순위

1. 사용자가 설정한 `customRoomName`
2. 없으면 fallback 제목
    - 1:1: 상대 이름
    - 그룹: 멤버 이름 조합

### 4-3. 오픈채팅 / 공용 설정 (추후 확장)

아래 항목은 **오픈채팅 도입 시점에 공용 설정으로 확장**한다.

- 공용 방 이름
- 방 설명
- 대표 이미지
- 공개 / 비공개 설정
- 초대 코드
- 최대 인원 제한
- 관리자 권한
- 강퇴 / 신고 / 메시지 삭제

즉, 현재 일반채팅/그룹채팅에는 공용 설정을 넣지 않고,  
오픈채팅에서 별도 도메인으로 확장하는 방향을 유지한다.

---

## 5. 메시지 검색

### 현재 고려 사항

- 키워드 기반 메시지 검색
- 작성자, 기간 필터 검색

### 기술 방향

- 초기에는 DB + Index 구조 고려 가능
- 규모가 커질 경우 Elasticsearch 도입 검토

### 상태

- 아직 우선순위는 낮음
- 현재는 핵심 채팅 흐름 안정화가 더 중요함

---

## 6. 보안 요구사항

- WebSocket CONNECT 시 JWT 인증
- HTTP 요청 시 JWT 기반 인증
- 파일 업로드 시 허용 확장자 / 크기 검증
- 잘못된 경로 접근 방지
- 채팅방 멤버만 관련 파일에 접근 가능해야 함

### 현재 중요 이슈 반영

- JWT 검증 시 issuer 불일치 문제를 방지해야 함
- 발급 규칙과 검증 규칙을 동일하게 유지해야 함
- issuer 값은 하드코딩보다 설정값 관리가 바람직함

---

## 7. 시스템 요구사항

### 7-1. 실시간 통신

- WebSocket 기반 (`SockJS + STOMP`)
- 접속 사용자와 채팅방 간 매핑 정보 Redis 활용 가능
- 초기에는 메시지 브로커 없이 개발
- 필요 시 Kafka / RabbitMQ 도입 고려

### 7-2. 메시지 저장

- 메시지는 DB(MySQL)에 저장
- 최근 메시지 / 보조 상태는 Redis 활용 가능
- 메시지 타입별 렌더링 분기 지원 필요

### 7-3. 파일 저장

- 업로드 파일은 프로젝트 내부 `resources` 가 아니라 **외부 디렉터리**에 저장
- 환경변수 기반 저장 경로 사용
- OS 경로 차이(Mac/Windows/Linux)를 고려한 설정 방식 필요
- `.env` 자동 로딩을 기대하지 않고, 실행 환경변수 명시 필요

---

## 8. 관리 및 유지보수

### 현재 필요

- orphan attachment cleanup scheduler
- 로그 기록
    - 메시지 송수신
    - reconnect
    - 인증 실패
    - 파일 업로드/다운로드

### 추후 확장

- 관리자 페이지
- 신고 메시지 처리
- 사용자 제재
- 메시지/파일 백업
- 정기 백업 및 복구

---

## 9. 현재 우선순위

### 완료 / 1차 안정화

- 1:1 채팅
- 그룹 채팅
- 읽음 처리
- unread count
- FILE / IMAGE 전송
- reconnect
- catch-up
- hidden 탭 read 처리
- JWT issuer 문제 해결
- CSS 충돌 해결
- 업로드 경로 환경변수 문제 해결

### 지금 다음 우선순위

1. 채팅방 개인 설정
    - 내가 보는 채팅방 이름 변경
    - 알림 끄기 / 켜기
    - 채팅방 나가기

### 이후 후보

- 메시지 검색
- 관리자 기능
- 오픈채팅
- 메시지 수정/삭제
- pinned message
- thread reply
- self-destruct message

---

## 10. 기술 스택 제안 (최신화)

| 항목 | 기술 |
|---|---|
| Frontend | Vue.js |
| API Server | Spring Boot |
| Auth Server | Spring Boot (분리) |
| WebSocket | Spring WebSocket + STOMP + SockJS |
| 인증 | JWT |
| 메시지 저장 | MySQL |
| 캐시/보조 | Redis |
| 파일 업로드 | Spring Multipart |
| 검색 | DB + Index / Elasticsearch(추후) |
| 알림 | Firebase Cloud Messaging (추후) |

---

## 11. 추가 정리 포인트

### 현재 문서에서 반드시 반영해야 하는 핵심

- reconnect / catch-up 요구사항
- unread count 와 read 처리 개념 분리
- `afterMessageId` 기반 누락 메시지 복구
- attachment 기반 파일 lifecycle
- orphan cleanup
- 개인 설정과 공용 설정의 구분
- 일반 그룹방과 오픈채팅의 구분
- 환경변수 기반 업로드 경로
- JWT issuer 검증 일치 필요

---

# 12. room setting 상세 설계 방향

## 12-1. 설계 목표

채팅방 설정은 **공용 메타데이터 수정 기능이 아니라 사용자 개인 설정 기능**으로 정의한다.

즉, 현재 단계에서 사용자는 다음을 설정할 수 있다.

- 내가 보는 채팅방 이름
- 해당 방 알림 수신 여부
- 해당 방 나가기

이 값들은 다른 멤버 화면에는 영향을 주지 않는다.

## 12-2. 데이터 저장 원칙

### 저장 위치

- `chat_room` 에 저장하지 않음
- 사용자-방 관계 데이터에 저장
- 현재 구조상 `chat_list` 또는 사용자별 채팅방 관계 테이블에 저장

### 저장 컬럼 예시

| 컬럼명 | 설명 |
|---|---|
| `custom_room_name` | 내가 보는 채팅방 이름 |
| `notification_enabled` | 해당 방 알림 수신 여부 |
| `deleted_at` 또는 상태값 | 채팅방 나가기 처리 시 사용 가능 |

## 12-3. 화면 반영 원칙

### API 방식

- 채팅방 설정 변경은 **REST API** 사용
- WebSocket 이벤트 전파는 현재 단계에서 사용하지 않음

### 반영 방식

- 설정 저장 성공 후 **응답값으로 프론트 local state patch**
- 성공 후 GET 재조회는 1차 구현에서는 하지 않음

### 이유

- 현재 변경 대상 값이 단순함
- 현재 사용자 화면만 바꾸면 됨
- 불필요한 네트워크 왕복을 줄일 수 있음

## 12-4. 제목 표시 규칙

현재 채팅방 제목은 아래 우선순위로 결정한다.

1. `customRoomName`
2. fallback 제목

### fallback 제목 규칙

- 1:1: 상대 사용자 이름
- 그룹방: 멤버 이름 조합

## 12-5. 채팅방 설정 UI 원칙

### 전역 `...` 버튼

- 계정/앱 설정 전용
- 환경설정
- 로그아웃

### 채팅방 설정 버튼

- 현재 채팅방 헤더에 별도 배치
- 현재 방이 열려 있을 때만 표시
- 전역 `...` 버튼과 의미를 섞지 않음

### 채팅방 설정 메뉴 항목

- 채팅방 이름 변경
- 알림 끄기 / 알림 켜기
- 채팅방 나가기

### UI 동작

- 채팅방 이름 변경 → 모달
- 알림 끄기/켜기 → 토글
- 채팅방 나가기 → 확인 모달

---

# 13. room setting API 초안

## 13-1. 채팅방 이름 변경

### 요청

`PATCH /v1/chat-room/{roomId}/settings/name`

```json
{
  "customRoomName": "백엔드 회의방"
}