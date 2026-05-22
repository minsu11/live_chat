package com.chat_server.friend.service.impl;

import com.chat_server.common.cursor.CursorCodec;
import com.chat_server.common.dto.exception.ConflictException;
import com.chat_server.common.dto.exception.ValidationException;
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

import java.time.LocalDateTime;
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
        if (limit <= 0 || limit > 200) {
            limit = 50;
        }

        CursorKey decoded = CursorCodec.decode(cursor);

        Slice<UserFriendResponse> slice = friendRepository.getFriendsWithProfileByCursor(userId, limit, decoded);
        String next = null;
        if (slice.hasNext() && !slice.getContent().isEmpty()) {
            UserFriendResponse last = slice.getContent().get(slice.getContent().size() - 1);
            next = CursorCodec.encode(last.nickName().toLowerCase(Locale.ROOT), last.uuid());
        }
        return new CursorPageResponse<>(slice.getContent(), next, slice.hasNext());
    }

    @Override
    public void saveFriend(UserFriendRegisterRequest registerRequest, Long userId) {
        String friendId = registerRequest.friendId();

        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        // 친구의 uuid로 찾음
        // todo 검색 방법에 대해서도 고민을 해봐야할듯
        User friend = userRepository.findByUuid(friendId)
                .orElseThrow(UserNotFoundException::new);

        if (user.getId().equals(friend.getId())) {
            throw new ValidationException("자기 자신은 친구로 추가할 수 없습니다.");
        }

        if (friendRepository.existsByUser_IdAndFriendUser_Id(user.getId(), friend.getId())) {
            throw new ConflictException("이미 친구로 추가된 사용자입니다.");
        }

        Friend registerFriend = Friend.builder()
                .user(user)
                .friendUser(friend)
                .createdAt(LocalDateTime.now())
                .build();

        friendRepository.save(registerFriend);
    }
}
