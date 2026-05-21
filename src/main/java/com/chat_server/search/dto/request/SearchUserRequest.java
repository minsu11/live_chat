package com.chat_server.search.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SearchUserRequest(
        @JsonAlias({"userId", "friendCode"})
        @NotBlank(message = "검색어는 필수입니다.")
        @Size(min = 1, max = 30, message = "검색어는 1자 이상 30자 이하로 입력해주세요.")
        String keyword
) {
}