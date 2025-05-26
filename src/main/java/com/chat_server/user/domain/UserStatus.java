package com.chat_server.user.domain;

import lombok.Getter;

/**
 * packageName    : com.chat_server.user.domain
 * fileName       : UserSstatus
 * author         : parkminsu
 * date           : 25. 5. 26.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 5. 26.        parkminsu       최초 생성
 */
@Getter
public enum UserStatus {
    ACTIVE("활성 사용자"),
    DORMANT("휴면 사용자"),
    BLOCKED("차단된 사용자"),
    WITHDRAWN("탈퇴한 사용자");

    private final String description;

    UserStatus(String description) {
        this.description = description;
    }

}