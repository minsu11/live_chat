package com.chat_server.user.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.HashMap;

/**
 * packageName    : com.chat_server.user.dto.request
 * fileName       : UserRegisterRequest
 * author         : parkminsu
 * date           : 25. 2. 26.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 2. 26.        parkminsu       최초 생성
 */
public record UserRegisterRequest(@NotBlank
                                  @Size(min = 5,max = 20)
                                  String id,

                                  @NotBlank
                                  String password,

                                  @NotBlank
                                  @Size(min = 3,max = 20)
                                  String name,

                                  @NotBlank
                                  @Size(min = 2,max = 20)
                                  String nickName,

                                  @NotNull
                                  @Min(value = 0)
                                  @Max(value = 100)
                                  Integer age,

                                  @NotBlank
                                  String gender,

                                  String email,

                                  String  birth,
                                  String phoneNumber
                                  ) {
}
