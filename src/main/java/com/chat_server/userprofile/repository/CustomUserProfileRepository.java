package com.chat_server.userprofile.repository;

import com.chat_server.userprofile.dto.response.UserMyProfileResponse;
import java.util.Optional;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface CustomUserProfileRepository {
    Optional<UserMyProfileResponse> findMyProfile(Long id);
}
