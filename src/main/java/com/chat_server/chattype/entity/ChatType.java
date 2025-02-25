package com.chat_server.chattype.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * packageName    : com.chat_server.chattype.entity
 * fileName       : ChatType
 * author         : parkminsu
 * date           : 25. 2. 25.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 2. 25.        parkminsu       최초 생성
 */
@Entity
@Table(name = "chat_type")
@Getter
@NoArgsConstructor
public class ChatType {
    @Id
    @Column(name = "chat_type_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "chat_type_name")
    private String chatTypeName;
}
