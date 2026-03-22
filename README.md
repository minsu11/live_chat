# Chat Server

Spring Boot 기반 1:1 채팅 서버 개인 프로젝트입니다.  
현재는 **1:1 채팅 메시지 송수신 기능**을 중심으로 구현하고 있으며, 기능 구현과 함께 **ERD와 서비스 구조를 계속 정리하고 있는 프로젝트**입니다.

## 프로젝트 개요

채팅 기능은 단순히 메시지를 주고받는 것에서 끝나지 않고,  
채팅방 구조, 사용자 관계, 메시지 표시 방식, 서비스 계층 분리까지 함께 고민해야 한다고 생각했습니다.

이 프로젝트에서는 실제 기능을 구현하는 과정에서  
데이터 구조와 서비스 구조를 여러 번 다시 돌아보며 정리해 보았습니다.

## 개발 기간

- 진행 중

## 기술 스택

- Java
- Spring Boot
- Spring Security
- MySQL
- Redis
- Docker

## 현재 구현 범위

### 인증 / 인가
- JWT 기반 로그인
- Access Token 쿠키 저장
- Refresh Token Redis 저장
- Spring Security 기반 인가 처리

### 친구 기능
- 사용자 검색 기반 친구 추가
- 친구 목록 조회

### 채팅 기능
- 1:1 채팅 메시지 송수신

## 프로젝트에서 고민한 점

### 1. ERD 구조 재정리
처음에는 chatRoom과 chatList 중심으로 단순하게 생각했지만,  
사용자 관계와 메시지 표시 방식까지 고려하다 보니 각 역할을 더 분리할 필요가 있다고 느꼈습니다.

### 2. 닉네임 표시 방식
단순히 채팅방 이름만 저장해서 보여 주는 방식보다,  
사용자가 지정한 닉네임이나 관계에 따라 표시 방식이 달라질 수 있다고 생각했습니다.  
그래서 메시지와 채팅방에서 어떤 정보를 기준으로 사용자에게 보여 줄지 계속 고민하고 있습니다.

### 3. 서비스 계층 구조 정리
처음에는 Controller에서 바로 Application Service를 호출하는 구조로 시작했습니다.  
하지만 기능이 늘어나면서 어떤 로직은 한 서비스에 몰리고, 어떤 로직은 별도로 분리되는 식으로 흐름이 섞이기 시작했습니다.  
그래서 유스케이스 단위의 처리 과정을 한 곳에서 정리하기 위해  
`Controller -> Facade Service -> Application Service -> Repository` 구조로 정리하는 방향을 고민하고 적용했습니다.

## 아키텍처/구성

이 프로젝트는 채팅 서버를 중심으로 개발 중이며,  
프론트 서버와 인증 서버를 분리하는 방향도 함께 고려하고 있습니다.

- Front Server: https://github.com/minsu11/live_chat_front
- Auth Server: https://github.com/minsu11/live_chat_auth

## 디렉토리

```bash
live_chat/
├── .github/workflows
├── doc
├── http
├── src
├── Dockerfile
├── build.gradle
└── README.md