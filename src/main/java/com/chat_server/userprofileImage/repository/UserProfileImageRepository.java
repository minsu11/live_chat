package com.chat_server.userprofileImage.repository;

import com.chat_server.userprofileImage.entity.UserProfileImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileImageRepository extends JpaRepository<UserProfileImage, Long>,
        CustomUserProfileImageRepository {

}
