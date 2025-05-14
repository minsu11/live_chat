package com.chat_server.user.entity;

import com.chat_server.gender.entity.Gender;
import com.chat_server.userstatus.entity.UserStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * packageName    : com.chat_server.user.entity
 * fileName       : User
 * author         : parkminsu
 * date           : 25. 2. 24.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 2. 24.        parkminsu       최초 생성
 */
@Getter
@Entity
@Table(name = "user")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @Column(name = "user_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "user_input_id")
    private String userInputId;

    @Column(name = "user_input_password")
    private String userInputPassword;

    @Column(name = "user_age")
    private Integer userAge;

    @Column(name = "user_nickname")
    private String userNickname;

    @Column(name = "user_created_at")
    private LocalDateTime userCreatedAt;

    @Column(name = "user_uuid")
    private String userUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gender_id")
    private Gender gender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_status_id")
    private UserStatus userStatus;
}
