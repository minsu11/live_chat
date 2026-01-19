package com.chat_server.chattype.enumulation;

public enum ChatRoomKind {
    DM("DM"),
    GROUP("GROUP");

    private final String code;

    ChatRoomKind(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ChatRoomKind getByCode(String code) {
        for(ChatRoomKind kind : values()) {
            if(kind.code.equals(code)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("No chat room kind found with code " + code);
    }

}
