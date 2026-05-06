**채팅 핵심 흐름 테스트 결과 요약**

* 로그인 직후 UI 깨짐 현상 원인 확인 및 수정 완료

    * 원인: CSS 충돌
    * 결과: 로그인 후 채팅 리스트/채팅방 화면 정상 렌더링 확인

* JWT 인증 문제 원인 확인 및 수정 완료

    * 원인: `JwtTokenProvider`의 issuer 검증값이 실제 발급 토큰 issuer와 불일치
    * 결과: 로그인 후 HTTP 인증 및 WebSocket CONNECT 정상 동작 확인

* WebSocket 재연결 및 구독 복구 테스트 성공

    * 강제 소켓 끊김 후 자동 재연결 확인
    * 저장된 구독 자동 복구 확인

* 누락 메시지 catch-up 테스트 성공

    * reconnect 후 `afterMessageId` 기반으로 누락 메시지 정상 복구
    * 다건 메시지 복구 시 중복/순서 이상 없음 확인

* read / unread 정합성 테스트 성공

    * 활성 방 unread 처리 정상
    * hidden 탭 복귀 후 read 반영 정상
    * 그룹 채팅에서 unread count 감소 흐름 정상

* FILE / IMAGE 메시지 복구 테스트 성공

    * reconnect 후 파일/이미지 메시지 복구 확인
    * FILE 다운로드 링크 및 렌더링 정상 확인

* 테스트용 WebSocket 강제 끊김 코드는 dev 전용으로만 유지하도록 정리 완료
