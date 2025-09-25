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
import org.springframework.data.domain.Slice;
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
        log.info("friend service start");
        if (limit <= 0 || limit > 200) {
            log.info("limit 가드레일");
            limit = 50;
        }// 가드레일

        log.info("decoded before");
        CursorKey decoded = CursorCodec.decode(cursor);
        log.info("decoded after");
        log.info("repository before");
        Slice<UserFriendResponse> slice = friendRepository.getFriendsWithProfileByCursor(userId, limit, decoded);
        log.info("repository after");
        String next = null;
        log.info("next null ");
        if (slice.hasNext() && !slice.getContent().isEmpty()) {
            log.info("slice ");
            UserFriendResponse last = slice.getContent().get(slice.getContent().size() - 1);

            next = CursorCodec.encode(last.name().toLowerCase(Locale.ROOT), last.id());
            log.info("next: {}",next);
        }

        log.info("return ");
        return new CursorPageResponse<>(slice.getContent(), next, slice.hasNext());
    }

    @Override
    public void saveFriend(UserFriendRegisterRequest registerRequest, Long userId) {

        String friendId = registerRequest.friendId();
        log.info("friend id: {}", friendId);
        log.info("friend id: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        // 친구의 uuid로 찾음
        // todo 검색 방법에 대해서도 고민을 해봐야할듯
        User friend = userRepository.findByUserUuid(friendId)
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
