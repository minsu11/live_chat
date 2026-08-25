package com.chat_server.integration.mysql;

import com.chat_server.chatmessage.repository.ChatMessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.chat_server.chatmessage.entity.ChatMessage;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.chat_server.chatmessage.dto.response.ChatMessageItemResponse;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@TestPropertySource(properties = {
        "auth.server.url=http://localhost:9090",
        "auth.api.address=/api/v1/auth"
})
class MySqlContainerIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8.0")
                    .withDatabaseName("chatalk_test")
                    .withUsername("test")
                    .withPassword("test")
                    .withInitScript("script/ddl.sql");


    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ChatMessageRepository chatMessageRepository;


    private void insertQuerydslTestData() {

        jdbcTemplate.update("""
            INSERT INTO login_type (
                id,
                name
            )
            VALUES (
                1,
                'NORMAL'
            )
            """);

        jdbcTemplate.update("""
            INSERT INTO user (
                id,
                login_type_id,
                input_id,
                name,
                nickname,
                friend_code,
                uuid,
                status
            )
            VALUES (
                1,
                1,
                'mysql_test_user',
                'Test User',
                'mysql-user',
                'CTK-TEST0001',
                '00000000-0000-0000-0000-000000000001',
                'ACTIVE'
            )
            """);

        jdbcTemplate.update("""
        INSERT INTO chat_room (
            id,
            room_type,
            name,
            created_by,
            created_at,
            last_message_at,
            participant_count
        )
        VALUES (
            10,
            'GROUP',
            'MySQL QueryDSL Test Room',
            1,
            '2026-08-24 19:59:00',
            NULL,
            1
        )
        """);


        jdbcTemplate.update("""
            INSERT INTO chat_message (
                id,
                chat_room_id,
                sender_id,
                message_type,
                message_content,
                created_at,
                is_deleted,
                client_message_id
            )
            VALUES (
                100,
                10,
                1,
                'TEXT',
                'first-message',
                '2026-08-24 20:00:00',
                0,
                'mysql-querydsl-100'
            )
            """);

        jdbcTemplate.update("""
            INSERT INTO chat_message (
                id,
                chat_room_id,
                sender_id,
                message_type,
                message_content,
                created_at,
                is_deleted,
                client_message_id
            )
            VALUES (
                101,
                10,
                1,
                'TEXT',
                'second-message',
                '2026-08-24 20:01:00',
                0,
                'mysql-querydsl-101'
            )
            """);

        jdbcTemplate.update("""
            INSERT INTO chat_message (
                id,
                chat_room_id,
                sender_id,
                message_type,
                message_content,
                created_at,
                is_deleted,
                client_message_id
            )
            VALUES (
                102,
                10,
                1,
                'TEXT',
                'third-message',
                '2026-08-24 20:02:00',
                0,
                'mysql-querydsl-102'
            )
            """);
    }

    private void insertFullTextTestData() {

        jdbcTemplate.update("""
            INSERT INTO login_type (
                id,
                name
            )
            VALUES (
                9101,
                'FTS_TEST'
            )
            """);

        jdbcTemplate.update("""
            INSERT INTO user (
                id,
                login_type_id,
                input_id,
                name,
                nickname,
                friend_code,
                uuid,
                status
            )
            VALUES (
                9101,
                9101,
                'fts_test_user',
                'FTS Test User',
                'fts-user',
                'CTK-FTS00001',
                '00000000-0000-0000-0000-000000009101',
                'ACTIVE'
            )
            """);

        jdbcTemplate.update("""
            INSERT INTO chat_room (
                id,
                room_type,
                name,
                created_by,
                created_at,
                last_message_at,
                participant_count
            )
            VALUES (
                9101,
                'GROUP',
                'FullText Search Test Room',
                9101,
                '2026-08-24 20:00:00',
                NULL,
                1
            )
            """);

        // FULLTEXT로 검색되어야 하는 TEXT 메시지
        jdbcTemplate.update("""
            INSERT INTO chat_message (
                id,
                chat_room_id,
                sender_id,
                message_type,
                message_content,
                created_at,
                is_deleted,
                client_message_id
            )
            VALUES (
                9201,
                9101,
                9101,
                'TEXT',
                'spring websocket integration',
                '2026-08-24 20:01:00',
                0,
                'fts-test-9201'
            )
            """);

        // spring은 들어있지만 IMAGE이므로 검색 결과에서 제외되어야 함
        jdbcTemplate.update("""
            INSERT INTO chat_message (
                id,
                chat_room_id,
                sender_id,
                message_type,
                message_content,
                created_at,
                is_deleted,
                client_message_id
            )
            VALUES (
                9202,
                9101,
                9101,
                'IMAGE',
                'spring websocket image',
                '2026-08-24 20:02:00',
                0,
                'fts-test-9202'
            )
            """);

        // 검색어와 관계없는 TEXT 메시지
        jdbcTemplate.update("""
            INSERT INTO chat_message (
                id,
                chat_room_id,
                sender_id,
                message_type,
                message_content,
                created_at,
                is_deleted,
                client_message_id
            )
            VALUES (
                9203,
                9101,
                9101,
                'TEXT',
                'redis write back metadata',
                '2026-08-24 20:03:00',
                0,
                'fts-test-9203'
            )
            """);
    }

    private void cleanupFullTextTestData() {

        jdbcTemplate.update("""
            DELETE FROM chat_message
            WHERE chat_room_id = 9101
            """);

        jdbcTemplate.update("""
            DELETE FROM chat_room
            WHERE id = 9101
            """);

        jdbcTemplate.update("""
            DELETE FROM user
            WHERE id = 9101
            """);

        jdbcTemplate.update("""
            DELETE FROM login_type
            WHERE id = 9101
            """);
    }


    @Test
    @DisplayName(
            "Testcontainers MySQL - 실제 MySQL 컨테이너에 Chatalk DDL이 적용된다"
    )
    void shouldStartMysqlAndApplySchema() {

        Integer count =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_schema = DATABASE()
                          AND table_name = 'chat_message'
                        """,
                        Integer.class
                );

        assertThat(count)
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Testcontainers MySQL - QueryDSL로 커서 이후 메시지를 실제 MySQL에서 조회한다"
    )
    void shouldGetMessagesAfterUsingQuerydslOnRealMysql() {

        // given
        insertQuerydslTestData();

        Long roomId = 10L;
        Long afterMessageId = 100L;


        // when
        Slice<ChatMessageItemResponse> result =
                chatMessageRepository.getMessagesAfter(
                        roomId,
                        afterMessageId,
                        10
                );


        List<ChatMessageItemResponse> messages =
                result.getContent();


        // then
        assertThat(messages)
                .hasSize(2);

        assertThat(messages)
                .extracting(
                        ChatMessageItemResponse::messageId
                )
                .containsExactly(
                        101L,
                        102L
                );

        assertThat(messages)
                .extracting(
                        ChatMessageItemResponse::content
                )
                .containsExactly(
                        "second-message",
                        "third-message"
                );

        assertThat(messages.get(0).senderId())
                .isEqualTo(1L);

        assertThat(messages.get(0).senderUuid())
                .isEqualTo(
                        "00000000-0000-0000-0000-000000000001"
                );

        assertThat(messages.get(0).senderNickname())
                .isEqualTo(
                        "mysql-user"
                );

        assertThat(result.hasNext())
                .isFalse();

        LocalDateTime orderAt =
                jdbcTemplate.queryForObject(
                        """
                        SELECT order_at
                        FROM chat_room
                        WHERE id = 10
                        """,
                        LocalDateTime.class
                );

        assertThat(orderAt)
                .isEqualTo(
                        LocalDateTime.of(
                                2026, 8, 24,
                                19, 59, 0
                        )
                );
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName(
            "Testcontainers MySQL - MATCH AGAINST BOOLEAN MODE로 메시지를 검색한다"
    )
    void shouldSearchMessageUsingMysqlFullTextSearch() {

        cleanupFullTextTestData();

        try {

            // given
            insertFullTextTestData();


            // when
            List<ChatMessage> result =
                    chatMessageRepository.searchMessagesByKeyword(
                            9101L,

                            // LIKE에는 절대로 매칭되지 않는 값
                            "__NO_LIKE_MATCH__",

                            // FULLTEXT MATCH에서만 spring 검색
                            "+spring*",

                            null,
                            null,
                            10
                    );


            // then
            assertThat(result)
                    .hasSize(1);

            assertThat(result)
                    .extracting(ChatMessage::getId)
                    .containsExactly(9201L);

            assertThat(
                    result.get(0).getMessageContent()
            ).isEqualTo(
                    "spring websocket integration"
            );

        } finally {

            cleanupFullTextTestData();
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName(
            "Testcontainers MySQL - FULLTEXT가 매칭되지 않으면 LIKE fallback으로 검색한다"
    )
    void shouldSearchMessageUsingLikeFallback() {

        cleanupFullTextTestData();

        try {

            // given
            insertFullTextTestData();


            // when
            List<ChatMessage> result =
                    chatMessageRepository.searchMessagesByKeyword(
                            9101L,

                            // 실제 메시지:
                            // "spring websocket integration"
                            //
                            // LIKE '%websock%' 으로 매칭
                            "websock",

                            // FULLTEXT에서는 절대 매칭되지 않게 설정
                            "+definitelynotfound*",

                            null,
                            null,
                            10
                    );


            // then
            assertThat(result)
                    .hasSize(1);

            assertThat(result)
                    .extracting(ChatMessage::getId)
                    .containsExactly(9201L);

            assertThat(
                    result.get(0).getMessageContent()
            ).isEqualTo(
                    "spring websocket integration"
            );

        } finally {

            cleanupFullTextTestData();
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName(
            "Testcontainers MySQL - 검색 cursor는 createdAt과 messageId 기준으로 다음 페이지를 조회한다"
    )
    void shouldSearchMessagesUsingCursor() {

        cleanupFullTextTestData();

        try {

            // given
            insertFullTextTestData();


            // 같은 createdAt을 가진 검색 대상 메시지 2개 추가
            jdbcTemplate.update("""
                INSERT INTO chat_message (
                    id,
                    chat_room_id,
                    sender_id,
                    message_type,
                    message_content,
                    created_at,
                    is_deleted,
                    client_message_id
                )
                VALUES (
                    9204,
                    9101,
                    9101,
                    'TEXT',
                    'spring cursor first',
                    '2026-08-24 20:04:00',
                    0,
                    'fts-test-9204'
                )
                """);

            jdbcTemplate.update("""
                INSERT INTO chat_message (
                    id,
                    chat_room_id,
                    sender_id,
                    message_type,
                    message_content,
                    created_at,
                    is_deleted,
                    client_message_id
                )
                VALUES (
                    9205,
                    9101,
                    9101,
                    'TEXT',
                    'spring cursor second',
                    '2026-08-24 20:04:00',
                    0,
                    'fts-test-9205'
                )
                """);


            /*
             * 현재 검색 대상:
             *
             * 9205 - 20:04 spring
             * 9204 - 20:04 spring
             * 9201 - 20:01 spring
             *
             * 정렬:
             * created_at DESC, id DESC
             */


            // when - 첫 페이지
            List<ChatMessage> firstPage =
                    chatMessageRepository.searchMessagesByKeyword(
                            9101L,
                            "__NO_LIKE_MATCH__",
                            "+spring*",
                            null,
                            null,
                            1
                    );


            // then
            assertThat(firstPage)
                    .extracting(ChatMessage::getId)
                    .containsExactly(9205L);


            ChatMessage cursorMessage =
                    firstPage.get(0);


            // when - cursor 이후 페이지
            List<ChatMessage> secondPage =
                    chatMessageRepository.searchMessagesByKeyword(
                            9101L,
                            "__NO_LIKE_MATCH__",
                            "+spring*",
                            cursorMessage.getCreatedAt(),
                            cursorMessage.getId(),
                            10
                    );


            // then
            assertThat(secondPage)
                    .extracting(ChatMessage::getId)
                    .containsExactly(
                            9204L,
                            9201L
                    );

        } finally {

            cleanupFullTextTestData();
        }
    }

}