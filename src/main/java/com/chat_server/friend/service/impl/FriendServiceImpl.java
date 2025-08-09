package com.chat_server.friend.service.impl;

import com.chat_server.common.cursor.CursorCodec;
import com.chat_server.friend.dto.request.UserFriendRegisterRequest;
import com.chat_server.friend.dto.response.CursorPageResponse;
import com.chat_server.common.cursor.CursorKey;
import com.chat_server.friend.dto.response.UserFriendResponse;
import com.chat_server.friend.entity.Friend;
import com.chat_server.friend.repository.FriendRepository;
import com.chat_server.friend.service.FriendService;
import com.chat_server.user.entity.User;
import com.chat_server.user.exception.UserNotFoundException;
import com.chat_server.user.repository.UserRepository;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {
    private final FriendRepository friendRepository;
    private final UserRepository userRepository;

    // 없으면 빈 리스트를 가지고 옴
    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<UserFriendResponse> getFriendsByCursor(Long userId, int limit, @Nullable String cursor) {
        if (limit <= 0 || limit > 200) limit = 50; // 가드레일

        var decoded = CursorCodec.decode(cursor);
        var slice = friendRepository.findFriendsWithProfileByCursor(userId, limit, decoded);

        String next = null;
        if (slice.hasNext() && !slice.getContent().isEmpty()) {
            var last = slice.getContent().get(slice.getContent().size() - 1);

            next = CursorCodec.encode(last.friendName().toLowerCase(Locale.ROOT), last.friendId());
        }

        return new CursorPageResponse<>(slice.getContent(), next, slice.hasNext());
    }

    @Override
    public void saveFriend(UserFriendRegisterRequest registerRequest, Long userId) {

        String friendId = registerRequest.friendId();

        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        User friend = userRepository.findByUserInputId(friendId)
                .orElseThrow(UserNotFoundException::new);

        Friend registerFriend = Friend.builder()
                .user(user)
                .friend(friend)
                .isBlocked(false)
                .createdAt(LocalDateTime.now())
                .build();

        friendRepository.save(registerFriend);
    }
}
