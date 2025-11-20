package com.chat_server.chatroom.repository.impl;

import com.chat_server.chatlist.entity.QChatList;
import com.chat_server.chatroom.dto.response.ChatRoomSummaryResponse;
import com.chat_server.chatroom.entity.QChatRoom;
import com.chat_server.chatroom.repository.ChatRoomRepositoryCustom;
import com.chat_server.chattype.entity.ChatType;
import com.chat_server.userprofile.enrtity.QUserProfile;
import com.chat_server.userprofile.url.entity.QUserProfileUrl;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Coalesce;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.JPQLQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.util.Optional;

@Slf4j
public class ChatRoomRepositoryCustomImpl extends QuerydslRepositorySupport implements ChatRoomRepositoryCustom {
    private final QChatRoom qChatRoom = QChatRoom.chatRoom;

    public ChatRoomRepositoryCustomImpl() {
        super(QChatRoom.class);
    }

    @Override
    public Optional<Long> findRoomIdByParticipantsHashAndChatType(String participantsHash, ChatType chatType) {
        return Optional.ofNullable(
                from(qChatRoom)
                        .select(qChatRoom.id)
                        .where(qChatRoom.chatType.eq(chatType)
                                .and(qChatRoom.participantsHash.eq(participantsHash)))
                .fetchOne()
        );
    }

    @Override
    public Optional<ChatRoomSummaryResponse> findChatRoomSummaryByRoomId(Long roomId) {
        // 결국 chat type 들어와서 분리를 해야할듯
        // todo 추 후 수정이 될 가능성도 있음
        // 1 대 1인 가정
        log.info("findChatRoomSummaryByRoomId:{}", roomId);

        QChatList qChatList = QChatList.chatList;
        QUserProfileUrl qUserProfileUrl = QUserProfileUrl.userProfileUrl;
        QUserProfile qUserProfile = QUserProfile.userProfile;

        // title
        StringExpression titleExpr = qChatList.customName
                .coalesce(qChatList.friend.user.userNickname)
                .coalesce(qChatRoom.name);

        JPQLQuery<Long> memberCnt = from(qChatList).select(qChatList.id.count())
                .where(qChatList.chatRoom.id.eq(roomId));


        ChatRoomSummaryResponse response = from(qChatList)
                .join(qChatList).on(qChatList.chatRoom.id.eq(qChatRoom.id))
                .leftJoin(qUserProfileUrl).on(qUserProfileUrl.userProfile.id.eq(qUserProfile.id))
                .leftJoin(qUserProfile).on(qUserProfile.user.id.eq(qChatList.friend.user.id))
                .select(Projections.constructor(
                        ChatRoomSummaryResponse.class,
                        qChatRoom.id,
                        qChatRoom.chatType.chatTypeName,
                        titleExpr,
                        qUserProfileUrl.imageUrl,
                        memberCnt
                ))
                .where(qChatRoom.id.eq(roomId))
                .fetchOne();


        log.info("result: {}", response);
        log.info("end");
        return Optional.ofNullable(response);
    }
}
