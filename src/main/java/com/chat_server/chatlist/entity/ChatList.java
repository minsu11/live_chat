package com.chat_server.chatlist.entity;

import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
@Getter
@Entity
@Table(
        name = "chat_list",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_chat_list_user_room", columnNames = {"user_id", "chat_room_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_chat_list_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false, foreignKey = @ForeignKey(name = "fk_chat_list_room"))
    private ChatRoom chatRoom;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @Column(name = "unread_count", nullable = false)
    private int unreadCount;

    @Column(name = "pinned", nullable = false)
    private boolean pinned;

    @Column(name = "muted", nullable = false)
    private boolean muted;

    @Column(name = "archived", nullable = false)
    private boolean archived;

    @Column(name = "custom_name", length = 50)
    private String customName;

    @Column(name = "last_opened_at")
    private LocalDateTime lastOpenedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void updateCustomName(String customName) {
        if (customName != null && customName.trim().isEmpty()) {
            this.customName = null;
        }else{
            log.info("이름 변경");
            this.customName = customName;
        }
    }
}