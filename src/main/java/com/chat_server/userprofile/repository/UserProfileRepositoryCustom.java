package com.chat_server.userprofile.repository;

import com.chat_server.userprofile.dto.response.UserMyProfileSummaryResponse;
import java.util.Optional;

import com.chat_server.userprofile.dto.response.UserProfileDetailResponse;

public interface UserProfileRepositoryCustom {
    Optional<UserMyProfileSummaryResponse> findMyProfile(Long id);
    Optional<UserProfileDetailResponse> findProfileDetail(Long id);
    Optional<UserProfileDetailResponse> findProfileDetailByUuid(Long viewerId, String targetUserUuid);

}
