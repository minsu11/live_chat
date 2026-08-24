# Redis Pub/Sub Multi-Instance Validation

## Test Environment

- API A: localhost:7070
- API B: localhost:7071
- Front A: localhost:8080
- Front B: localhost:8081
- Redis Channel: `chatroom`
- Shared MySQL / Redis

## Validation Scenario

1. 두 API 인스턴스가 동일한 Redis `chatroom` 채널 구독
2. 서로 다른 API 인스턴스에 사용자 WebSocket 연결
3. 한 사용자가 `테스트 입니다.` 메시지 전송
4. 송신 API에서 Redis Publish 수행
5. 다른 API 인스턴스의 Redis Subscriber가 메시지 수신
6. 해당 인스턴스의 STOMP 세션으로 실시간 전달
7. DB 저장 결과 확인

## Evidence

### 1. Redis Subscriber 확인

두 API 인스턴스가 동일한 `chatroom` 채널을 구독한 상태에서
Subscriber 수가 2개인 것을 확인했습니다.

![Redis NUMSUB](redis-pubsub-multi-instance/01.%20redis_numsub.PNG)

### 2. 송신 사용자

Front B에서 테스트 메시지 `테스트 입니다.`를 전송했습니다.

![Sender Front](redis-pubsub-multi-instance/02.%20sender_front.PNG)

### 3. 송신 인스턴스 Redis Publish

송신 API 인스턴스에서 Redis `chatroom` 채널로 Publish가 수행되는 것을 확인했습니다.

![Sender Log](redis-pubsub-multi-instance/03.%20sender_log.PNG)

### 4. 수신 사용자

다른 API 인스턴스에 연결된 Front A에서 동일한 메시지를 실시간으로 수신했습니다.

![Receiver Front](redis-pubsub-multi-instance/04.%20receiver_front.PNG)

### 5. 수신 인스턴스 Redis Subscriber

수신 API 인스턴스의 Redis Subscriber가 동일한 메시지 ID `252693`을 수신하고
STOMP 세션 1개로 브로드캐스트하는 것을 확인했습니다.

![Receiver Log](redis-pubsub-multi-instance/05.%20receiver_log.PNG)

### 6. DB 저장 확인

동일한 메시지 ID `252693`이 DB에 1건 저장된 것을 확인했습니다.

![DB Result](redis-pubsub-multi-instance/06.%20db_result.PNG)


## Result

- Redis Subscriber: 2
- Test Message ID: `252693`
- Message Type: `TEXT`
- Message Content: `테스트 입니다.`
- Cross-instance realtime delivery: PASS
- DB persistence: 1 row confirmed

## Conclusion

서로 다른 API 인스턴스에 연결된 사용자 간 메시지가
Redis Pub/Sub을 통해 전달되고,
수신 인스턴스의 WebSocket/STOMP 세션까지 정상 브로드캐스트되는 것을 확인했다.

