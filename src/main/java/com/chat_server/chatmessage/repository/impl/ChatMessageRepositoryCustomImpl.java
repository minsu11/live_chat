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

    /**
     * Querydsl 기반 커스텀 레포지토리 생성자.
     */
    public ChatMessageRepositoryCustomImpl() {
        super(QChatMessage.class);
    }

    /**
     * 채팅방 진입용 메시지를 커서 기반으로 조회한다.
     *
     * @param roomId 채팅방 ID
     * @param limit 페이지 크기
     * @param cursorKey 커서 키(없으면 첫 페이지)
     * @return 메시지 Slice (hasNext 포함)
     *
     * <p>동작 방식:
     * <ul>
     *   <li>삭제되지 않은 메시지만 조회</li>
     *   <li>정렬: createdAt DESC, id DESC</li>
     *   <li>limit+1 조회 후 hasNext 계산</li>
     * </ul>
     */
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
                .join(qChatMessage.sender)
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
