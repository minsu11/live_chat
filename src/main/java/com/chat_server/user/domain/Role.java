package com.chat_server.user.domain;

public enum Role {
    유저("User"),
    관리자("Admin");

    private final String value;
    Role(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }

}
