package com.chat_server.search.controller;

import com.chat_server.common.dto.response.ApiResponse;
import com.chat_server.search.dto.request.SearchUserRequest;
import com.chat_server.search.dto.response.SearchUserResponse;
import com.chat_server.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 추 후 메시지 검색 등 검색 관련 기능 controller 역할
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class SearchController {
    private final SearchService searchService;

    @PostMapping("/user/search")
    public ResponseEntity<ApiResponse<SearchUserResponse>> searchUser(
        @RequestBody SearchUserRequest request
    ){
        SearchUserResponse searchUserResponse = searchService.searchUserByUserId(request);

        ApiResponse<SearchUserResponse> response = ApiResponse.success(200, "검색에 성공했습니다.",searchUserResponse);

        return ResponseEntity.ok(response);
    }

}
