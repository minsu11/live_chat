package com.chat_server.chatauthor.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * packageName    : com.chat_server.chatauthor.entity
 * fileName       : ChatAuthor
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
@NoArgsConstructor
@Table(name = "chat_author")
public class ChatAuthor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_author_id")
    private Integer id;

    @Column(name = "chat_author_name")
    private String name;

    @Column(name = "chat_author_created_at")
    private LocalDateTime createdAt;

}
