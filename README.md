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

## **Trouble Shooting & Performance Tuning**

### [성능 최적화] 대규모 트래픽 대비 실시간 채팅 동시성 제어 및 아키텍처 개선

1. 도입 배경 및 문제 상황 (As-Is)

- 기존 구조: 사용자가 메시지를 발송할 때마다 chat_message 테이블에 INSERT하고, 동시에 채팅방 목록(chat_list)의 '안 읽음 개수'와 '마지막 메시지'를 업데이트하기 위해 DB UPDATE 쿼리를 실시간으로 실행함.

- 성능 테스트 결과: 500명의 가상 유저가 동시 다발적으로 메시지를 발송하는 부하 테스트 진행 결과, 동일한 채팅방 행(Row)을 수정하려는 트랜잭션들이 충돌하여 DB Lock 대기 및 데드락(Deadlock)이 100% 발생함.

- 영향: 트랜잭션 롤백으로 인한 다수의 메시지 유실 및 WebSocket 연결 강제 종료(Connection reset) 현상 발생. (실패한 쿼리 포함 측정된 가짜 TPS: 2,686, 에러율 80% 이상)

#### **Log**

```text
2026-05-03 21:41:48.615 [clientInboundChannel-8] ERROR o.s.w.s.m.WebSocketAnnotationMethodMessageHandler - Unhandled exception from message handler method
org.springframework.dao.CannotAcquireLockException: could not execute statement [Deadlock found when trying to get lock; try restarting transaction] [update chat_room set created_at=?,created_by=?,description=?,dm_key=?,invite_code=?,is_private=?,last_message_at=?,last_message_id=?,last_message_preview=?,last_message_sender_id=?,max_person=?,name=?,participant_count=?,pinned_message_id=?,room_type=? where id=?]; SQL [update chat_room set created_at=?,created_by=?,description=?,dm_key=?,invite_code=?,is_private=?,last_message_at=?,last_message_id=?,last_message_preview=?,last_message_sender_id=?,max_person=?,name=?,participant_count=?,pinned_message_id=?,room_type=? where id=?]
	at org.springframework.orm.jpa.vendor.HibernateJpaDialect.convertHibernateAccessException(HibernateJpaDialect.java:283)
	at org.springframework.orm.jpa.vendor.HibernateJpaDialect.translateExceptionIfPossible(HibernateJpaDialect.java:244)
	at org.springframework.orm.jpa.AbstractEntityManagerFactoryBean.translateExceptionIfPossible(AbstractEntityManagerFactoryBean.java:560)
	at org.springframework.dao.support.ChainedPersistenceExceptionTranslator.translateExceptionIfPossible(ChainedPersistenceExceptionTranslator.java:61)
	at org.springframework.dao.support.DataAccessUtils.translateIfNecessary(DataAccessUtils.java:343)
	at org.springframework.dao.support.PersistenceExceptionTranslationInterceptor.invoke(PersistenceExceptionTranslationInterceptor.java:160)
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:184)
	at org.springframework.data.jpa.repository.support.CrudMethodMetadataPostProcessor$CrudMethodMetadataPopulatingMethodInterceptor.invoke(CrudMethodMetadataPostProcessor.java:136)
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:184)
	at org.springframework.aop.framework.JdkDynamicAopProxy.invoke(JdkDynamicAopProxy.java:223)
	at jdk.proxy4/jdk.proxy4.$Proxy183.increaseUnreadCount(Unknown Source)
	at com.chat_server.chatlist.service.impl.ChatListServiceImpl.increaseUnreadCount(ChatListServiceImpl.java:129)
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at org.springframework.aop.support.AopUtils.invokeJoinpointUsingReflection(AopUtils.java:359)
	at org.springframework.aop.framework.ReflectiveMethodInvocation.invokeJoinpoint(ReflectiveMethodInvocation.java:196)
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:163)
	at org.springframework.transaction.interceptor.TransactionAspectSupport.invokeWithinTransaction(TransactionAspectSupport.java:380)
	at org.springframework.transaction.interceptor.TransactionInterceptor.invoke(TransactionInterceptor.java:119)
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:184)
	at org.springframework.aop.framework.CglibAopProxy$DynamicAdvisedInterceptor.intercept(CglibAopProxy.java:727)
	at com.chat_server.chatlist.service.impl.ChatListServiceImpl$$SpringCGLIB$$0.increaseUnreadCount(<generated>)
	at com.chat_server.chatmessage.service.impl.ChatMessageFacadeServiceImpl.sendMessage(ChatMessageFacadeServiceImpl.java:96)
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at org.springframework.aop.support.AopUtils.invokeJoinpointUsingReflection(AopUtils.java:359)
	at org.springframework.aop.framework.ReflectiveMethodInvocation.invokeJoinpoint(ReflectiveMethodInvocation.java:196)
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:163)
	at org.springframework.transaction.interceptor.TransactionAspectSupport.invokeWithinTransaction(TransactionAspectSupport.java:380)
	at org.springframework.transaction.interceptor.TransactionInterceptor.invoke(TransactionInterceptor.java:119)
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:184)
	at org.springframework.aop.framework.CglibAopProxy$DynamicAdvisedInterceptor.intercept(CglibAopProxy.java:727)
	at com.chat_server.chatmessage.service.impl.ChatMessageFacadeServiceImpl$$SpringCGLIB$$0.sendMessage(<generated>)
	at com.chat_server.chatmessage.controller.ChatMessageWsController.sendMessage(ChatMessageWsController.java:36)
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at org.springframework.messaging.handler.invocation.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod.java:169)
	at org.springframework.messaging.handler.invocation.InvocableHandlerMethod.invoke(InvocableHandlerMethod.java:119)
	at org.springframework.messaging.handler.invocation.AbstractMethodMessageHandler.handleMatch(AbstractMethodMessageHandler.java:568)
	at org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler.handleMatch(SimpAnnotationMethodMessageHandler.java:530)
	at org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler.handleMatch(SimpAnnotationMethodMessageHandler.java:93)
	at org.springframework.messaging.handler.invocation.AbstractMethodMessageHandler.handleMessageInternal(AbstractMethodMessageHandler.java:522)
	at org.springframework.messaging.handler.invocation.AbstractMethodMessageHandler.handleMessage(AbstractMethodMessageHandler.java:457)
	at org.springframework.messaging.support.ExecutorSubscribableChannel$SendTask.run(ExecutorSubscribableChannel.java:152)
	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1144)
	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:642)
	at java.base/java.lang.Thread.run(Thread.java:1583)
Caused by: org.hibernate.exception.LockAcquisitionException: could not execute statement [Deadlock found when trying to get lock; try restarting transaction] [update chat_room set created_at=?,created_by=?,description=?,dm_key=?,invite_code=?,is_private=?,last_message_at=?,last_message_id=?,last_message_preview=?,last_message_sender_id=?,max_person=?,name=?,participant_count=?,pinned_message_id=?,room_type=? where id=?]
	at org.hibernate.dialect.MySQLDialect.lambda$buildSQLExceptionConversionDelegate$3(MySQLDialect.java:1260)
	at org.hibernate.exception.internal.StandardSQLExceptionConverter.convert(StandardSQLExceptionConverter.java:58)
	at org.hibernate.engine.jdbc.spi.SqlExceptionHelper.convert(SqlExceptionHelper.java:108)
	at org.hibernate.engine.jdbc.internal.ResultSetReturnImpl.executeUpdate(ResultSetReturnImpl.java:197)
	at org.hibernate.engine.jdbc.mutation.internal.AbstractMutationExecutor.performNonBatchedMutation(AbstractMutationExecutor.java:134)
	at org.hibernate.engine.jdbc.mutation.internal.MutationExecutorSingleNonBatched.performNonBatchedOperations(MutationExecutorSingleNonBatched.java:55)
	at org.hibernate.engine.jdbc.mutation.internal.AbstractMutationExecutor.execute(AbstractMutationExecutor.java:55)
	at org.hibernate.persister.entity.mutation.UpdateCoordinatorStandard.doStaticUpdate(UpdateCoordinatorStandard.java:781)
	at org.hibernate.persister.entity.mutation.UpdateCoordinatorStandard.performUpdate(UpdateCoordinatorStandard.java:328)
	at org.hibernate.persister.entity.mutation.UpdateCoordinatorStandard.update(UpdateCoordinatorStandard.java:245)
	at org.hibernate.action.internal.EntityUpdateAction.execute(EntityUpdateAction.java:169)
	at org.hibernate.engine.spi.ActionQueue.executeActions(ActionQueue.java:644)
	at org.hibernate.engine.spi.ActionQueue.executeActions(ActionQueue.java:511)
	at org.hibernate.event.internal.AbstractFlushingEventListener.performExecutions(AbstractFlushingEventListener.java:414)
	at org.hibernate.event.internal.DefaultFlushEventListener.onFlush(DefaultFlushEventListener.java:41)
	at org.hibernate.event.service.internal.EventListenerGroupImpl.fireEventOnEachListener(EventListenerGroupImpl.java:127)
	at org.hibernate.internal.SessionImpl.doFlush(SessionImpl.java:1429)
	at org.hibernate.internal.SessionImpl.flush(SessionImpl.java:1415)
	at org.hibernate.query.sql.internal.NativeQueryImpl.prepareForExecution(NativeQueryImpl.java:666)
	at org.hibernate.query.spi.AbstractSelectionQuery.beforeQuery(AbstractSelectionQuery.java:172)
	at org.hibernate.query.spi.AbstractSelectionQuery.beforeQueryHandlingFetchProfiles(AbstractSelectionQuery.java:159)
	at org.hibernate.query.spi.AbstractQuery.executeUpdate(AbstractQuery.java:648)
	at org.springframework.data.jpa.repository.query.JpaQueryExecution$ModifyingExecution.doExecute(JpaQueryExecution.java:267)
	at org.springframework.data.jpa.repository.query.JpaQueryExecution.execute(JpaQueryExecution.java:93)
	at org.springframework.data.jpa.repository.query.AbstractJpaQuery.doExecute(AbstractJpaQuery.java:152)
	at org.springframework.data.jpa.repository.query.AbstractJpaQuery.execute(AbstractJpaQuery.java:140)
	at org.springframework.data.repository.core.support.RepositoryMethodInvoker.doInvoke(RepositoryMethodInvoker.java:170)
	at org.springframework.data.repository.core.support.RepositoryMethodInvoker.invoke(RepositoryMethodInvoker.java:158)
	at org.springframework.data.repository.core.support.QueryExecutorMethodInterceptor.doInvoke(QueryExecutorMethodInterceptor.java:170)
	at org.springframework.data.repository.core.support.QueryExecutorMethodInterceptor.invoke(QueryExecutorMethodInterceptor.java:149)
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:184)
	at org.springframework.data.projection.DefaultMethodInvokingMethodInterceptor.invoke(DefaultMethodInvokingMethodInterceptor.java:69)
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:184)
	at org.springframework.transaction.interceptor.TransactionAspectSupport.invokeWithinTransaction(TransactionAspectSupport.java:380)
	at org.springframework.transaction.interceptor.TransactionInterceptor.invoke(TransactionInterceptor.java:119)
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:184)
	at org.springframework.dao.support.PersistenceExceptionTranslationInterceptor.invoke(PersistenceExceptionTranslationInterceptor.java:138)
	... 41 common frames omitted
Caused by: com.mysql.cj.jdbc.exceptions.MySQLTransactionRollbackException: Deadlock found when trying to get lock; try restarting transaction
	at com.mysql.cj.jdbc.exceptions.SQLError.createSQLException(SQLError.java:115)
	at com.mysql.cj.jdbc.exceptions.SQLExceptionsMapping.translateException(SQLExceptionsMapping.java:114)
	at com.mysql.cj.jdbc.ClientPreparedStatement.executeInternal(ClientPreparedStatement.java:988)
	at com.mysql.cj.jdbc.ClientPreparedStatement.executeUpdateInternal(ClientPreparedStatement.java:1166)
	at com.mysql.cj.jdbc.ClientPreparedStatement.executeUpdateInternal(ClientPreparedStatement.java:1101)
	at com.mysql.cj.jdbc.ClientPreparedStatement.executeLargeUpdate(ClientPreparedStatement.java:1467)
	at com.mysql.cj.jdbc.ClientPreparedStatement.executeUpdate(ClientPreparedStatement.java:1084)
	at com.zaxxer.hikari.pool.ProxyPreparedStatement.executeUpdate(ProxyPreparedStatement.java:61)
	at com.zaxxer.hikari.pool.HikariProxyPreparedStatement.executeUpdate(HikariProxyPreparedStatement.java)
	at org.hibernate.engine.jdbc.internal.ResultSetReturnImpl.executeUpdate(ResultSetReturnImpl.java:194)
	... 74 common frames omitted
```

#### **Profiler Flame Graph(Dead Lock)**
![dead-lock(profiler).PNG](docs%2Fimage%2Fdead-lock%28profiler%29.PNG)


2. 해결 방안 (Action)

- **"DB 직접 쓰기 구조에서 Redis 기반 Write-Back 아키텍처로 전환"**

- Redis를 Single Source of Truth(SSOT)로 활용: 빈번하게 변경되는 메타데이터(안읽음 개수, 최근 메시지 ID)의 실시간 업데이트를 DB가 아닌 Redis Hash와 Set 자료구조를 활용해 메모리 I/O로 대체함.

- 비동기 배치 스케줄러(Scheduler) 도입:
  - API 스레드는 Redis에만 데이터를 기록하고 응답을 종료하며, 백그라운드 스케줄러가 10초 주기로 Redis의 Dirty 데이터를 DB에 동기화(Eventual Consistency)하도록 구조 분리.

3. 결과 및 성과 (To-Be)

- 데드락 완벽 해소: 프로파일러(Profiler) 분석 결과, API 처리 스레드(clientInboundChannel)에서 발생하던 DB Lock 대기 병목 구간이 완전히 제거됨.

- 메시지 처리 성능(TPS) 검증:

  - 500명 전원 연결 완료 후 일제히 메시지를 발송하는 통제된 부하 테스트 스크립트 작성 및 검증.

  - DB 에러 및 메시지 유실률 0% 상태에서 평균 2,666 TPS (Inbound 기준) 달성.

  - 발신 메시지를 방 내부의 500명에게 브로드캐스트하는 Fan-out 트래픽 기준, 초당 약 133만 건의 아웃바운드 메시지를 지연 없이 안정적으로 처리함.


#### Profiler Flame Graph
![profiler-flame-graph(redis-batch).PNG](docs%2Fimage%2Fprofiler-flame-graph%28redis-batch%29.PNG)

#### Profiler Timeline
![profiler-timeline(redis-batch).PNG](docs%2Fimage%2Fprofiler-timeline%28redis-batch%29.PNG)


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