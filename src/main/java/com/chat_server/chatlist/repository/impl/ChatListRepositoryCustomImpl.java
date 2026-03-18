package com.chat_server.chatlist.repository.impl;

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
import com.querydsl.core.types.dsl.Expressions;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.lang.Nullable;

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

        var isDm = qChatRoom.dmKey.isNotNull();

        var dmDisplayName = Expressions.stringTemplate(
                "COALESCE({0}, {1}, {2})",
                qFriend.customNickname,
                qPartnerUser.nickname,
                qChatRoom.name
        );

        var displayName = new CaseBuilder()
                .when(qChatList.customName.isNotNull()).then(qChatList.customName)
                .when(isDm).then(dmDisplayName)
                .otherwise(qChatRoom.name);

        var orderAt = Expressions.dateTimeTemplate(
                LocalDateTime.class,
                "COALESCE({0}, {1})",
                qChatRoom.lastMessageAt,
                qChatRoom.createdAt
        );

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

                // DM일 때만 상대방 멤버 조인
                .leftJoin(qChatRoomMember).on(
                        qChatRoomMember.chatRoom.eq(qChatRoom)
                                .and(qChatRoom.dmKey.isNotNull())
                                .and(qChatRoomMember.user.id.ne(userId))
                                .and(qChatRoomMember.active.isTrue())
                                .and(qChatRoomMember.leftAt.isNull())
                )
                .leftJoin(qChatRoomMember.user, qPartnerUser)

                // 내 친구목록에서 상대방 별칭 조회
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
                        r.lastMessageAt()
                ))
                .toList();

        return new SliceImpl<>(content, PageRequest.of(0, limit), hasNext);
    }
}