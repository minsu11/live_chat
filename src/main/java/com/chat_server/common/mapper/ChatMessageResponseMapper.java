package com.chat_server.common.mapper;

import com.chat_server.chatmessage.dto.response.ChatMessageItemResponse;
import com.chat_server.chatmessage.dto.response.ChatMessageResponse;
import com.chat_server.chatmessage.dto.response.ChatMessageSenderResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ChatMessageResponseMapper {
    public ChatMessageResponse fromItem(
            ChatMessageItemResponse item,
            Long roomId,
            Long viewerUserId
    ) {
        return new ChatMessageResponse(
                item.messageId(),
                roomId,
                item.messageType(),
                new ChatMessageSenderResponse(
                        item.senderUuid(),
                        item.senderNickname(),
                        item.profileImageUrl(),
                        item.senderId().equals(viewerUserId)
                ),
                item.content(),
                item.createdAt()
        );
    }

    public ChatMessageResponse fromMessage(
            Long messageId,
            Long roomId,
            String messageType,
            Long senderId,
            String senderUuid,
            String senderNickname,
            String profileImageUrl,
            String content,
            LocalDateTime createdAt,
            Long viewerUserId
    ) {
        return new ChatMessageResponse(
                messageId,
                roomId,
                messageType,
                new ChatMessageSenderResponse(
                        senderUuid,
                        senderNickname,
                        profileImageUrl,
                        senderId.equals(viewerUserId)
                ),
                content,
                createdAt
        );
    }

}
