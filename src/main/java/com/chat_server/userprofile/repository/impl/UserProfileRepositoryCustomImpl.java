package com.chat_server.userprofile.repository.impl;

import com.chat_server.user.entity.QUser;
import com.chat_server.userprofile.dto.response.UserMyProfileDetailResponse;
import com.chat_server.userprofile.dto.response.UserMyProfileSummaryResponse;
import com.chat_server.userprofile.enrtity.QUserProfile;
import com.chat_server.userprofile.enrtity.UserProfile;
import com.chat_server.userprofile.repository.UserProfileRepositoryCustom;
import com.chat_server.userprofileImage.entity.QUserProfileImage;
import com.querydsl.core.types.Projections;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.util.Optional;

public class UserProfileRepositoryCustomImpl extends QuerydslRepositorySupport implements UserProfileRepositoryCustom {
    private final QUserProfile qUserProfile = QUserProfile.userProfile;
    private final QUser qUser = QUser.user;
    private final QUserProfileImage qUserProfileImage = QUserProfileImage.userProfileImage;

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
                        .where(qUser.id.eq(id))
                        .fetchOne()
        );
    }

    @Override
    public Optional<UserMyProfileDetailResponse> findProfileDetail(Long id) {
        return Optional.ofNullable(
                from(qUserProfile)
                        .join(qUserProfile.user, qUser)
                        .leftJoin(qUserProfileImage).on(
                                qUserProfileImage.userProfile.eq(qUserProfile)
                                        .and(qUserProfileImage.current.isTrue())
                        )
                        .select(Projections.constructor(
                                UserMyProfileDetailResponse.class,
                                qUser.uuid,
                                qUser.nickname,
                                qUser.friendCode,
                                qUserProfile.stateMessage,
                                qUserProfileImage.imageUrl
                        ))
                        .where(qUser.id.eq(id))
                        .fetchOne()
        );
    }
}