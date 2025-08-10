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
    // todo 추 후 정책이 생기면 별도로 User에 차단, 비활성 등등의 공통 서비스 만들어야함
    private final UserRepository userRepository;


    //todo null로 해야하는지 추후 고민
    @Override
    public SearchUserResponse searchUserByUserId(SearchUserRequest request) {
         return userRepository.getSearchUserByUserId(request.userId())
            .orElse(null);
    }
}
