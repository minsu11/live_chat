package com.chat_server.userprofile.repository.impl;

import com.chat_server.user.entity.QUser;
import com.chat_server.userprofile.dto.response.UserMyProfileResponse;
import com.chat_server.userprofile.dto.response.UserProfileDetailResponse;
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


    // 쿼리 관점으로 from 기준을 잡으면 될듯.
    // user profile 무조건 존재한다고 가정 하면은. user profile 가능, 그렇지만 무조건 존재하지 않는다면은 user로 잡아야 함
    // 그 이유는 user profile이 없는 사람이 있다면 프로필을 가지고 올 수 없기 때문
    @Override
    public Optional<UserProfileDetailResponse> findProfileDetail(Long id) {
        return
                 Optional.ofNullable(
                         from(qUserProfile)
                                 .leftJoin(qUserProfile).on(qUserProfile.user.eq(qUser))
                                 .leftJoin(qUserProfileUrl).on(qUserProfileUrl.userProfile.eq(qUserProfile)
                                         .and(qUserProfileUrl.isCurrent.isTrue()))
                                 .select(Projections.constructor(
                                         UserProfileDetailResponse.class,
                                         qUser.userName,
                                         qUserProfile.stateMessage,
                                         qUserProfileUrl.imageUrl
                                 ))
                                 .where(qUserProfile.user.id.eq(id))
                                 .fetchOne()
                 );
    }
}
