package com.chat_server.chatlist.entity;

import com.chat_server.chatauthor.entity.ChatAuthor;
import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.user.entity.User;
import jakarta.persistence.*;
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

    @EmbeddedId
    private Pk pk;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="chat_author_id")
    private ChatAuthor chatAuthor;

    @Getter
    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    class Pk implements Serializable {
        @Column(name = "user_id")

        private Long userId;

        @Column(name = "chat_list_id")
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long chatId;

    }


    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

}
