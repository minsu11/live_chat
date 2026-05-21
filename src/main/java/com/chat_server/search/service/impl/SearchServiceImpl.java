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

import java.util.Collections;
import java.util.List;
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
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

        if (compact.startsWith("CTK")) {
            String codeBody = compact.substring(3);
            return "CTK-" + codeBody;
        }

        return trimmed;
    }
}