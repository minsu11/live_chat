# 작업 내용 

- [ ] 인가 처리 마무리
- [ ] 헤더에 쿠키 만료시간 받기
- [ ] auth에는 헤더 담아서 보내기
- [ ] chat api
- [ ] 프론트 회원가입 axios 공통 처리한 부분 적용하기
- [ ] ci cd
- [ ] server 올리기
- [ ] auth server principal refactoring



# 기억 해야하는 부분

- 토큰 안에 있는 데이터는 uuid
- 인가 처리할 때 UserId 변수명으로 했지만, 실제 데이터는 User uuid
- user id 반환하는 부분은 무조건 uuid 응답하기(Long userId 하는 경우 종종 생김)

friend nickname 필드 추가로 
친구 목록을 가지고 올 때 해당 필드에 값이 있는지 확인하고 이름 가지고 오기

