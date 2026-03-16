package com.chat_server.userprofileImage.repository.impl;

import com.chat_server.user.entity.QUser;
import com.chat_server.userprofile.enrtity.QUserProfile;
import com.chat_server.userprofileImage.entity.QUserProfileImage;
import com.chat_server.userprofileImage.entity.UserProfileImage;
import com.chat_server.userprofileImage.repository.CustomUserProfileImageRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

public class CustomUserProfileImageRepositoryImpl extends QuerydslRepositorySupport implements
        CustomUserProfileImageRepository {

    private final QUserProfileImage qUserProfileImage = QUserProfileImage.userProfileImage;
    private final QUserProfile qUserProfile = QUserProfile.userProfile;
    private final QUser qUser = QUser.user;
    public CustomUserProfileImageRepositoryImpl() {
        super(QUserProfileImage.class);
    }

    @Override
    public Optional<UserProfileImage> getUserProfileUrl(Long userId) {


        return Optional.ofNullable(
            from(qUserProfileImage)
                .select(qUserProfileImage)
                .leftJoin(qUserProfile).on(qUserProfile.id.eq(qUserProfileImage.userProfile.id))
                .leftJoin(qUser).on(qUser.id.eq(qUserProfile.user.id))
                .where(qUserProfileImage.current.eq(true).and(qUser.id.eq(userId)))
                .fetchOne()
        );
    }
}
