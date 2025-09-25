package com.chat_server.chatlist.repository.impl;

import com.chat_server.chatlist.dto.response.ChatRoomListResponse;
import com.chat_server.chatlist.entity.ChatList;
import com.chat_server.chatlist.entity.QChatList;
import com.chat_server.chatlist.repository.ChatListRepositoryCustom;
import com.chat_server.chatroom.entity.QChatRoom;
import com.chat_server.common.cursor.ChatListCursorKey;
import com.chat_server.common.cursor.CursorKey;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Null;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

@Slf4j
public class ChatListRepositoryCustomImpl extends QuerydslRepositorySupport implements
    ChatListRepositoryCustom {
    private final QChatList qChatList = QChatList.chatList;
    private final QChatRoom qChatRoom = QChatRoom.chatRoom;
    public ChatListRepositoryCustomImpl() {
        super(QChatList.class);
    }


    @Override
    public Slice<ChatRoomListResponse> getChatRoomListByCursor(Long userId, int limit,
        @Nullable ChatListCursorKey cursorKey) {

        // user id 조회
        BooleanBuilder where = new BooleanBuilder().and(qChatList.user.id.eq(userId));

        if(cursorKey != null ) {
            LocalDateTime cursorAt = Instant.ofEpochMilli(cursorKey.lastAtEpochMillis())
                .atOffset(ZoneOffset.UTC)
                .toLocalDateTime();
            where.and(
                qChatRoom.lastMessageAt.lt(cursorAt)
                    .or(
                        qChatRoom.lastMessageAt.eq(cursorAt)
                            .and(qChatRoom.id.lt(cursorKey.lastRoomId()))
                    )
            );
        }

        List<ChatRoomListResponse> rows = from(qChatList)
            .leftJoin(qChatList.chatRoom, qChatRoom)
            .where(where)
            .orderBy(qChatRoom.lastMessageAt.desc().nullsLast(), qChatRoom.id.desc())
            .limit(limit + 1)
            .select(Projections.constructor(
                ChatRoomListResponse.class,
                qChatRoom.id,
                qChatRoom.name,
                qChatRoom.lastMessageAt
            )).fetch();

        boolean hasNext = rows.size() > limit;
        if (hasNext) {
            rows = rows.subList(0, limit);
        }

        return new SliceImpl<>(rows, PageRequest.of(0,limit), hasNext);
    }
}
