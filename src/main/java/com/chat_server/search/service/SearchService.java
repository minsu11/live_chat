package com.chat_server.search.service;

import com.chat_server.search.dto.request.SearchUserRequest;
import com.chat_server.search.dto.response.SearchUserResponse;

public interface SearchService {
    SearchUserResponse searchUserByUserId(SearchUserRequest request);
}
