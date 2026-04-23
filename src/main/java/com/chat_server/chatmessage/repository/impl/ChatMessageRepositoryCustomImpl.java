package com.chat_server.chatmessage.repository.impl;

import com.chat_server.chatlist.entity.QChatList;
import com.chat_server.chatmessage.dto.response.ChatMessageItemResponse;
import com.chat_server.chatmessage.entity.QChatMessage;
import com.chat_server.chatmessage.repository.ChatMessageRepositoryCustom;
import com.chat_server.chatread.dto.event.UpdatedMessageUnreadCount;
import com.chat_server.chatroommember.entity.QChatRoomMember;
import com.chat_server.common.cursor.ChatMessageCursorKey;
import com.chat_server.userprofile.enrtity.QUserProfile;
import com.chat_server.userprofileImage.entity.QUserProfileImage;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.querydsl.jpa.JPQLQuery;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.lang.Nullable;

public class ChatMessageRepositoryCustomImpl extends QuerydslRepositorySupport implements ChatMessageRepositoryCustom {

    private final QChatMessage qChatMessage = QChatMessage.chatMessage;
    private final QUserProfile qUserProfile = QUserProfile.userProfile;
    private final QChatList qChatList = QChatList.chatList;
    private final QUserProfileImage qUserProfileImage = QUserProfileImage.userProfileImage;
    private final QChatRoomMember qChatRoomMember = QChatRoomMember.chatRoomMember;


    public ChatMessageRepositoryCustomImpl() {
        super(QChatMessage.class);
    }

    @Override
    public Slice<ChatMessageItemResponse> getEnterMessagesByCursor(
            Long roomId,
            int limit,
            @Nullable ChatMessageCursorKey cursorKey
    ) {
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

        JPQLQuery<Integer> unreadCountExpr = getUnreadCountExpr();

        List<ChatMessageItemResponse> rows = from(qChatMessage)
                .join(qChatMessage.sender)
                .leftJoin(qUserProfile).on(qUserProfile.user.id.eq(qChatMessage.sender.id))
                .leftJoin(qUserProfileImage).on(
                        qUserProfileImage.userProfile.id.eq(qUserProfile.id)
                                .and(qUserProfileImage.current.isTrue())
                )
                .where(where)
                .orderBy(qChatMessage.createdAt.desc(), qChatMessage.id.desc())
                .limit(limit + 1)
                .select(Projections.constructor(
                        ChatMessageItemResponse.class,
                        qChatMessage.id,
                        qChatMessage.sender.id,
                        qChatMessage.sender.uuid,
                        qChatMessage.sender.nickname,
                        qUserProfileImage.imageUrl,
                        qChatMessage.messageType.stringValue(),
                        qChatMessage.messageContent,
                        qChatMessage.createdAt,
                        unreadCountExpr
                ))
                .fetch();

        boolean hasNext = rows.size() > limit;
        if (hasNext) {
            rows = rows.subList(0, limit);
        }

        return new SliceImpl<>(rows, PageRequest.of(0, limit), hasNext);
    }

    @Override
    public List<UpdatedMessageUnreadCount> findUpdatedUnreadCounts(Long roomId, Long lastReadMessageId) {
        JPQLQuery<Integer> unreadCountExpr = getUnreadCountExpr();

        return from(qChatMessage)
                .where(
                        qChatMessage.chatRoom.id.eq(roomId),
                        qChatMessage.deleted.isFalse(),
                        qChatMessage.id.loe(lastReadMessageId)
                )
                .orderBy(qChatMessage.id.asc())
                .select(Projections.constructor(
                        UpdatedMessageUnreadCount.class,
                        qChatMessage.id,
                        unreadCountExpr
                ))
                .fetch();
    }

    @Override
    public Slice<ChatMessageItemResponse> getMessagesAfter(Long roomId, Long afterMessageId, int limit) {


        List<ChatMessageItemResponse> rows = from(qChatMessage)
                .join(qChatMessage.sender)
                .leftJoin(qUserProfile).on(qUserProfile.user.id.eq(qChatMessage.sender.id))
                .leftJoin(qUserProfileImage).on(
                        qUserProfileImage.userProfile.id.eq(qUserProfile.id)
                                .and(qUserProfileImage.current.isTrue())
                )
                .where(
                        qChatMessage.chatRoom.id.eq(roomId),
                        qChatMessage.deleted.isFalse(),
                        qChatMessage.id.gt(afterMessageId)
                )
                .orderBy(qChatMessage.createdAt.asc(), qChatMessage.id.asc())
                .limit(limit + 1)
                .select(Projections.constructor(
                        ChatMessageItemResponse.class,
                        qChatMessage.id,
                        qChatMessage.sender.id,
                        qChatMessage.sender.uuid,
                        qChatMessage.sender.nickname,
                        qUserProfileImage.imageUrl,
                        qChatMessage.messageType.stringValue(),
                        qChatMessage.messageContent,
                        qChatMessage.createdAt,
                        getUnreadCountExpr()
                ))
                .fetch();

        boolean hasNext = rows.size() > limit;
        if (hasNext) {
            rows = rows.subList(0, limit);
        }

        return new SliceImpl<>(rows, PageRequest.of(0, limit), hasNext);
    }

    private  JPQLQuery<Integer> getUnreadCountExpr(){

    return JPAExpressions
                    .select(qChatRoomMember.count().intValue())
                    .from(qChatRoomMember)
                    .leftJoin(qChatList).on(
                            qChatList.user.id.eq(qChatRoomMember.user.id)
                                    .and(qChatList.chatRoom.id.eq(qChatRoomMember.chatRoom.id))
                    )
                    .where(
                            qChatRoomMember.chatRoom.id.eq(qChatMessage.chatRoom.id),
                            qChatRoomMember.active.isTrue(),
                            qChatMessage.sender.id.isNull()
                                    .or(qChatRoomMember.user.id.ne(qChatMessage.sender.id)),
                            qChatList.lastReadMessageId.isNull()
                                    .or(qChatList.lastReadMessageId.lt(qChatMessage.id))
                    );
    }
}