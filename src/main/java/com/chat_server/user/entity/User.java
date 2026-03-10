package com.chat_server.user.entity;

import com.chat_server.gender.entity.Gender;
import com.chat_server.user.enums.UserStatus;
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
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String userName;

    @Column(name = "input_id")
    private String userInputId;

    @Column(name = "input_password")
    private String userInputPassword;

    @Column(name = "age")
    private Integer userAge;

    @Column(name = "nickname")
    private String userNickname;

    @Column(name = "created_at")
    private LocalDateTime userCreatedAt;

    @Column(name = "uuid")
    private String userUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gender_id")
    private Gender gender;

    public void updateNickname(String userNickname){
        this.userNickname = userNickname;
    }
}
