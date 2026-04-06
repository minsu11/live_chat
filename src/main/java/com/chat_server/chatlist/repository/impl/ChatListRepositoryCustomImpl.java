package com.chat_server.chatlist.repository.impl;

import com.chat_server.chatlist.dto.response.ChatListItemResponse;
import com.chat_server.chatlist.dto.response.ChatRoomListResponse;
import com.chat_server.chatlist.dto.response.ChatRoomListRow;
import com.chat_server.chatlist.entity.QChatList;
import com.chat_server.chatlist.repository.ChatListRepositoryCustom;
import com.chat_server.chatroom.entity.QChatRoom;
import com.chat_server.chatroommember.entity.QChatRoomMember;
import com.chat_server.common.cursor.ChatListCursorKey;
import com.chat_server.friend.entity.QFriend;
import com.chat_server.user.entity.QUser;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringExpression;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.lang.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Slf4j
public class ChatListRepositoryCustomImpl extends QuerydslRepositorySupport
    implements ChatListRepositoryCustom {

    private final QChatList qChatList = QChatList.chatList;
    private final QChatRoom qChatRoom = QChatRoom.chatRoom;
    private final QChatRoomMember qChatRoomMember = QChatRoomMember.chatRoomMember;
    private final QFriend qFriend = QFriend.friend;
    private final QUser qPartnerUser = new QUser("partnerUser");

    public ChatListRepositoryCustomImpl() {
        super(QChatList.class);
    }

    @Override
    public Slice<ChatRoomListResponse> getChatRoomListByCursor(
        Long userId,
        int limit,
        @Nullable ChatListCursorKey cursorKey
    ) {
        StringExpression displayName = getDisplayNameExpression(userId);
        DateTimeExpression<LocalDateTime> orderAt = getOrderAtExpression();

        BooleanBuilder where = new BooleanBuilder()
            .and(qChatList.user.id.eq(userId));

        if (cursorKey != null) {
            LocalDateTime cursorAt = Instant.ofEpochMilli(cursorKey.lastAtEpochMillis())
                .atOffset(ZoneOffset.UTC)
                .toLocalDateTime();

            where.and(
                orderAt.lt(cursorAt)
                    .or(orderAt.eq(cursorAt)
                        .and(qChatRoom.id.lt(cursorKey.lastRoomId())))
            );
        }

        List<ChatRoomListRow> rows = from(qChatList)
            .join(qChatList.chatRoom, qChatRoom)
            .leftJoin(qChatRoomMember).on(getDmPartnerJoinCondition(userId))
            .leftJoin(qChatRoomMember.user, qPartnerUser)
            .leftJoin(qFriend).on(
                qFriend.user.id.eq(userId)
                    .and(qFriend.friendUser.id.eq(qPartnerUser.id))
            )
            .where(where)
            .orderBy(orderAt.desc(), qChatRoom.id.desc())
            .limit(limit + 1)
            .select(Projections.constructor(
                ChatRoomListRow.class,
                qChatRoom.id,
                displayName,
                qChatRoom.lastMessageAt,
                qChatList.unreadCount,
                orderAt
            ))
            .fetch();

        boolean hasNext = rows.size() > limit;
        if (hasNext) {
            rows = rows.subList(0, limit);
        }

        List<ChatRoomListResponse> content = rows.stream()
            .map(r -> new ChatRoomListResponse(
                r.roomId(),
                r.displayName(),
                r.unreadCount(),
                r.lastMessageAt(),
                r.orderAt()
            ))
            .toList();

        return new SliceImpl<>(content, PageRequest.of(0, limit), hasNext);
    }

    @Override
    public Optional<ChatListItemResponse> findChatListItem(Long roomId, Long userId) {
        StringExpression displayName = getDisplayNameExpression(userId);
        DateTimeExpression<LocalDateTime> orderAt = getOrderAtExpression();

        ChatListItemResponse result = from(qChatList)
            .join(qChatList.chatRoom, qChatRoom)
            .leftJoin(qChatRoomMember).on(getDmPartnerJoinCondition(userId))
            .leftJoin(qChatRoomMember.user, qPartnerUser)
            .leftJoin(qFriend).on(
                qFriend.user.id.eq(userId)
                    .and(qFriend.friendUser.id.eq(qPartnerUser.id))
            )
            .where(
                qChatList.user.id.eq(userId),
                qChatRoom.id.eq(roomId)
            )
            .select(Projections.constructor(
                ChatListItemResponse.class,
                qChatRoom.id,
                displayName,
                qChatList.unreadCount,
                qChatRoom.lastMessageAt,
                orderAt
            ))
            .fetchOne();

        return Optional.ofNullable(result);
    }

    private StringExpression getDisplayNameExpression(Long userId) {
        var isDm = qChatRoom.dmKey.isNotNull();

        var dmDisplayName = Expressions.stringTemplate(
            "COALESCE({0}, {1}, {2})",
            qFriend.customNickname,
            qPartnerUser.nickname,
            qChatRoom.name
        );

        return new CaseBuilder()
            .when(qChatList.customName.isNotNull()).then(qChatList.customName)
            .when(isDm).then(dmDisplayName)
            .otherwise(qChatRoom.name);
    }

    private DateTimeExpression<LocalDateTime> getOrderAtExpression() {
        return Expressions.dateTimeTemplate(
            LocalDateTime.class,
            "COALESCE({0}, {1})",
            qChatRoom.lastMessageAt,
            qChatRoom.createdAt
        );
    }

    private BooleanBuilder getDmPartnerJoinCondition(Long userId) {
        return new BooleanBuilder()
            .and(qChatRoomMember.chatRoom.eq(qChatRoom))
            .and(qChatRoom.dmKey.isNotNull())
            .and(qChatRoomMember.user.id.ne(userId))
            .and(qChatRoomMember.active.isTrue())
            .and(qChatRoomMember.leftAt.isNull());
    }
}