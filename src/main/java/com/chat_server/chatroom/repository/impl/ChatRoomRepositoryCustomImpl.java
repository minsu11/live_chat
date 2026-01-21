package com.chat_server.chatroom.repository.impl;

import com.chat_server.chatlist.entity.QChatList;
import com.chat_server.chatroom.dto.response.ChatRoomSummaryResponse;
import com.chat_server.chatroom.entity.QChatRoom;
import com.chat_server.chatroom.repository.ChatRoomRepositoryCustom;
import com.chat_server.chattype.entity.ChatType;
import com.chat_server.user.entity.QUser;
import com.chat_server.userprofile.enrtity.QUserProfile;
import com.chat_server.userprofileurl.entity.QUserProfileUrl;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.JPAExpressions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.util.Optional;

@Slf4j
public class ChatRoomRepositoryCustomImpl extends QuerydslRepositorySupport implements ChatRoomRepositoryCustom {
    private final QChatRoom qChatRoom = QChatRoom.chatRoom;
    private final QUser qUser = QUser.user;
    private final QChatList qChatList = QChatList.chatList;
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
    public Optional<ChatRoomSummaryResponse> findChatRoomSummaryByRoomId(Long roomId, Long userId) {
        // 결국 chat type 들어와서 분리를 해야할듯
        // todo 추 후 수정이 될 가능성도 있음
        // 1 대 1인 가정
        log.info("findChatRoomSummaryByRoomId:{}", roomId);

        QChatList qChatList = QChatList.chatList;
        QUserProfileUrl qUserProfileUrl = QUserProfileUrl.userProfileUrl;
        QUserProfile qUserProfile = QUserProfile.userProfile;

        // title
        StringExpression titleExpr = qChatList.customName
                .coalesce(qChatList.partnerUser.userNickname)
                .coalesce(qChatRoom.name);

        Expression<String> imageUrlExpr = JPAExpressions
                .select(qUserProfileUrl.imageUrl)
                .from(qUserProfileUrl)
                .where(qUserProfileUrl.userProfile.id.eq(qUserProfile.id))
                .orderBy(qUserProfileUrl.id.desc())
                .limit(1L);

        ChatRoomSummaryResponse response = from(qChatRoom)
                .join(qChatList).on(qChatList.chatRoom.id.eq(qChatRoom.id)
                        .and(qChatList.user.id.eq(userId)))
                .leftJoin(qUserProfile).on(qUserProfile.user.id.eq(qChatList.partnerUser.id))
                .leftJoin(qUserProfileUrl).on(qUserProfileUrl.userProfile.id.eq(qUserProfile.id))
                .select(Projections.constructor(
                        ChatRoomSummaryResponse.class,
                        qChatRoom.id,
                        qChatRoom.chatType.chatTypeName,
                        titleExpr,
                        imageUrlExpr,
                        qChatRoom.maxPerson
                ))
                .where(qChatRoom.id.eq(roomId))
                .fetchOne();


        log.info("result: {}", response);
        log.info("end");
        return Optional.ofNullable(response);
    }

    // 본인 유저 아이디로 상대 측 유저 아이디 알아내는 메서드
    @Override
    public Optional<Long> findMemberIdByRoomId(Long roomId, Long userId) {
        return Optional.ofNullable(
            from(qChatList)
                .select(qChatList.partnerUser.id)
                .join(qChatList.chatRoom,qChatRoom)
                .where(
                    qChatRoom.id.eq(roomId)
                            .and(qChatList.user.id.eq(userId))
                        .and(qChatList.partnerUser.isNotNull())
                )
                .fetchOne()
        );
    }

    // room에 본인 유저 아이디가 있는지 판별하는 아이디
    @Override
    public boolean existsByUserId(Long roomId,Long userId) {
        Long result = from(qChatList)
            .select(qChatList.user.id)
            .where(qChatRoom.id.eq(roomId)
                .and(qChatList.user.id.eq(userId))
            )
            .fetchFirst();
        return result != null;
    }

}
