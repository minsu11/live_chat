package com.chat_server.chatmessage.repository.impl;

import com.chat_server.chatmessage.dto.response.ChatMessageItemResponse;
import com.chat_server.chatmessage.entity.QChatMessage;
import com.chat_server.chatmessage.repository.ChatMessageRepositoryCustom;
import com.chat_server.common.cursor.ChatMessageCursorKey;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.lang.Nullable;

public class ChatMessageRepositoryCustomImpl extends QuerydslRepositorySupport implements ChatMessageRepositoryCustom {

    private final QChatMessage qChatMessage = QChatMessage.chatMessage;

    public ChatMessageRepositoryCustomImpl() {
        super(QChatMessage.class);
    }

    @Override
    public Slice<ChatMessageItemResponse> getEnterMessagesByCursor(Long roomId, int limit,
                                                                   @Nullable ChatMessageCursorKey cursorKey) {
        BooleanBuilder where = new BooleanBuilder()
                .and(qChatMessage.chatRoom.id.eq(roomId))
                .and(qChatMessage.deleted.isFalse());

        if (cursorKey != null) {
            LocalDateTime cursorAt = Instant.ofEpochMilli(cursorKey.lastMessageAtEpochMillis())
                    .atOffset(ZoneOffset.UTC)
                    .toLocalDateTime();

            where.and(
                    qChatMessage.createdAt.lt(cursorAt)
                            .or(qChatMessage.createdAt.eq(cursorAt)
                                    .and(qChatMessage.id.lt(cursorKey.lastMessageId())))
            );
        }

        List<ChatMessageItemResponse> rows = from(qChatMessage)
                .join(qChatMessage.sender).fetchJoin()
                .where(where)
                .orderBy(qChatMessage.createdAt.desc(), qChatMessage.id.desc())
                .limit(limit + 1)
                .select(Projections.constructor(
                        ChatMessageItemResponse.class,
                        qChatMessage.id,
                        qChatMessage.sender.id,
                        qChatMessage.sender.nickname,
                        qChatMessage.messageType.stringValue(),
                        qChatMessage.messageContent,
                        qChatMessage.createdAt
                ))
                .fetch();

        boolean hasNext = rows.size() > limit;
        if (hasNext) {
            rows = rows.subList(0, limit);
        }

        return new SliceImpl<>(rows, PageRequest.of(0, limit), hasNext);
    }
}
