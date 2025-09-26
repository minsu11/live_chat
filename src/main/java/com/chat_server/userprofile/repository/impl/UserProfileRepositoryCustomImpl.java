package com.chat_server.userprofile.repository.impl;

import com.chat_server.user.entity.QUser;
import com.chat_server.userprofile.dto.response.UserMyProfileResponse;
import com.chat_server.userprofile.enrtity.QUserProfile;
import com.chat_server.userprofile.enrtity.UserProfile;
import com.chat_server.userprofile.repository.UserProfileRepositoryCustom;
import com.chat_server.userprofile.url.entity.QUserProfileUrl;
import com.querydsl.core.types.Projections;
import java.util.Optional;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

public class UserProfileRepositoryCustomImpl extends QuerydslRepositorySupport implements UserProfileRepositoryCustom {
    private final QUserProfile qUserProfile = QUserProfile.userProfile;
    private final QUser qUser = QUser.user;
    private final QUserProfileUrl qUserProfileUrl = QUserProfileUrl.userProfileUrl;
    public UserProfileRepositoryCustomImpl() {
        super(UserProfile.class);
    }
    @Override
    public Optional<UserMyProfileResponse> findMyProfile(Long id) {

        Optional<UserMyProfileResponse> response =
            Optional.ofNullable(
              from(qUserProfile)
                  .leftJoin(qUserProfileUrl).on(qUserProfileUrl.userProfile.eq(qUserProfile))
                  .leftJoin(qUserProfile).on(qUserProfile.user.eq(qUser))
                  .select(Projections.constructor(UserMyProfileResponse.class,
                        qUserProfileUrl.imageUrl,
                          qUserProfile.stateMessage
                      ))
                  .where(qUserProfile.user.id.eq(id))
                  .fetchOne()
            );

        return response;
    }
}
