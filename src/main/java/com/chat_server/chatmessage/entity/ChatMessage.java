package com.chat_server.chatmessage.entity;

import com.chat_server.chatmessage.enums.MessageType;
import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    public static ChatMessage create(ChatRoom chatRoom, User user, String message, MessageType messageType, LocalDateTime createdAt) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.chatRoom = chatRoom;
        chatMessage.sender = user;
        chatMessage.messageType = messageType;
        chatMessage.messageContent = message;
        chatMessage.deleted = false;
        chatMessage.createdAt = createdAt;
        return chatMessage;
    }

    public static ChatMessage create(ChatRoom chatRoom, User user, String message, String messageType) {
        MessageType type = null;
        if(messageType.equalsIgnoreCase("text")) {
            type = MessageType.TEXT;
        }else if(messageType.equalsIgnoreCase("image")) {
            type = MessageType.IMAGE;
        }else if(messageType.equalsIgnoreCase("system")) {}
        else{
            type = MessageType.FILE;
        }
        return create(chatRoom, user, message, type, LocalDateTime.now());
    }


}
