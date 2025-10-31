package com.chat_server.chattype.enumulation;

public enum ChatType {
    개인(1L),
    그룹(2L);

    private Long value;

    ChatType(Long value) {
        this.value = value;
    }

    public Long getValue() {
        return value;
    }
}
