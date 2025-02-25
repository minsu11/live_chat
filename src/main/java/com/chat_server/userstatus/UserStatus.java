package com.chat_server.userstatus;

import jakarta.persistence.*;
import lombok.Getter;

import java.util.Date;

/**
 * packageName    : com.chat_server.userstatus
 * fileName       : UserStatus
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
@Table(name = "user_status")
public class UserStatus{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_status_id")
    private int id;

    @Column(name = "user_status_name")
    private String userStatusName;

    @Column(name = "user_status_created_at")
    private Date userStatusCreatedAt;
}

