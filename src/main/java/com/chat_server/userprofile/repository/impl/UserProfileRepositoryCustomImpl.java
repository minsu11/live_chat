package com.chat_server.userprofile.repository.impl;

import com.chat_server.friend.entity.QFriend;
import com.chat_server.user.entity.QUser;
import com.chat_server.user.enums.UserStatus;
import com.chat_server.userprofile.dto.response.UserProfileDetailResponse;
import com.chat_server.userprofile.dto.response.UserMyProfileSummaryResponse;
import com.chat_server.userprofile.enrtity.QUserProfile;
import com.chat_server.userprofile.enrtity.UserProfile;
import com.chat_server.userprofile.repository.UserProfileRepositoryCustom;
import com.chat_server.userprofileImage.entity.QUserProfileImage;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.util.Optional;

public class UserProfileRepositoryCustomImpl extends QuerydslRepositorySupport implements UserProfileRepositoryCustom {

    private final QUserProfile qUserProfile = QUserProfile.userProfile;
    private final QUser qUser = QUser.user;
    private final QUserProfileImage qUserProfileImage = QUserProfileImage.userProfileImage;
    private final QFriend qFriend = QFriend.friend;

    public UserProfileRepositoryCustomImpl() {
        super(UserProfile.class);
    }

    @Override
    public Optional<UserMyProfileSummaryResponse> findMyProfile(Long id) {
        return Optional.ofNullable(
                from(qUserProfile)
                        .join(qUserProfile.user, qUser)
                        .leftJoin(qUserProfileImage).on(
                                qUserProfileImage.userProfile.eq(qUserProfile)
                                        .and(qUserProfileImage.current.isTrue())
                        )
                        .select(Projections.constructor(
                                UserMyProfileSummaryResponse.class,
                                qUser.uuid,
                                qUser.nickname,
                                qUser.friendCode,
                                qUserProfile.stateMessage,
                                qUserProfileImage.imageUrl
                        ))
                        .where(
                                qUser.id.eq(id)
                                        .and(qUser.status.eq(UserStatus.ACTIVE))
                        )
                        .fetchOne()
        );
    }

    /**
     * 내 프로필 상세 조회
     * - me = true
     * - friend = false
     */
    @Override
    public Optional<UserProfileDetailResponse> findProfileDetail(Long id) {
        return Optional.ofNullable(
                from(qUserProfile)
                        .join(qUserProfile.user, qUser)
                        .leftJoin(qUserProfileImage).on(
                                qUserProfileImage.userProfile.eq(qUserProfile)
                                        .and(qUserProfileImage.current.isTrue())
                        )
                        .select(Projections.constructor(
                                UserProfileDetailResponse.class,
                                qUser.uuid,
                                qUser.nickname,
                                qUser.friendCode,
                                qUserProfile.stateMessage,
                                qUserProfileImage.imageUrl,
                                Expressions.constant(false), // friend
                                Expressions.constant(true)   // me
                        ))
                        .where(
                                qUser.id.eq(id)
                                        .and(qUser.status.eq(UserStatus.ACTIVE))
                        )
                        .fetchOne()
        );
    }

    /**
     * uuid 기반 프로필 상세 조회
     * - viewerId: 현재 로그인한 사용자 id
     * - targetUserUuid: 조회 대상 사용자 uuid
     */
    @Override
    public Optional<UserProfileDetailResponse> findProfileDetailByUuid(
            Long viewerId,
            String targetUserUuid
    ) {
        return Optional.ofNullable(
                from(qUserProfile)
                        .join(qUserProfile.user, qUser)
                        .leftJoin(qUserProfileImage).on(
                                qUserProfileImage.userProfile.eq(qUserProfile)
                                        .and(qUserProfileImage.current.isTrue())
                        )
                        .leftJoin(qFriend).on(
                                qFriend.user.id.eq(viewerId)
                                        .and(qFriend.friendUser.eq(qUser))
                        )
                        .select(Projections.constructor(
                                UserProfileDetailResponse.class,
                                qUser.uuid,
                                qUser.nickname,
                                qUser.friendCode,
                                qUserProfile.stateMessage,
                                qUserProfileImage.imageUrl,
                                qFriend.id.isNotNull(), // friend
                                qUser.id.eq(viewerId)   // me
                        ))
                        .where(
                                qUser.uuid.eq(targetUserUuid)
                                        .and(qUser.status.eq(UserStatus.ACTIVE))
                        )
                        .fetchOne()
        );
    }
}