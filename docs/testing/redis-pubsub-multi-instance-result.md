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