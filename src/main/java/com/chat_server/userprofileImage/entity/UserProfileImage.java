package com.chat_server.userprofileImage.entity;

import com.chat_server.userprofile.enrtity.UserProfile;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "user_profile_image")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfileImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_profile_image_profile"))
    private UserProfile userProfile;

    @Column(name = "image_url", nullable = false, length = 255)
    private String imageUrl;

    @Column(name = "is_current", nullable = false)
    private boolean current;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;
}