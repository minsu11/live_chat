package com.chat_server.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record InputIdCheckRequest(

        @NotBlank(message = "아이디는 필수입니다.")
        @Size(min = 4, max = 30, message = "아이디는 4자 이상 30자 이하로 입력해주세요.")
        @Pattern(
                regexp = "^[a-zA-Z0-9_]+$",
                message = "아이디는 영문, 숫자, 언더스코어만 사용할 수 있습니다."
        )
        String inputId
) {
}
