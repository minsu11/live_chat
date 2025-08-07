package com.chat_server.user.domain;

/**
 * packageName    : com.chat_server.user.domain
 * fileName       : UserType
 * author         : parkminsu
 * date           : 25. 5. 26.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 5. 26.        parkminsu       최초 생성
 */

public enum UserType {
    USER("일반 사용자"),
    ADMIN("관리자");

    private final String description;

    UserType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

