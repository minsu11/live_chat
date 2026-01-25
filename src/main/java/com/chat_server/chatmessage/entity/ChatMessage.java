package com.chat_server.chatmessage.entity;

import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.user.entity.User;
import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "chat_message")
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="message_type")
    private String messageType;

    @Column(name = "message_content")
    private String messageContent;

    @Column(name="created_at")
    private LocalDateTime createdAt;

    @Column(name="is_deleted")
    private Boolean isDeleted;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="sender_id")
    private User user;

    public static ChatMessage create(ChatRoom chatRoom, User user, String message, String messageType, LocalDateTime createdAt) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.chatRoom = chatRoom;
        chatMessage.user = user;
        chatMessage.messageType = messageType;
        chatMessage.messageContent = message;
        chatMessage.isDeleted = false;
        chatMessage.createdAt = createdAt;
       return chatMessage;
    }

}
