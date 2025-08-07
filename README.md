# chat server

- [ ] 세션 로그인 방식 대신, 토큰 방식 로그인으로 변경
- [ ] 그로 인해, api서버에서는 인증 기능 x



# 📌 채팅 서버 요구사항 정의서 (단일 서버 기반)

## ✅ 목적

사용자가 **실시간으로 채팅**하고, 메시지를 **안정적이고 보안성 있게 처리**할 수 있는 시스템을 구축한다.  
Frontend, API Server, Auth Server 구조에서 **채팅 기능은 API Server에서 통합 처리**한다.

---

## ✅ 1. 기능 요구사항

### 💬 기본 채팅 기능

-  1:1 채팅 및 그룹 채팅 모두 지원

-  WebSocket 기반 메시지 실시간 송수신

-  클라이언트는 WebSocket에 JWT 기반 인증 후 연결

-  메시지 전송/수신 확인 (`읽음 여부`, `보낸 시간`)

-  **오프라인 사용자에게는 푸시 알림 연동** (Firebase 등)

-  채팅방 목록은 `최근 메시지 기준 정렬`

-  메시지 전송 실패/중복 방지를 위한 메시지 ID 및 재전송 처리


### 📌 메시지 기능 상세

-  일반 텍스트 메시지

-  이모지, 이미지, 파일 업로드 포함 메시지

-  **메시지 수정 및 삭제 (Soft Delete, 시간 제한 가능)**

-  **Pinned Message (상단 고정 메시지)**

-  **Thread Reply (스레드 형태로 답장)**

-  **Self-destruct 메시지 (n초 뒤 삭제되는 메시지)**


### 🔍 메시지 검색

-  키워드 기반 메시지 검색

-  작성자, 기간 필터 검색

-  Elasticsearch 또는 DB + Index 구조 활용


---

## ✅ 2. 채팅방 관리 기능

-  채팅방 생성 (1:1 / 그룹 선택)

-  채팅방 이름, 설명, 대표 이미지 설정 가능

-  공개 / 비공개 설정 가능 (초대코드 기반)

-  사용자는 채팅방을 **즐겨찾기 등록 / 나가기 가능**

-  채팅방에 **최대 인원 제한** 가능 (예: 50명)

-  **관리자가 유저 강퇴 / 메시지 삭제 가능**

-  나간 사용자: **이전 메시지 보기 권한 여부 설정 가능**


---

## ✅ 3. 시스템 요구사항

### 🧱 실시간 통신

- WebSocket 기반 (`SockJS + STOMP` or 순수 WebSocket + JSON)

- 접속 사용자와 채팅방 간 매핑 정보 Redis에 저장

- 메시지 브로커 없이도 우선 개발, 필요 시 Kafka/RabbitMQ 도입 고려


### 🧠 메시지 저장

- 메시지 전송 시 DB에 저장 (MySQL 기준)

- 최근 메시지 Redis에 캐시

- 메시지 타입: TEXT / IMAGE / FILE / SYSTEM / THREAD / TEMP


### 🔒 보안

- WebSocket 연결 시 JWT 인증 (Handshake 단계)

- 메시지 암호화는 클라이언트 또는 API 서버 내 처리 고려

- 파일 업로드 시 백엔드에서 바이러스 스캔 및 만료 시간 설정


---

## ✅ 4. 관리 및 유지보수

- 관리자 페이지에서:

    -  채팅방 목록 및 메시지 조회

    -  신고 메시지 삭제

    -  사용자 제재 (채팅 금지 or 강퇴)

    -  로그 기록: 메시지 송수신, 접속 기록

- 백업 및 복구:

    -  메시지 정기 백업

    -  파일 업로드 데이터의 자동 삭제 스케줄링


---

## ✅ 5. 기술 스택 제안

|항목|기술|
|---|---|
|WebSocket|Spring Boot + WebSocket or STOMP|
|인증|JWT + Redis 세션|
|메시지 저장|MySQL + Redis (캐시)|
|파일 업로드|Spring Multipart + MinIO or AWS S3|
|메시지 검색|Elasticsearch (선택)|
|실시간 상태 관리|Redis Pub/Sub (선택)|
|알림|Firebase Cloud Messaging (FCM)|

---

필요하다면 이 요구사항을 바탕으로:

- 📦 DB 테이블 설계 (`ChatRoom`, `Message`, `ChatRoomUser`, ...)

- 🔄 WebSocket 메시지 프로토콜 명세 (`SEND`, `SUBSCRIBE`, `ERROR`, 등)

- 🔧 Java Spring WebSocket 핸들러 코드 기본 틀

- 🎨 채팅 UI 구성요소 설계