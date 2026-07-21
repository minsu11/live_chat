# Chatalk 테스트 정리

> 목적: 테스트 파일 개수나 커버리지 수치를 외우기 위한 문서가 아니다. 각 테스트가 어떤 문제를 막기 위해 작성됐고, 실패했을 때 서비스에 어떤 영향이 생기는지를 내 말로 설명하기 위한 정리다.

---

## 1. 이 문서를 만든 이유

Chatalk의 테스트 코드는 성공, 실패, 예외, 경계값을 기준으로 많이 보강됐다. 하지만 테스트 코드를 직접 처음부터 모두 작성하지 않았기 때문에, 코드가 통과한다는 사실과 내가 설명할 수 있다는 것은 별개의 문제였다.

면접에서는 보통 다음 순서로 질문이 깊어진다.

1. 테스트를 작성했나요?
2. 어떤 기능을 테스트했나요?
3. 왜 그 테스트가 필요했나요?
4. Mock으로 무엇을 대체했나요?
5. 해당 테스트가 어떤 장애를 방지하나요?
6. 단위 테스트로 충분하지 않은 부분은 무엇인가요?

따라서 이 문서는 각 테스트를 `기능 → 위험 → 테스트 구성 → 검증 → 한계` 순서로 다시 읽기 위해 만들었다.

---

## 2. 현재 테스트 전략 한 문장 정리

> Chatalk에서는 메시지 저장과 실시간 전파, 사용자별 읽음 상태, Redis Write-Back, 재연결 복구처럼 데이터 정합성에 영향을 주는 흐름을 우선으로 테스트했고, 정상 결과뿐 아니라 실패 이후 후속 작업이 중단되는지까지 확인했다.

---

## 3. 테스트를 읽는 기본 순서

테스트 메서드를 볼 때 아래 다섯 가지를 먼저 찾는다.

### 1. 어떤 기능을 테스트하는가?

예:

- 메시지 전송
- 읽음 처리
- 채팅방 생성
- Redis 배치 동기화
- 파일 업로드
- JWT 인증

### 2. 어떤 상황을 만들었는가?

```java
when(chatMessageService.saveMessage(any()))
        .thenThrow(new RuntimeException("DB 저장 실패"));
```

이 코드는 실제 DB를 고장 낸 것이 아니라, 메시지 저장 Service가 실패하는 상황을 Mock으로 만든 것이다.

### 3. 어떤 메서드를 실행했는가?

```java
facade.sendMessage(request, userId);
```

테스트 대상은 Mock이 아니라 실제 `Facade` 객체다. Facade가 의존하는 Service와 Broadcaster를 Mock으로 바꿨다.

### 4. 어떤 결과를 검증했는가?

```java
assertThat(result.roomId()).isEqualTo(roomId);
```

반환값이나 Entity 상태가 기대값과 같은지 확인한다.

### 5. 어떤 호출이 발생하거나 발생하지 않았는가?

```java
verify(broadcaster, never())
        .broadcastMessage(anyLong(), any());
```

예외만 발생하는지 확인하는 것보다 중요할 수 있다. 실패한 메시지가 사용자에게 전달되는 부작용을 막는지 확인하기 때문이다.

---

## 4. 테스트에서 자주 사용하는 문법

### `when(...).thenReturn(...)`

의존 객체가 특정 값을 반환하도록 설정한다.

```java
when(chatRoomRepository.findById(1L))
        .thenReturn(Optional.of(chatRoom));
```

### `when(...).thenThrow(...)`

의존 객체가 실패하는 상황을 만든다.

```java
when(chatRoomRepository.save(any()))
        .thenThrow(new DataIntegrityViolationException("duplicate"));
```

### `assertThat(...)`

반환값 또는 상태를 확인한다.

```java
assertThat(chatRoom.getParticipantCount()).isEqualTo(3);
```

### `assertThatThrownBy(...)`

예외의 타입과 메시지를 확인한다.

```java
assertThatThrownBy(() -> service.getChatRoom(999L))
        .isInstanceOf(ChatRoomNotFoundException.class);
```

### `verify(...)`

의존 객체의 메서드가 기대한 인자로 호출됐는지 확인한다.

```java
verify(repository).save(message);
```

### `verify(..., never())`

실패 이후 실행되면 안 되는 작업을 확인한다.

```java
verify(redisService, never())
        .updateRoomMeta(anyLong(), anyLong(), anyString(), any());
```

### `ArgumentCaptor`

Mock 메서드에 실제로 전달된 객체를 꺼내 내부 값을 확인한다.

```java
ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
verify(repository).save(captor.capture());
assertThat(captor.getValue().getMessageContent()).isEqualTo("안녕하세요");
```

### `InOrder`

여러 의존 객체의 호출 순서를 확인한다.

```java
InOrder inOrder = inOrder(chatListService, memberService, roomService);
inOrder.verify(chatListService).leaveChatRoom(roomId, userId);
inOrder.verify(memberService).leaveRoomMember(roomId, userId);
inOrder.verify(roomService).decrementParticipantCount(roomId);
```

---

## 5. 기능별 테스트 지도

| 기능 | 정상 흐름 | 실패·예외 흐름 | 경계값 |
| --- | --- | --- | --- |
| 메시지 전송 | 저장 후 사용자별 브로드캐스트 | 방 없음, 저장 실패, Redis 실패, 참여자 조회 실패 | FILE 메시지 attachmentId 누락 |
| 읽음 처리 | 읽음 상태 저장과 이벤트 전파 | Redis 장애, 멤버 조회 실패, unread 계산 실패 | 마지막 메시지 ID 0 |
| Redis 배치 | Dirty 데이터를 DB에 일괄 반영 | DB batchUpdate 실패, 잘못된 Dirty Key | Dirty Set 없음, 선택 값 누락 |
| 채팅방 생성 | 기존 DM 반환, 신규 DM·그룹방 생성 | 사용자 없음, 저장 실패, Unique 충돌 복구 실패 | 중복 UUID, 빈 초대 목록 |
| 채팅 목록 | Cursor 조회, Redis 메타 병합 | DB row 없음, 잘못된 Cursor 생성 조건 | orderAt null, 빈 사용자 목록 |
| 파일 | 이미지와 일반 파일 저장 | 잘못된 MIME·확장자, IOException | 20MB, 20MB + 1byte |
| 표시 이름 | 커스텀·DM·그룹·오픈 제목 | 채팅방 없음 | 이름 3명 초과, 공백·중복 이름 |
| HTTP 인증 | 유효 JWT로 SecurityContext 설정 | 쿠키 없음, 검증 실패 | 기존 인증 객체가 이미 있음 |
| STOMP 인증 | CONNECT에서 Principal 설정 | 헤더 없음, Bearer 아님, JWT 실패 | SUBSCRIBE는 추가 인증 없이 통과 |
| 프로필 | 닉네임·상태 메시지·이미지 수정 | 파일 저장 실패 | 일부 필드만 전달, 모두 null |
| 채팅방 나가기 | 목록·멤버·인원·시스템 메시지 처리 | 목록 제거 실패, 멤버 처리 실패 | 실패 지점 이후 후속 호출 없음 |

---

# 6. 대표 테스트를 내 말로 설명하기

## 6.1 메시지 저장 실패 시 브로드캐스트 중단

### 기능

사용자가 보낸 메시지를 저장하고 채팅방 멤버에게 실시간으로 전달한다.

### 위험

DB 저장 전에 WebSocket 전파가 실행되거나, 저장 실패 이후에도 전파가 계속되면 클라이언트에는 보이지만 재조회할 수 없는 메시지가 생긴다.

### 테스트 구성

- 채팅방 멤버 검증은 성공하도록 설정
- 메시지 저장 Service에서 예외 발생
- Facade 메시지 전송 실행

### 검증

- 예외가 호출자에게 전달되는지 확인
- Redis 메타데이터가 갱신되지 않았는지 확인
- Broadcaster가 호출되지 않았는지 확인
- 알림 이벤트가 전파되지 않았는지 확인

### 면접 답변

> 메시지 저장과 실시간 전파의 순서를 검증했습니다. 저장에 실패했는데도 브로드캐스트가 실행되면 클라이언트에는 보이지만 DB에는 없는 메시지가 생길 수 있습니다. 그래서 저장 예외 이후 Redis 갱신과 WebSocket 전파가 호출되지 않는지 확인했습니다.

---

## 6.2 Redis 배치 실패 시 Dirty Key 유지

### 기능

Redis에 먼저 기록한 채팅방·사용자 메타데이터를 주기적으로 DB에 반영한다.

### 위험

DB 반영에 실패했는데 Dirty Key를 삭제하면 해당 데이터는 다음 배치 대상에서 사라진다.

### 테스트 구성

- Dirty Set에 동기화 대상 추가
- Redis Hash에 unread count 또는 마지막 메시지 메타데이터 설정
- `JdbcTemplate.batchUpdate()`에서 예외 발생

### 검증

```java
verify(setOperations, never())
        .remove(DIRTY_KEY, targetKey);
```

### 면접 답변

> Write-Back 방식에서는 DB 반영 성공과 Dirty Key 제거 순서가 중요합니다. DB 반영이 실패한 경우 Dirty Key를 유지해야 다음 스케줄에서 재시도할 수 있으므로, batchUpdate 예외 이후 remove가 호출되지 않는지 검증했습니다.

---

## 6.3 Redis 캐시 미스 상태에서 unread count 증가

### 기능

메시지를 받지 않은 사용자의 채팅방 unread count를 증가시킨다.

### 위험

DB unread count가 5인 상태에서 Redis key가 없다면, Redis에서 바로 increment할 경우 값이 1부터 시작한다.

### 기대 흐름

```text
Redis key 확인
→ key가 없으면 DB unreadCount 조회
→ Redis에 DB 값 저장
→ 1 증가
```

### 면접 답변

> 캐시 미스가 곧 데이터가 0이라는 뜻은 아닙니다. DB의 기존 값을 Redis에 먼저 올린 뒤 증가해야 정합성이 유지됩니다. 테스트에서는 DB 조회, 캐시 워밍, increment 순서와 최종 호출을 확인했습니다.

---

## 6.4 동일한 DM 채팅방의 동시 생성

### 기능

두 사용자 사이에 기존 DM이 있으면 반환하고, 없으면 생성한다.

### 위험

동시에 두 요청이 실행되면 두 요청 모두 기존 방이 없다고 판단할 수 있다.

### 처리 방식

```text
사용자 조합으로 DM hash 생성
→ 기존 방 조회
→ 저장 시 DB Unique Constraint 충돌
→ 동일 hash로 기존 방 재조회
```

### 테스트 구성

- 첫 조회는 빈 값
- 저장 시 `DataIntegrityViolationException`
- 재조회에서는 기존 방 반환

### 면접 답변

> 조회 후 생성 방식만으로는 동시 요청을 막을 수 없습니다. DB Unique Constraint를 최종 방어선으로 사용하고, 충돌이 발생하면 먼저 생성된 방을 다시 조회해 반환하도록 했습니다. 테스트에서는 충돌 복구 성공과 재조회 실패를 나눠 확인했습니다.

---

## 6.5 파일 크기 경계값

### 기능

설정한 최대 크기 이하의 채팅 파일만 저장한다.

### 위험

`>`와 `>=`를 잘못 사용하면 정확한 제한 크기의 파일까지 거부하거나, 제한을 1byte 초과한 파일을 허용할 수 있다.

### 테스트 값

```text
20MB       → 허용
20MB + 1B  → 거부
```

### 면접 답변

> 단순히 큰 파일 하나만 테스트하지 않고 제한값과 제한 바로 다음 값을 확인했습니다. 이런 테스트는 비교 연산자의 경계 오류를 잡기 위해 작성했습니다.

---

## 6.6 Cursor 문자열 내부 값 검증

### 기능

채팅방 목록의 마지막 항목을 기준으로 다음 페이지 Cursor를 만든다.

### 부족한 검증

```java
assertThat(result.next()).isNotBlank();
```

문자열이 존재하는 것만 확인하면 잘못된 시간이나 roomId를 인코딩해도 테스트가 통과한다.

### 보강한 검증

```java
ChatListCursorKey decoded = ChatListCursorCodec.decode(result.next());
assertThat(decoded.lastRoomId()).isEqualTo(lastRoomId);
assertThat(decoded.lastAtEpochMillis()).isEqualTo(expectedTime);
```

### 면접 답변

> Cursor가 생성됐다는 사실보다 어떤 값이 들어갔는지가 중요합니다. 생성한 Cursor를 다시 디코딩해서 마지막 데이터의 정렬 시간과 roomId가 정확히 들어갔는지 확인했습니다.

---

## 6.7 읽음 처리 중간 실패 이후 이벤트 중단

### 기능

사용자가 메시지를 읽으면 읽음 메타데이터를 저장하고 방 멤버에게 unread count 변경 이벤트를 보낸다.

### 위험

읽음 계산에 실패했는데 이벤트가 전파되면 사용자의 화면에는 잘못된 숫자가 표시된다.

### 테스트 구성

- 읽음 상태 저장 성공
- 참여자 조회 성공
- unread count 계산에서 예외 발생

### 검증

- `READ_UPDATED` 이벤트 미전파
- 채팅방 목록 upsert 이벤트 미전파

### 면접 답변

> Facade의 중간 단계에서 실패했을 때 뒤의 이벤트가 실행되지 않는지를 확인했습니다. 예외 발생 여부뿐 아니라 잘못된 상태가 다른 사용자에게 전파되지 않는지가 핵심이었습니다.

---

## 6.8 HTTP JWT 인증 필터

### 기능

HTTP 요청의 `accessToken` 쿠키를 검증하고 인증 사용자를 SecurityContext에 저장한다.

### 확인한 경우

- WebSocket과 로그인 경로가 필터 제외 대상인지
- 유효한 토큰이면 Principal과 권한이 저장되는지
- 쿠키가 없으면 `INVALID_TOKEN`이 기록되는지
- 토큰 검증 실패 시 사용자 조회가 실행되지 않는지
- 이미 인증된 객체가 있으면 덮어쓰지 않는지

### 면접 답변

> 인증 성공만 테스트하지 않고 토큰이 없거나 검증에 실패했을 때 SecurityContext가 비어 있는지, AuthorizationService를 불필요하게 호출하지 않는지 확인했습니다. 기존 인증 객체가 있는 경우에는 덮어쓰지 않는 분기도 검증했습니다.

### 코드에서 다시 볼 부분

`JwtAuthenticationFilter`의 `shouldNotFilter()`와 `doFilterInternal()`을 함께 읽는다. 특히 제외 경로는 `OncePerRequestFilter`가 `doFilterInternal()` 호출 전에 판정한다는 점을 이해한다.

---

## 6.9 STOMP CONNECT 인증

### 기능

WebSocket 연결의 첫 STOMP CONNECT 프레임에서 JWT를 확인하고 Principal을 설정한다.

### 확인한 경우

- `Authorization: Bearer <token>` 정상 처리
- Authorization 헤더 누락
- Bearer가 아닌 인증 방식
- JWT 검증 실패
- CONNECT 이후 SUBSCRIBE에서는 추가 토큰 검증 없음

### 면접 답변

> HTTP 인증과 WebSocket 인증 경로가 다르기 때문에 STOMP CONNECT 시점에 별도 검증이 필요했습니다. 유효한 Bearer 토큰에서만 Principal을 설정하고, 형식 오류나 검증 실패에서는 사용자 조회를 진행하지 않는지 확인했습니다.

---

## 6.10 프로필 부분 수정

### 기능

닉네임과 상태 메시지를 선택적으로 수정하고, 프로필 이미지는 파일 저장 후 URL을 갱신한다.

### 확인한 경우

- 닉네임과 메시지 모두 수정
- 닉네임만 수정
- 메시지만 수정
- 둘 다 null이면 저장 Service 미호출
- 이미지 저장 성공 후 URL 갱신
- 이미지 저장 실패 시 URL 갱신 미실행

### 면접 답변

> 부분 수정 요청에서는 전달되지 않은 필드를 기존 값으로 유지해야 합니다. null인 필드의 Service를 호출하지 않는지 확인했고, 이미지 파일 저장에 실패한 경우 저장되지 않은 URL이 프로필에 반영되지 않는지도 검증했습니다.

---

## 6.11 채팅방 나가기

### 기능

채팅방 목록 제거, 멤버 비활성화, 참여자 수 감소, 퇴장 시스템 메시지 전파를 조율한다.

### 확인한 경우

- 정상 처리 순서
- 채팅 목록 제거 실패 후 후속 작업 중단
- 멤버 비활성화 실패 후 참여자 수와 시스템 메시지 변경 중단

### 면접 답변

> 여러 상태를 바꾸는 작업이라 호출 순서와 실패 지점이 중요했습니다. `InOrder`로 정상 순서를 확인했고, 각 실패 지점 뒤에 있는 작업이 호출되지 않는지 검증했습니다. 실제 트랜잭션 롤백은 통합 테스트가 추가로 필요합니다.

---

# 7. 단위 테스트와 통합 테스트 구분

## 현재 많이 작성된 테스트

현재 테스트의 대부분은 JUnit 5와 Mockito를 이용한 단위 테스트다.

장점:

- 빠르게 실행된다.
- 실패 상황을 만들기 쉽다.
- 특정 클래스의 분기와 호출 조건을 자세히 확인할 수 있다.

한계:

- 실제 SQL이 실행되지 않는다.
- JPA 매핑 오류를 찾지 못한다.
- Spring Transaction 롤백을 확인하지 못한다.
- 실제 Redis와 WebSocket 연결 동작을 보장하지 않는다.

## Repository는 왜 통합 테스트가 필요한가?

Querydsl Repository를 Mock으로 테스트하면 “Querydsl 메서드를 호출했다”는 사실만 확인하기 쉽다. 하지만 실제로 중요한 것은 다음과 같다.

- WHERE 조건이 맞는가?
- 정렬 순서가 맞는가?
- Cursor 경계에서 중복·누락이 없는가?
- JOIN 때문에 row가 중복되지 않는가?
- MySQL의 Full-Text Search가 실제로 동작하는가?

따라서 Repository 구현체는 `@DataJpaTest` 또는 Testcontainers MySQL 테스트로 확인해야 한다.

## WebSocket은 왜 통합 테스트가 필요한가?

현재는 Interceptor와 Broadcaster 단위 테스트로 다음을 확인한다.

- 토큰 검증 분기
- Principal 설정
- destination 문자열
- RedisPublisher 호출

하지만 실제 검증에는 다음이 더 필요하다.

```text
WebSocket handshake
→ STOMP CONNECT
→ SUBSCRIBE
→ SEND
→ Controller
→ Facade
→ Broker 또는 Redis Pub/Sub
→ 구독 클라이언트 수신
```

이 흐름은 실제 서버 포트를 띄운 통합 테스트에서 확인해야 한다.

---

# 8. JaCoCo 커버리지 해석

## 현재 기준

```text
Line Coverage   >= 60%
Branch Coverage >= 40%
```

최신 `feature/test-code` 브랜치 결과:

- 전체 테스트: **326개**
- 실패: **0개**
- Line Coverage: **64.36%** (`1,647 / 2,559`)
- Branch Coverage: **65.23%** (`424 / 650`)

## 커버리지가 의미하는 것

- Line Coverage: 실행된 코드 줄의 비율
- Branch Coverage: if, switch, 삼항 연산자 등의 각 분기를 실행한 비율

## 커버리지가 의미하지 않는 것

커버리지가 100%여도 검증문이 부실하면 버그를 잡지 못할 수 있다.

```java
service.sendMessage(request);
```

이 코드만 실행해도 많은 라인이 커버될 수 있지만 결과, 상태, 외부 전파를 확인하지 않으면 좋은 테스트라고 보기 어렵다.

## 측정에서 제외한 코드

- DTO
- 설정 클래스와 Properties
- enum
- 단순 Exception
- Querydsl 자동 생성 Q 클래스
- Spring Boot Application 진입점

Service, Facade, Controller, Repository 구현체, Scheduler, Filter, Interceptor는 측정에 포함했다.

---

# 9. 면접에서 AI 사용을 질문받았을 때

숨기거나 과장하지 않는다.

### 답변 예시

> 테스트가 부족한 기능을 찾고 성공, 실패, 경계값 시나리오를 확장하는 과정에서 AI를 활용했습니다. 다만 생성된 코드를 그대로 끝내지 않고 실제 DTO와 메서드 구조를 다시 맞췄고, GitHub Actions의 컴파일 오류와 테스트 결과를 기준으로 수정했습니다. 이후 면접에서 설명할 수 있도록 메시지 저장 실패 후 브로드캐스트 중단, Redis Dirty Key 재시도, DM 동시 생성 충돌 같은 핵심 시나리오를 직접 다시 분석해 문서화했습니다.

### 피해야 할 답변

> AI가 다 작성해서 정확히는 잘 모릅니다.

### 더 중요한 기준

도구를 사용했는지보다 아래 질문에 답할 수 있는지가 중요하다.

- 왜 이 테스트가 필요한가?
- Mock으로 무엇을 대체했는가?
- 무엇을 검증했는가?
- 이 테스트로 잡지 못하는 문제는 무엇인가?

---

# 10. 테스트 파일을 공부하는 순서

## 1단계: 정합성에 직접 영향을 주는 테스트

1. `ChatMessageFacadeServiceImplTest`
2. `ChatMessageFacadeServiceImplFailureTest`
3. `ChatReadFacadeServiceImplTest`
4. `ChatReadFacadeServiceImplFailureTest`
5. `ChatMetadataBatchSchedulerTest`
6. `ChatMetadataRedisServiceTest`

## 2단계: 채팅방과 목록

1. `ChatRoomServiceImplTest`
2. `ChatRoomFacadeServiceImplTest`
3. `ChatRoomFacadeServiceImplValidationTest`
4. `ChatListServiceImplTest`
5. `ChatListServiceImplAdditionalTest`
6. `ChatRoomDisplayResolverImplTest`

## 3단계: 파일과 사용자 기능

1. `FileServiceImplTest`
2. `ChatAttachmentCleanupServiceImplTest`
3. `UserProfileServiceImplBehaviorTest`
4. `UserProfileFacadeImplTest`
5. `ChatRoomMemberServiceImplBehaviorTest`
6. `ChatRoomSettingFacadeServiceImplTest`

## 4단계: 인증과 실시간 전파

1. `JwtAuthenticationFilterTest`
2. `StompAuthChannelInterceptorTest`
3. `WebSocketBroadcasterImplementationsTest`
4. `GlobalExceptionHandlerTest`

---

# 11. 한 테스트를 공부할 때 작성할 메모 양식

아래 양식을 Notion 하위 페이지나 블로그 초안에 복사해서 사용한다.

```markdown
## 테스트 이름

### 기능

### 발생할 수 있는 문제

### Given

### When

### Then

### Mock으로 대체한 대상

### 이 테스트가 막는 장애

### 이 테스트의 한계

### 면접에서 30초 답변
```

---

# 12. 블로그 글로 확장할 수 있는 주제

## 글 1. 실시간 채팅에서 저장 실패 후 브로드캐스트를 막은 이유

구성:

1. 메시지 저장과 전파 순서
2. 잘못된 순서에서 생기는 정합성 문제
3. Facade 단위 테스트 구성
4. `verify(..., never())`를 사용한 이유
5. 단위 테스트의 한계

## 글 2. Redis Write-Back에서 Dirty Key를 언제 지워야 할까?

구성:

1. Write-Back을 적용한 이유
2. Dirty Set 역할
3. DB batchUpdate 실패 상황
4. 성공한 Key만 제거하는 방식
5. AOF와 멱등성의 남은 과제

## 글 3. DM 채팅방 동시 생성과 Unique Constraint

구성:

1. 조회 후 생성의 race condition
2. 애플리케이션 Lock 대신 DB Unique Constraint를 사용한 이유
3. 충돌 후 재조회
4. 성공·실패 테스트
5. 분산 환경에서의 고려 사항

## 글 4. 커서가 존재하는지만 검사하면 부족한 이유

구성:

1. Cursor pagination의 목적
2. 단순 `isNotBlank()` 테스트의 한계
3. Cursor decode 검증
4. 중복·누락을 막기 위한 Repository 통합 테스트 필요성

---

# 13. 아직 남은 테스트 과제

우선순위는 커버리지 수치가 아니라 실제 코드의 위험도 기준으로 잡는다.

## 1순위: Querydsl Repository 통합 테스트

- 채팅방 목록 Cursor 정렬
- 메시지 검색 Cursor
- 메시지 Context 조회
- 멤버 JOIN 결과 중복 여부
- 사용자·프로필 조회 Projection

MySQL 전용 기능이 있으므로 Testcontainers MySQL을 우선 검토한다.

## 2순위: 실제 STOMP 통합 테스트

- CONNECT 인증 성공·실패
- 사용자별 `/user` destination 수신
- READ_UPDATED 이벤트 수신
- 재연결 후 catch-up과 실시간 메시지 병합

## 3순위: Redis 장애 자동화 테스트

- Redis 중단 시 Circuit Breaker fallback
- Redis 복구 이후 상태 전환
- Dirty 데이터 재동기화
- 같은 데이터의 중복 반영 방지

## 4순위: 사용자 시나리오 테스트

```text
회원가입
→ 친구 등록
→ DM 생성
→ 메시지 전송
→ 상대 unread 증가
→ 상대방 채팅방 진입
→ READ_UPDATED
→ 연결 종료
→ 메시지 추가 전송
→ 재연결 catch-up
```

---

# 14. 최종 점검 질문

아래 질문에 답할 수 있으면 기본적인 면접 대비가 된 상태다.

- [ ] 단위 테스트와 통합 테스트의 차이를 설명할 수 있다.
- [ ] Mock을 사용한 이유와 단점을 설명할 수 있다.
- [ ] 메시지 저장 실패 테스트에서 `never()`를 사용한 이유를 설명할 수 있다.
- [ ] Redis Dirty Key를 DB 반영 성공 후 지워야 하는 이유를 설명할 수 있다.
- [ ] 캐시 미스와 데이터가 0인 상태가 다른 이유를 설명할 수 있다.
- [ ] DM 동시 생성에서 Unique Constraint가 필요한 이유를 설명할 수 있다.
- [ ] 파일 크기 경계값 테스트를 설명할 수 있다.
- [ ] Cursor를 디코딩해서 검증한 이유를 설명할 수 있다.
- [ ] HTTP JWT와 STOMP JWT 인증 경로가 다른 이유를 설명할 수 있다.
- [ ] 커버리지 60%가 좋은 테스트를 보장하지 않는 이유를 설명할 수 있다.
- [ ] 현재 Repository 테스트가 부족한 이유를 설명할 수 있다.
- [ ] AI를 어떻게 활용했고 결과를 어떻게 검증했는지 솔직하게 설명할 수 있다.

---

# 15. 1분 요약 답변

> Chatalk에서는 메시지 전송, 읽음 처리, Redis 메타데이터 동기화, 재연결 복구처럼 데이터 정합성에 영향을 주는 기능을 우선 테스트했습니다. 정상 흐름뿐 아니라 DB 저장 실패, Redis 장애, 잘못된 입력, 파일 크기 경계값, 동일 DM 동시 생성 같은 상황을 나눠 확인했습니다. 특히 예외가 발생했는지만 보는 것이 아니라 실패 이후 WebSocket 이벤트나 Redis 갱신이 실행되지 않는지도 검증했습니다. 현재 단위 테스트와 CI 커버리지 기준은 마련했지만, Querydsl 쿼리와 실제 STOMP 연결은 Testcontainers와 통합 테스트로 보강해야 한다고 보고 있습니다.
