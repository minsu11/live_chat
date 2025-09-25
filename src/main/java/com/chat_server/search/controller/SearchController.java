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

import java.util.List;

// 추 후 메시지 검색 등 검색 관련 기능 controller 역할
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class SearchController {
    private final SearchService searchService;

    // 아이디로 검색하기 떄문에 post 요청
    @PostMapping("/search/users")
    public ResponseEntity<ApiResponse<List<SearchUserResponse>>> searchUser(
        @RequestBody SearchUserRequest request
    ){
        log.info("search controller");
        log.info("id : {}", request.userId());
        List<SearchUserResponse> searchUserResponse = searchService.searchUserByUserId(request);
        log.info("search response: {}", searchUserResponse);
        ApiResponse<List<SearchUserResponse>> response = ApiResponse.success(200, "검색에 성공했습니다.",searchUserResponse);
        log.info("response : {}", response.getData());
        return ResponseEntity.ok(response);
    }

}
