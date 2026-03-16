package com.chat_server.userprofile.enrtity;

import com.chat_server.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "user_profile",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_profile_user", columnNames = "user_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_profile_user"))
    private User user;

    @Column(name = "state_message", length = 60)
    private String stateMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void update(String stateMessage){
        this.stateMessage = stateMessage;
        this.updatedAt = LocalDateTime.now();
    }
}