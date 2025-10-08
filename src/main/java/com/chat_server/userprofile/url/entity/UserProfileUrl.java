package com.chat_server.userprofile.url.entity;

import com.chat_server.user.entity.User;
import com.chat_server.userprofile.enrtity.UserProfile;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_profile_url")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileUrl {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_profile_url_id")
    private Long id;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "is_current")
    private Boolean isCurrent;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id")
    private UserProfile userProfile;

    public void updateIsCurrent(Boolean isCurrent) {
        this.isCurrent = isCurrent;
    }
}
