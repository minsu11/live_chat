package com.chat_server.user.entity;

import com.chat_server.gender.entity.Gender;
import com.chat_server.user.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "user",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_input_id", columnNames = "input_id"),
                @UniqueConstraint(name = "uk_user_uuid", columnNames = "uuid")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "gender_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_gender")
    )
    private Gender gender;

    @Column(name = "input_id", nullable = false, length = 30)
    private String inputId;

    @Column(name = "input_password", nullable = false, length = 100)
    private String inputPassword;

    @Column(name = "name", nullable = false, length = 30)
    private String name;

    @Column(name = "nickname", nullable = false, length = 30)
    private String nickname;

    @Column(name = "uuid", nullable = false, length = 36)
    private String uuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "login_lasted_at")
    private LocalDateTime loginLastedAt;
}