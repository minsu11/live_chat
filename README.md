``# 💬 Live Chat API Server

Spring Boot 기반 실시간 채팅 **API 서버**입니다.  
현재 저장소는 채팅 도메인(채팅방/멤버십/메시지/읽음/첨부파일/알림)과 WebSocket 실시간 처리, Redis 메타데이터 동기화를 담당합니다.

## 📌 프로젝트 개요

- 1:1 기능에서 시작했지만, 코드 구조는 GROUP/OPEN 타입까지 확장 가능한 형태로 운영
- Front/Auth/API 서버를 분리해 책임을 분리
- 대량 트래픽에서의 DB 경합을 줄이기 위해 메타데이터 Write-Back 구조 적용B 경합을 줄이기 위해 메타데이터 Write-Back 구조 적용

## 아키텍처/구성
- Front Server: https://github.com/minsu11/live_chat_front
- Auth Server: https://github.com/minsu11/live_chat_auth
- API Server(현재 저장소): 채팅 도메인 + 실시간 브로드캐스트 + Redis-DB 동기화

## 기술 스택
- Java 17
- Spring Boot / Spring Security / Spring Data JPA
- MySQL / Redis
- WebSocket / STOMP
- Docker
---

## ✨ 구현 기능 (API 서버 기준)

### 1️⃣ 채팅방/멤버십
- DM/GROUP/OPEN 타입 기반 채팅방 모델 분리
- 1:1 채팅방 생성/조회, 그룹방 생성
- 멤버 조회/초대/나가기
- 멤버십 검증(비멤버 접근 차단)

### 2️⃣ 메시지/실시간 전송
- `@MessageMapping` 기반 WebSocket 메시지 송신
- TEXT/FILE/SYSTEM 메시지 저장
- 저장 완료 후 수신자별 브로드캐스트
- 차단 관계 검증 후 수신 대상 필터링
- 채팅방 목록 업데이트 이벤트 + 알림 이벤트 동시 전파

### 3️⃣ 메시지 조회/복구
- 커서 기반 과거 메시지 조회
- `afterMessageId` 기반 끊김 구간 catch-up 조회
- 페이지네이션 API와 복구 API 목적 분리

### 4️⃣ 읽음/안읽음 정합성
- 사용자별 `lastReadMessageId`, `unreadCount` 분리 관리
- 메시지 unread와 채팅목록 unread를 분리 계산
- 메시지 수신/채팅방 진입 시 read 메타 반영
- 신규 멤버 초대 시점 read 기준 초기화

### 5️⃣ 채팅방 표시 이름/설정
- 조회자 기준 표시 이름 계산
    - custom room name 우선
    - DM: 상대 표시 이름 fallback
    - GROUP: 방 이름 또는 멤버 이름 조합 fallback
- `chat_room_setting`으로 방 옵션 관리
    - 파일 업로드 허용
    - 메시지 수정/삭제 시간 제한
    - 기본 알림 여부

### 6️⃣ 첨부파일 처리
- 첨부파일 선업로드 후 메시지와 연결
- 연결 실패/미전송 파일(orphan attachment) 정리 스케줄러 운영
- 다운로드 API 제공

### 7️⃣ 인증/보안
- JWT 기반 HTTP 인증
- STOMP CONNECT 시 JWT 유효성 검증
- Refresh Token Redis 저장

### 8️⃣ API 계층 구조
- `Controller -> Facade -> Service -> Repository` 구조로 오케스트레이션 분리
- Facade에서 유스케이스 흐름(검증/저장/브로드캐스트/이벤트) 통합 관리

### 9️⃣ 친구/검색/프로필
- 친구 목록 커서 조회, 친구 등록
- 유저 ID 기반 검색
- 내 프로필 요약/상세 조회, 타인 프로필 조회
- 프로필 이미지 업로드, 프로필 수정

###  채팅 목록/복구 API
- 채팅방 목록 커서 조회
- `afterMessageId` 기반 누락 메시지 복구 API
- WebSocket read 이벤트(`/chat/read`)로 읽음 상태 동기화

---

## ⚡ Redis Write-Back 메타데이터 동기화

### 처리 대상
- 채팅방 메타: `lastMessageId`, `lastMessagePreview`, `lastMessageAt`
- 사용자 메타: `unreadCount`, `lastReadMessageId`, `lastOpenedAt`

### 처리 방식
- 실시간 경로: Redis Hash에 우선 반영
- 변경 추적: Dirty Set(`chat:user:dirty`, `chat:room:dirty`) 기록
- 배치 반영: 10초 주기 스케줄러가 Redis -> DB 동기화

### 의도
- 메시지 핫패스에서 동시 UPDATE 경합을 줄이고, DB 반영을 비동기로 분리

---

## 🔥 Trouble Shooting & Performance Tuning

### 1) 대규모 동시 발송 시 DB 데드락
- **문제**: 동일 채팅방 메타데이터를 DB에서 즉시 갱신하면서 lock 경합/데드락 발생
- **대응**: 메타데이터 즉시 갱신 경로를 Redis로 이동, DB 반영은 배치 스케줄러로 분리
- **결과**: API 핫패스의 DB 락 경합 완화, 고부하 구간 안정성 개선

### 2) 가짜 TPS와 실제 처리량 불일치
- **문제**: 발송량(TPS)만 보면 정상처럼 보이지만 롤백/실패가 포함될 수 있음
- **대응**: 발송량 + 실제 저장 건수 + 에러 로그를 함께 검증
- **결과**: 성능 수치를 성공률 기반으로 해석하는 기준 정리

### 3) unread 정합성 이슈
- **문제**: 목록 unread/메시지 unread를 단일 값처럼 다루면 쉽게 불일치 발생
- **대응**: `lastReadMessageId`와 `unreadCount`를 분리 저장하고, read 시점에 명시적 갱신
- **결과**: 재접속/재조회 후 unread 안정성 향상

### 4) orphan attachment 누적
- **문제**: 첨부 업로드 후 메시지 미전송 시 고아 데이터 누적
- **대응**: 메시지 연결 로직 분리 + 주기적 cleanup 스케줄러
- **결과**: 파일 스토리지/메타데이터 누수 리스크 감소

### 5) JWT 검증 규칙 불일치
- **문제**: 로그인/발급 성공과 API/STOMP 인증 성공이 일치하지 않는 케이스 발생
- **대응**: issuer 등 발급/검증 규칙을 코드 레벨에서 동일하게 강제
- **결과**: 인증 실패 원인 추적 범위 축소, 운영 안정성 개선

---

## 🎯 다음 개선 과제 (추천)

### 단기
- [ ] Redis -> DB 동기화 Bulk Update(JdbcTemplate)로 배치 시간 단축
- [ ] 배치 실패 복구 전략 강화(재시도/부분 실패 로그/idempotency 키)
- [ ] 채팅 메시지 검색(방 단위 키워드/기간/발신자 필터) API 추가
- [ ] Redis 장애 자동 복구(runbook + fallback) 도입
   - Redis 쓰기 실패 시 임시 DB 직접 반영 정책(서킷 브레이커)
   - 복구 후 미반영 메타 재동기화(replay job)

### 중기
- 관리자 기능 추가
    - 유저 제재(정지/차단), 신고 처리, 채팅방 강제 퇴장
    - 운영 감사 로그(AdminActionLog) 조회 API
- 관측성 강화
    - Micrometer + Prometheus + Grafana로 TPS, 지연시간, 에러율, 큐 적체 시각화
    - 배치 동기화 지표(처리건수, 실패건수, 지연시간) 대시보드화
- 메시지/운영 검색 고도화
    - 메시지 전문 검색 인덱스(예: OpenSearch)
    - 관리자 검색(유저/방/신고) 필터 API

### 확장
- 모바일 환경 최적화
    - 끊김 복구(catch-up)와 read ACK를 저전력/백그라운드 복귀 시나리오로 튜닝
    - 푸시 알림 토큰 연동과 알림 재전송 정책 정리
- 멀티 디바이스 동기화
    - 동일 계정 다중 세션의 읽음/알림 상태 동기화 정책 명시
- 안전한 전송 품질 보강
    - 메시지 중복 방지(client message id + idempotency)
    - ACK/재전송 정책으로 at-least-once 전달 보장

## 🔗 연관 저장소

- Front Server: https://github.com/minsu11/live_chat_front
- Auth Server: https://github.com/minsu11/live_chat_auth

``