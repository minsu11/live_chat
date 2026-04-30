package com.chat_server.chatroommember.repository.impl;

import com.chat_server.chatroommember.dto.response.ChatRoomMemberInfoDto;
import com.chat_server.chatroommember.entity.QChatRoomMember;
import com.chat_server.chatroommember.repository.ChatRoomMemberRepositoryCustom;
import com.chat_server.user.entity.QUser;
import com.chat_server.userprofile.enrtity.QUserProfile;
import com.chat_server.userprofileImage.entity.QUserProfileImage;
import com.querydsl.core.types.Projections;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.util.List;

public class ChatRoomMemberRepositoryCustomImpl extends QuerydslRepositorySupport implements ChatRoomMemberRepositoryCustom{
    private final QChatRoomMember qChatRoomMember = QChatRoomMember.chatRoomMember;
    public ChatRoomMemberRepositoryCustomImpl() {
        super(QChatRoomMember.class);
    }


    @Override
    public List<ChatRoomMemberInfoDto> findMemberInfosByRoomId(Long roomId) {
        QUserProfileImage userProfileImage = QUserProfileImage.userProfileImage;
        QUserProfile userProfile = QUserProfile.userProfile;
        QUser user = QUser.user;
        return from(qChatRoomMember)
                .leftJoin(userProfile).on(userProfile.user.eq(qChatRoomMember.user))
                .leftJoin(userProfileImage).on(userProfileImage.userProfile.id.eq(userProfile.id).and(userProfileImage.current.isTrue()))
                .innerJoin(qChatRoomMember.user, user)
                .select(Projections.constructor(
                        ChatRoomMemberInfoDto.class,
                        qChatRoomMember.user.id,
                        qChatRoomMember.user.uuid,
                        qChatRoomMember.user.nickname,
                        userProfileImage.imageUrl

                ))
                .where(qChatRoomMember.chatRoom.id.eq(roomId)
                        .and(qChatRoomMember.active.isTrue())
                        .and(qChatRoomMember.leftAt.isNull()))
                .fetch();
    }
}
