package com.chat_server.search.service;

import com.chat_server.search.dto.request.SearchUserRequest;
import com.chat_server.search.dto.response.SearchUserResponse;

import java.util.List;

public interface SearchService {
    List<SearchUserResponse> searchUserByUserId(SearchUserRequest request);
}
