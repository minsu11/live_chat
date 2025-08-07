package com.chat_server.chatmessage.entity;

import com.chat_server.user.entity.User;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "chat_message")
@NoArgsConstructor
public class ChatMessage {

    @Id
    @Column(name="chat_message_id")
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
    @JoinColumn(name="sender_id")
    private User user;

}
