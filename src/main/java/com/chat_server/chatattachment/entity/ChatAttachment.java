package com.chat_server.chatattachment.entity;

import com.chat_server.chatmessage.entity.ChatMessage;
import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "chat_attachment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_message_id",
            foreignKey = @ForeignKey(name = "fk_chat_attachment_message"))
    private ChatMessage chatMessage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "room_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_attachment_room")
    )
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "uploader_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_attachment_uploader")
    )
    private User uploader;

    @Column(name = "file_url", nullable = false, length = 255)
    private String fileUrl;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "stored_file_name", length = 255)
    private String storedFileName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public ChatAttachment(
            ChatMessage chatMessage,
            ChatRoom room,
            User uploader,
            String fileUrl,
            String originalFileName,
            String storedFileName,
            String contentType,
            Long fileSize,
            LocalDateTime createdAt
    ) {
        this.chatMessage = chatMessage;
        this.room = room;
        this.uploader = uploader;
        this.fileUrl = fileUrl;
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.createdAt = createdAt;
    }

    public void connectMessage(ChatMessage chatMessage) {
        this.chatMessage = chatMessage;
    }
}