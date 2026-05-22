package com.chat_server.search.service.impl;

import com.chat_server.search.dto.request.SearchUserRequest;
import com.chat_server.search.dto.response.SearchUserResponse;
import com.chat_server.search.service.SearchService;
import com.chat_server.user.exception.UserNotFoundException;
import com.chat_server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private static final String FRIEND_CODE_PREFIX = "CTK-";
    private static final int FRIEND_CODE_BODY_LENGTH = 8;
    private static final String FRIEND_CODE_BODY_PATTERN = "^[A-Z0-9]{8}$";

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public SearchUserResponse searchUser(Long userId, SearchUserRequest request) {
        String keyword = normalizeSearchKeyword(request.keyword());

        SearchUserResponse response = userRepository.searchUserByFriendCodeOrInputId(userId, keyword);

        if (response == null) {
            throw new UserNotFoundException("사용자를 찾을 수 없습니다.");
        }

        return response;
    }

    private String normalizeSearchKeyword(String keyword) {
        if (keyword == null) {
            return "";
        }

        String trimmed = keyword.trim();

        if (trimmed.isBlank()) {
            return "";
        }

        String compact = trimmed
                .replace(" ", "")
                .replace("-", "")
                .toUpperCase();

        if (isFriendCodeLike(compact)) {
            return FRIEND_CODE_PREFIX + compact.substring(3);
        }

        return trimmed;
    }

    private boolean isFriendCodeLike(String compactKeyword) {
        if (compactKeyword == null) {
            return false;
        }

        if (!compactKeyword.startsWith("CTK")) {
            return false;
        }

        String body = compactKeyword.substring(3);

        return body.length() == FRIEND_CODE_BODY_LENGTH
                && body.matches(FRIEND_CODE_BODY_PATTERN);
    }
}