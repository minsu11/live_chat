package com.chat_server.chatroom.entity;

import com.chat_server.chattype.entity.ChatType;
import com.chat_server.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * packageName    : com.chat_server.chatroom.entity
 * fileName       : ChatRoom
 * author         : parkminsu
 * date           : 25. 2. 25.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 2. 25.        parkminsu       최초 생성
 */
@Getter
@Entity
@Table(name = "chat_room")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "max_person")
    private int maxPerson;

    @Column(name="name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "is_private")
    private boolean isPrivate;

    @Column(name = "last_message_id")
    private Long lastMessageId;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "last_message_preview", length = 120)
    private String lastMessagePreview;

    @Column(name ="participants_hash")
    private String participantsHash;

    @Column(name = "last_message_sender_id")
    private Long lastMessageSenderId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "order_at",insertable = false, updatable = false)
    private LocalDateTime orderAt;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_type_id")
    private ChatType chatType;
}
