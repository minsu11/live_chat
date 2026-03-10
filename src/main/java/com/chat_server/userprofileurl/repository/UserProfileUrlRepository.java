package com.chat_server.userprofileurl.repository;

import com.chat_server.userprofileurl.entity.UserProfileUrl;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileUrlRepository extends JpaRepository<UserProfileUrl, Long>,
    CustomUserProfileUrlRepository {

}
