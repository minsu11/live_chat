package com.chat_server.userprofile.dto.response;

// 본인 프로필
public record UserMyProfileSummaryResponse(String uuid,
                                           String nickName,
                                           String message,
                                           String profileUrl) {

}
