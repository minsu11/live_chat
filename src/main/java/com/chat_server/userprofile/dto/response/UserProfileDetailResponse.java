package com.chat_server.userprofile.dto.response;

// 프로필 모든 정보 가지고 오는 DTO
public record UserProfileDetailResponse(
        String uuid,
        String nickName,
        String friendCode,
        String message,
        String profileUrl,
        boolean friend,
        boolean me
) {
}
