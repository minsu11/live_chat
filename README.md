# chat server
 
# 개발 상태

- front(Vue) 연동 하는 작업에서 에러 발생이 잦아 완성하진 못함.
- 하루에 기능 하나 이상씩 구현 할 예정
- 채팅 서버로써 최소한 작업을 마친 뒤 서버 배포 예정

# 부가 서버

- front 서버 링크: https://github.com/minsu11/live_chat_front
- auth 서버 링크: https://github.com/minsu11/live_chat_auth


# 기능

1. 로그인
- jwt token 기반 로그인
- access token cookie 저장
- refresh token redis 저장
- RSA 알고리즘 채택
- 추 후 msa 환경으로 변환한다고 가정 했을 때
다중 서비스에서 비밀키 공유 하지 않고 공개키로 검증 가능, 
- 또한 HMAC은 여러 서비스에서 시크릿 키 공유해야하하는데, 유출 리스크가 있다고 판단햇음


2. 인가
- Spring security 활용한 인가 처리
- 로직
  - front 서버에서 api 서버로 요청
  - api 서버에서는 인가 처리 뒤, 만료되거나 변조된 토큰에 대해서 에러를 던짐
  - front 서버가 에러를 받으면, auth 서버에 재발급 api 요청
  - 재발급이 완료가 되면, api 서버에 요청 했던 로직 재요청
  - Vue interceptor 사용해서 front에서도 공통 처리를 함

3. 친구 검색

- 유저의 아이디 검색해서 친구 추가
- 현재는 단순히 where 절을 사용해서 유저를 찾음
- 유저 아이디는 민감한 정보라고 판단해서, GET 요청해서 파라미터로 보내는 방식이 아닌
POST 요청해서 유저아이디를 Body 데이터에 넣어둠

- 고민:
  - 유저 아이디로 검색하는 게 과연 맞을까 고민
  - 디스코드처럼 이름#숫자 이런 형식으로 하게끔 해야하나 고민중

4. 실시감 메시지 처리
- 현재 개발 중

# 처리해야하는 부분

- config server 만들어서 yml 파일 한군데 관리
- ddl문과 같은 문서 별도로 분리
- 민감한 정보들 암호화, 암호화가 안된다면 config 서버 사용해서 임시 방편 암호화
- common module 바꿔서 다른 서버들과 공통 처리 및 공통 메세지 처리 시키기

