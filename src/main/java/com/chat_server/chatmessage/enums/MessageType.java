package com.chat_server.chatmessage.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

public enum MessageType {
    TEXT,
    EMOJI,
    IMAGE,
    FILE,
    SYSTEM;

    @JsonCreator
    public static MessageType from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return MessageType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("지원하지 않는 messageType: " + value);
        }
    }
}
