package com.chat_server.chatlist.entity;

import com.chat_server.chatauthor.entity.ChatAuthor;
import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * packageName    : com.chat_server.chatroom.entity
 * fileName       : ChatList
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
@Table(name = "chat_list")
@NoArgsConstructor
public class ChatList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_list_id")
    private Long id;

    @Column(name = "unread_count")
    private int unreadCount = 0;

    @Column(name = "pinned")
    private boolean pinned = false;

    @Column(name = "muted")
    private boolean muted = false;

    @Column(name = "archived")
    private boolean archived = false;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @Column(name = "last_opened_at")
    private LocalDateTime lastOpenedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="chat_author_id")
    private ChatAuthor chatAuthor;
}

