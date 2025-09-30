package com.chat_server.userprofile.service;

import com.chat_server.userprofile.dto.response.UserMyProfileInfoResponse;
import com.chat_server.userprofile.dto.response.UserMyProfileResponse;

public interface UserProfileService {
    /**
     * 홈 화면에 보여줄 프로필 서비스 로직
     * @param userId
     * @return
     */
    UserMyProfileResponse getMyProfileSummary(Long userId);

    /**
     * 본인  프로필 상세 데이터 가지고옴
     * @param userId 유저 아이디
     * @return 상세 정보 DTO
     */
    UserMyProfileInfoResponse getMyProfileDetail(Long userId);
}
