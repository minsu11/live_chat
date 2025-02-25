package com.chat_server.user.entity;

import com.chat_server.gender.entity.Gender;
import com.chat_server.userstatus.UserStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Date;

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
@Setter
@Entity
@Table(name = "user")
@NoArgsConstructor
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

    @Column(name="user_age")
    private Integer userAge;

    @Column(name="user_nickname")
    private String userNickname;

    @Column(name = "user_created_at")
    private Date userCreatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gender_id")
    private Gender gender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_status_id")
    private UserStatus userStatus;
}
