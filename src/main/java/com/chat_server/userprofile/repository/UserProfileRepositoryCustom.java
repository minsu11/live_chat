package com.chat_server.userprofile.repository;

import com.chat_server.userprofile.dto.response.UserMyProfileSummaryResponse;
import java.util.Optional;

import com.chat_server.userprofile.dto.response.UserMyProfileDetailResponse;

public interface UserProfileRepositoryCustom {
    Optional<UserMyProfileSummaryResponse> findMyProfile(Long id);
    Optional<UserMyProfileDetailResponse> findProfileDetail(Long id);

}
