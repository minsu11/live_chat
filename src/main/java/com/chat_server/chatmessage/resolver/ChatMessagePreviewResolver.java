package com.chat_server.chatmessage.resolver;

import com.chat_server.chatmessage.enums.MessageType;


public final class ChatMessagePreviewResolver {
    private static final int PREVIEW_MAX_LENGTH = 20;
    public static String resolve(MessageType messageType, String messageContent) {
        String safeContent = messageContent == null ? "" : messageContent.trim();

        return switch (messageType) {
            case TEXT, EMOJI -> abbreviateOrFallback(safeContent);
            case IMAGE -> "사진 보냈습니다.";
            case FILE -> "파일 보냈습니다.";
            case SYSTEM -> "시스템 알림입니다.";
            case SYSTEM_LEAVE, SYSTEM_INVITE -> null;
        };
    }

    private static String abbreviateOrFallback(String content) {
        if (content.isBlank()) {
            return "메시지를 보냈습니다.";
        }

        return content.length() <= PREVIEW_MAX_LENGTH
                ? content
                : content.substring(0, PREVIEW_MAX_LENGTH) + "...";
    }
    private ChatMessagePreviewResolver() {}
}
