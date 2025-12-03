package com.chat_server.userprofile.dto.response;

/**
 * update response
 * @param nickName
 * @param message
 */
public record UserProfileUpdateResponse(String nickName, String message) {

}
