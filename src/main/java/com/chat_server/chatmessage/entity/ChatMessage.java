package com.chat_server.chatmessage.entity;

import com.chat_server.chatmessage.enums.MessageType;
import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
@Getter
@Entity
@Table(
        name = "chat_message",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_chat_message_client", columnNames = "client_message_id")
        }
)

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false, foreignKey = @ForeignKey(name = "fk_chat_message_room"))
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false, foreignKey = @ForeignKey(name = "fk_chat_message_sender"))
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private MessageType messageType;

    @Column(name = "message_content")
    private String messageContent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_message_id", foreignKey = @ForeignKey(name = "fk_chat_message_parent"))
    private ChatMessage parentMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "client_message_id", length = 100)
    private String clientMessageId;

    public static ChatMessage create(
            ChatRoom chatRoom,
            User user,
            String message,
            MessageType messageType,
            LocalDateTime createdAt
    ) {
        return create(chatRoom, user, message, messageType, createdAt, null);
    }
    /**
     * 클라이언트 메시지 식별자를 포함해 채팅 메시지를 생성한다.
     *
     * <p>clientMessageId는 필수값이 아니다.
     * 기존 프론트에서는 사용하지 않을 수 있고, 부하 테스트/재전송/idempotency가 필요한 경우에만 사용한다.</p>
     */
    public static ChatMessage create(
            ChatRoom chatRoom,
            User user,
            String message,
            MessageType messageType,
            LocalDateTime createdAt,
            String clientMessageId
    ) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.chatRoom = chatRoom;
        chatMessage.sender = user;
        chatMessage.messageType = messageType;
        chatMessage.messageContent = message;
        chatMessage.deleted = false;
        chatMessage.createdAt = createdAt;
        chatMessage.clientMessageId = normalizeClientMessageId(clientMessageId);
        return chatMessage;
    }

    public static ChatMessage create(ChatRoom chatRoom, User user, String message, String messageType) {
        return create(chatRoom, user, message, messageType, null);
    }

    public static ChatMessage create(
            ChatRoom chatRoom,
            User user,
            String message,
            String messageType,
            String clientMessageId
    ) {
        MessageType type = null;
        log.info("message type: {}", messageType);

        if (messageType.equalsIgnoreCase("text")) {
            type = MessageType.TEXT;
        } else if (messageType.equalsIgnoreCase("image")) {
            type = MessageType.IMAGE;
        } else if (messageType.equalsIgnoreCase("system")) {
            type = MessageType.SYSTEM;
        } else if (messageType.equalsIgnoreCase("emoji")) {
            type = MessageType.EMOJI;
        } else if (messageType.equalsIgnoreCase("SYSTEM_LEAVE")) {
            type = MessageType.SYSTEM_LEAVE;
        } else if (messageType.equalsIgnoreCase("SYSTEM_INVITE")) {
            type = MessageType.SYSTEM_INVITE;
        } else {
            type = MessageType.FILE;
        }

        return create(chatRoom, user, message, type, LocalDateTime.now(), clientMessageId);
    }

    private static String normalizeClientMessageId(String clientMessageId) {
        if (clientMessageId == null || clientMessageId.isBlank()) {
            return null;
        }
        return clientMessageId.trim();
    }
}
