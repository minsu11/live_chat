package com.chat_server.chatroom.entity;


import com.chat_server.chatroom.enums.RoomType;
import com.chat_server.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "chat_room",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_chat_room_dm_key", columnNames = "dm_key")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false, length = 20)
    private RoomType roomType;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "max_person")
    private Integer maxPerson;

    @Column(name = "is_private", nullable = false)
    private boolean isPrivate;

    @Column(name = "invite_code", length = 50)
    private String inviteCode;

    @Column(name = "dm_key", length = 50)
    private String dmKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, foreignKey = @ForeignKey(name = "fk_chat_room_created_by"))
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_message_id")
    private Long lastMessageId;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "last_message_preview", length = 120)
    private String lastMessagePreview;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_message_sender_id", foreignKey = @ForeignKey(name = "fk_chat_room_last_sender"))
    private User lastMessageSender;

    @Column(name = "pinned_message_id")
    private Long pinnedMessageId;

    @Column(name = "order_at", insertable = false, updatable = false)
    private LocalDateTime orderAt;

    public void updateLastMessageAt(Long senderId, Long messageId, String preview, LocalDateTime createdAt){

        this.lastMessageId = messageId;
        this.lastMessageAt = createdAt;
        this.lastMessagePreview = preview;

    }
}
