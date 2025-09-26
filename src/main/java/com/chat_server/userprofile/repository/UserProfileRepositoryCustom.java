package com.chat_server.userprofile.repository;

import com.chat_server.userprofile.dto.response.UserMyProfileResponse;
import java.util.Optional;

import com.chat_server.userprofile.dto.response.UserProfileDetailResponse;
import org.springframework.data.repository.NoRepositoryBean;

public interface UserProfileRepositoryCustom {
    Optional<UserMyProfileResponse> findMyProfile(Long id);
    Optional<UserProfileDetailResponse> findProfileDetail(Long id);

}
