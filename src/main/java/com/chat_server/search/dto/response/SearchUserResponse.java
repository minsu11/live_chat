package com.chat_server.search.dto.response;

public record SearchUserResponse(String uuid,
                                 String name,
                                 String profileUrl,
                                 boolean isFriend
) {

}
