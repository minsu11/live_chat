package com.chat_server.chatroomsetting.entity;

import com.chat_server.chatroom.entity.ChatRoom;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "chat_room_setting",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_chat_room_setting_room", columnNames = "chat_room_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_room_setting_room"))
    private ChatRoom chatRoom;

    @Column(name = "allow_file_upload", nullable = false)
    private boolean allowFileUpload;

    @Column(name = "allow_self_destruct_message", nullable = false)
    private boolean allowSelfDestructMessage;

    @Column(name = "allow_thread", nullable = false)
    private boolean allowThread;

    @Column(name = "message_edit_time_limit")
    private Integer messageEditTimeLimit;

    @Column(name = "message_delete_time_limit")
    private Integer messageDeleteTimeLimit;

    @Column(name = "default_notification_on", nullable = false)
    private boolean defaultNotificationOn;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}