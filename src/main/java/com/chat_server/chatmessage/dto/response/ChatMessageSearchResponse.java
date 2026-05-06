package com.chat_server.chatmessage.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ChatMessageSearchResponse(Long messageId,
                                        Long roomId,
                                        String messageType,
                                        Long senderId,
                                        String senderNickname,
                                        String profileImageUrl,
                                        String content,
                                        LocalDateTime createdAt) {
}
