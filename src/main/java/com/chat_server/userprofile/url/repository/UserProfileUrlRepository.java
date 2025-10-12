package com.chat_server.userprofile.url.repository;

import com.chat_server.userprofile.url.entity.UserProfileUrl;
import com.chat_server.userprofile.url.service.UserProfileUrlService;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileUrlRepository extends JpaRepository<UserProfileUrl, Long>,
    CustomUserProfileUrlRepository {

}
