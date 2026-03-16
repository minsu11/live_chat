package com.chat_server.friend.entity;

import com.chat_server.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "friend",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_friend", columnNames = {"user_id", "friend_user_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Friend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_friend_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "friend_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_friend_target"))
    private User friendUser;

    @Column(name = "custom_nickname", length = 30)
    private String customNickname;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}