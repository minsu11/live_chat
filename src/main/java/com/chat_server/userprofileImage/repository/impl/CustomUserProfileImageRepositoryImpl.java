package com.chat_server.userprofileImage.repository.impl;

import com.chat_server.user.entity.QUser;
import com.chat_server.userprofile.enrtity.QUserProfile;
import com.chat_server.userprofileImage.entity.QUserProfileUrl;
import com.chat_server.userprofileImage.entity.UserProfileUrl;
import com.chat_server.userprofileImage.repository.CustomUserProfileImageRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

public class CustomUserProfileImageRepositoryImpl extends QuerydslRepositorySupport implements
        CustomUserProfileImageRepository {

    private final QUserProfileUrl qUserProfileUrl = QUserProfileUrl.userProfileUrl;
    private final QUserProfile qUserProfile = QUserProfile.userProfile;
    private final QUser qUser = QUser.user;
    public CustomUserProfileImageRepositoryImpl() {
        super(UserProfileUrl.class);
    }

    @Override
    public Optional<UserProfileUrl> getUserProfileUrl(Long userId) {


        return Optional.ofNullable(
            from(qUserProfileUrl)
                .select(qUserProfileUrl)
                .leftJoin(qUserProfile).on(qUserProfile.id.eq(qUserProfileUrl.userProfile.id))
                .leftJoin(qUser).on(qUser.id.eq(qUserProfile.user.id))
                .where(qUserProfileUrl.isCurrent.eq(true).and(qUser.id.eq(userId)))
                .fetchOne()
        );
    }
}
