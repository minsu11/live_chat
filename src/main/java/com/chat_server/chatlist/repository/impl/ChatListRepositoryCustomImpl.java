package com.chat_server.chatlist.repository.impl;

import com.chat_server.chatlist.dto.response.ChatRoomListResponse;
import com.chat_server.chatlist.dto.response.ChatRoomListRow;
import com.chat_server.chatlist.entity.ChatList;
import com.chat_server.chatlist.entity.QChatList;
import com.chat_server.chatlist.repository.ChatListRepositoryCustom;
import com.chat_server.chatroom.entity.QChatRoom;
import com.chat_server.common.cursor.ChatListCursorKey;
import com.chat_server.common.cursor.CursorKey;
import com.chat_server.friend.entity.QFriend;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Coalesce;
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
    private final QFriend qFriend = QFriend.friend1;
    public ChatListRepositoryCustomImpl() {
        super(QChatList.class);
    }


    @Override
    public Slice<ChatRoomListResponse> getChatRoomListByCursor(Long userId, int limit,
        @Nullable ChatListCursorKey cursorKey) {

        var isDm = qChatRoom.participantsHash.isNotNull();

        var dmName = new CaseBuilder()
                .when(qFriend.user.userNickname.isNotNull()).then(qFriend.user.userNickname)
                .otherwise(qChatRoom.name);

        var displayName = new CaseBuilder()
                .when(qChatList.customName.isNotNull()).then(qChatList.customName)
                .when(isDm).then(dmName)
                .otherwise(qChatRoom.name); // ← StringExpression 확정

        log.info("display name: {}", displayName);

        var orderAt = com.querydsl.core.types.dsl.Expressions.dateTimeTemplate(
                LocalDateTime.class,
                "COALESCE({0}, {1})",
                qChatRoom.lastMessageAt, qChatRoom.createdAt);
        
        // TODO 마지막 메세지 OR 최근에 온 메세지 + 채팅방 프로필 추가해야함

        // user id 조회
        BooleanBuilder where = new BooleanBuilder().and(qChatList.user.id.eq(userId));

        if(cursorKey != null ) {
            var cursorAt = Instant.ofEpochMilli(cursorKey.lastAtEpochMillis())
                    .atOffset(ZoneOffset.UTC).toLocalDateTime();

            // OR isNull() 제거. 정렬키와 동일한 비교식으로 통일
            where.and(
                    orderAt.lt(cursorAt)
                            .or(qChatRoom.orderAt.eq(cursorAt).and(qChatRoom.id.lt(cursorKey.lastRoomId())))
            );
        }

        List<ChatRoomListRow> rows = from(qChatList)
            .join(qChatList.chatRoom, qChatRoom)
                .leftJoin(qChatList.friend, qFriend).on(isDm)
            .where(where)
            .orderBy(qChatRoom.orderAt.desc(), qChatRoom.id.desc())
            .limit(limit + 1)
            .select(Projections.constructor(
                    ChatRoomListRow.class,
                    qChatRoom.id,
                    displayName,
                    qChatRoom.lastMessageAt,
                    orderAt
            )).fetch();

        boolean hasNext = rows.size() > limit;
        if (hasNext) rows = rows.subList(0, limit);

        List<ChatRoomListResponse> content = rows.stream()
                .map(r -> new ChatRoomListResponse(r.roomId(), r.displayName(), r.lastMessageAt()))
                .toList();

        // nextCursor 만들 때 orderAt 사용 (NULL 안전)
        ChatListCursorKey next = null;
        if (hasNext && !rows.isEmpty()) {
            ChatRoomListRow tail = rows.get(rows.size() - 1);
            long ts = tail.orderAt().toInstant(ZoneOffset.UTC).toEpochMilli();
            next = new ChatListCursorKey(ts, tail.roomId());
        }


        return new SliceImpl<>(content, PageRequest.of(0,limit), hasNext);
    }
}
