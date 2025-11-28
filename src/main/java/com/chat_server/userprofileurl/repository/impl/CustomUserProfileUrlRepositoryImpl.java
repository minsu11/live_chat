package com.chat_server.userprofileurl.repository.impl;

import com.chat_server.user.entity.QUser;
import com.chat_server.userprofile.enrtity.QUserProfile;
import com.chat_server.userprofileurl.entity.QUserProfileUrl;
import com.chat_server.userprofileurl.entity.UserProfileUrl;
import com.chat_server.userprofileurl.repository.CustomUserProfileUrlRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

public class CustomUserProfileUrlRepositoryImpl extends QuerydslRepositorySupport implements
    CustomUserProfileUrlRepository {

    private final QUserProfileUrl qUserProfileUrl = QUserProfileUrl.userProfileUrl;
    private final QUserProfile qUserProfile = QUserProfile.userProfile;
    private final QUser qUser = QUser.user;
    public CustomUserProfileUrlRepositoryImpl() {
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
