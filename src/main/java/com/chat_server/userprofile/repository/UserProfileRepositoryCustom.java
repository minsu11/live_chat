package com.chat_server.userprofile.repository;

import com.chat_server.userprofile.dto.response.UserMyProfileResponse;
import java.util.Optional;

import com.chat_server.userprofile.dto.response.UserMyProfileInfoResponse;

public interface UserProfileRepositoryCustom {
    Optional<UserMyProfileResponse> findMyProfile(Long id);
    Optional<UserMyProfileInfoResponse> findProfileDetail(Long id);

}
