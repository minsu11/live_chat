# 테스트 전략과 SonarQube 커버리지 확인 방법

## SonarQube를 꼭 local에 설치해야 하나?

필수는 아니다. 선택지는 세 가지다.

1. **Local SonarQube Server**
   - Docker 또는 로컬 설치로 `http://localhost:9000`에서 직접 실행한다.
   - 개인 PC에서 품질 리포트를 확인하기 좋다.
2. **팀 공용 SonarQube Server**
   - 회사/팀 서버 한 곳에 SonarQube를 띄우고 모든 개발자가 같은 기준을 본다.
   - CI와 붙이기 가장 좋다.
3. **SonarCloud**
   - SonarSource가 운영하는 SaaS다.
   - 서버 운영이 싫고 GitHub 연동 중심이면 편하다.

이 프로젝트는 기본값을 local SonarQube(`http://localhost:9000`)로 두었고, CI/팀 서버에서는 환경변수로 바꿀 수 있게 했다.

```bash
SONAR_HOST_URL=http://localhost:9000 \
SONAR_TOKEN=<your-token> \
JAVA_HOME=/root/.local/share/mise/installs/java/21.0.2 \
./gradlew test jacocoTestReport sonar
```

> Java 프로젝트에서 SonarQube가 테스트 커버리지를 보려면 보통 JaCoCo XML 리포트를 입력으로 받는다. 그래서 대시보드와 품질 게이트는 SonarQube로 보되, 커버리지 XML 생성기는 JaCoCo를 유지한다.

## Local SonarQube Docker 실행 예시

```bash
docker run -d --name sonarqube -p 9000:9000 sonarqube:lts-community
```

브라우저에서 `http://localhost:9000` 접속 후 프로젝트 토큰을 만들고 `SONAR_TOKEN`으로 넘기면 된다.

## 현재 테스트 분류

| 분류 | 목적 | 권장 위치 | 방지하는 문제 |
| --- | --- | --- | --- |
| Controller unit test | 컨트롤러가 인증 사용자, path/query/body 값을 서비스로 정확히 전달하고 HTTP 응답을 감싸는지 검증 | `src/test/java/.../<domain>/controller/*ControllerTest.java` | 잘못된 status/message/data 응답, 서비스 인자 전달 오류 |
| Service unit test | 단일 서비스 메서드의 성공/실패 분기와 예외를 mock 기반으로 검증 | `src/test/java/.../<domain>/service/impl/*ServiceImplTest.java` | 비즈니스 분기 누락, 예외 처리 누락, 저장/조회 호출 오류 |
| Service orchestration test | Facade가 여러 서비스/Redis/브로드캐스터를 올바른 순서와 조건으로 조합하는지 검증 | `*FacadeServiceImplTest` | 메시지 저장 후 알림 누락, 차단 사용자 전송, 읽음/안읽음 메타 갱신 누락 |
| Repository unit test | Redis repository validation처럼 외부 저장소 어댑터의 단위 동작을 mock으로 검증 | `src/test/java/.../<domain>/repository/impl/*RepositoryImplTest.java` | key/value validation 누락, 저장소 API 호출 오류 |
| Repository integration test | 실제 JPA/H2 또는 Testcontainers DB에서 쿼리 결과를 검증 | `*RepositoryDataJpaTest` | mock으로는 못 잡는 JPQL/Querydsl/DDL mismatch |
| WebSocket/STOMP integration test | STOMP 연결, 인증 principal, message mapping, broadcast 흐름 검증 | `*StompIntegrationTest` | 실제 구독/발행 경로 오류, destination mismatch |
| User scenario test | 회원가입/로그인/친구/채팅방/메시지 같은 사용자 흐름 검증 | `*ScenarioTest` | 개별 단위는 성공하지만 전체 흐름이 깨지는 회귀 |

## SonarQube 분석 명령

```bash
SONAR_HOST_URL=http://localhost:9000 \
SONAR_TOKEN=<your-token> \
JAVA_HOME=/root/.local/share/mise/installs/java/21.0.2 \
./gradlew test jacocoTestReport sonar
```

분석 후 SonarQube UI에서 아래를 확인한다.

- Coverage
- Line Coverage
- Branch Coverage
- Bugs
- Vulnerabilities
- Code Smells
- Duplications
- Quality Gate

## 커버리지/품질 도구 비교

| 도구 | 용도 | 장점 | 단점 |
| --- | --- | --- | --- |
| SonarQube | 정적 분석 + coverage 대시보드 + quality gate | 팀 단위 품질 기준 관리, PR/CI 연동, 버그/취약점/중복/커버리지 통합 관리 | 서버 운영 또는 SonarCloud 연동 필요 |
| SonarCloud | SonarQube SaaS | 서버 운영 불필요, GitHub 연동 쉬움 | private repo/조직 정책에 따라 비용과 보안 검토 필요 |
| JaCoCo | Java coverage XML/HTML 생성 | SonarQube와 사실상 표준 조합, Gradle/JUnit 연동 쉬움 | 단독으로는 품질 게이트/추세 관리가 약함 |
| IntelliJ IDEA Coverage | 로컬 라인 커버리지 확인 | IDE에서 바로 색상으로 확인 가능 | 팀 공통 기준/CI 게이트로 쓰기 어려움 |
| PIT Mutation Testing | 테스트가 실제 버그를 잡는지 변이 테스트 | 라인 커버리지보다 테스트 품질 판단에 강함 | 느리고 초기 도입 비용이 큼 |

## 왜 테스트를 분리해야 하나?

- `ChatRoomController`를 고치면 `ChatRoomControllerTest`만 보면 된다.
- `UserServiceImpl`을 고치면 `UserServiceImplTest`만 보면 된다.
- 한 파일에 모든 테스트를 넣으면 실패 위치를 찾기 어렵고, fixture가 섞여 유지보수가 힘들어진다.
- 도메인/레이어별 패키지 구조를 main 코드와 맞추면 테스트 탐색 비용이 줄어든다.

## 주의할 점

- SonarQube coverage 100%가 좋은 테스트를 의미하지는 않는다.
- 단순 getter/setter나 record DTO를 억지로 커버하는 테스트보다 권한, 예외, 분기, 외부 전파 여부를 검증하는 테스트가 더 중요하다.
- JPA/Querydsl repository는 mock unit test만으로는 품질이 부족하다. 실제 쿼리 검증은 `@DataJpaTest` 또는 Testcontainers 기반 통합 테스트가 필요하다.
- WebSocket/STOMP와 실제 사용자 시나리오는 단위 테스트가 아니라 통합/시나리오 테스트로 분리해야 한다.
