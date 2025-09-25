package com.chat_server.chatroomsetting.entity;

import com.chat_server.chatroom.entity.ChatRoom;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_room_setting")
@Getter
@NoArgsConstructor
public class ChatRoomSetting {
    @Id
    @Column(name = "chat_room_setting_id")
    private Long id;

    @Column(name = "allow_file_upload")
    private boolean allowFileUpload;

    @Column(name = "allow_self_destruct_message")
    private boolean allowSelfDestructMessage;

    @Column(name = "allow_thread")
    private boolean allowThread;

    @Column(name = "message_edit_time_limit")
    private int messageEditTimeLimit;

    @Column(name = "message_delete_time_limit")
    private int messageDeleteTimeLimit;

    @Column(name = "default_notification_on")
    private boolean defaultNotificationOn;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;
}
