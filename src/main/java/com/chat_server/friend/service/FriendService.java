package com.chat_server.friend.service;

import com.chat_server.friend.dto.request.UserFriendRegisterRequest;
import com.chat_server.friend.dto.response.CursorPageResponse;
import com.chat_server.friend.dto.response.UserFriendResponse;
import jakarta.annotation.Nullable;

import java.util.List;

public interface FriendService {

    CursorPageResponse<UserFriendResponse> getFriendsByCursor(Long userId, int limit, @Nullable String cursor);

    void saveFriend(UserFriendRegisterRequest registerRequest, Long userId);
}
