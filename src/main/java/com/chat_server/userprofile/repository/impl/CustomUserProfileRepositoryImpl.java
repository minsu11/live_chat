//package com.chat_server.userprofile.repository.impl;
//
//import com.chat_server.userprofile.dto.response.UserMyProfileResponse;
//import com.chat_server.userprofile.enrtity.QUserProfile;
//import com.chat_server.userprofile.enrtity.UserProfile;
//import com.chat_server.userprofile.repository.CustomUserProfileRepository;
//import java.util.Optional;
//import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
//
//public class CustomUserProfileRepositoryImpl extends QuerydslRepositorySupport implements CustomUserProfileRepository {
//    private final QUserProfile qUserProfile = QUserProfile.userProfile;
//
//    public CustomUserProfileRepositoryImpl() {
//        super(UserProfile.class);
//    }
//    @Override
//    public Optional<UserMyProfileResponse> findMyProfile(Long id) {
//        return Optional.empty();
//    }
//}
