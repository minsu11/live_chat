package com.chat_server.user.repository.impl;

import com.chat_server.friend.entity.QFriend;
import com.chat_server.search.dto.response.SearchUserResponse;
import com.chat_server.user.dto.response.AuthenticatedUser;
import com.chat_server.user.dto.response.UserAuthenticationResponse;
import com.chat_server.user.entity.QUser;
import com.chat_server.user.enums.UserStatus;
import com.chat_server.user.repository.UserRepositoryCustom;
import com.chat_server.userprofile.enrtity.QUserProfile;
import com.chat_server.userprofileImage.entity.QUserProfileImage;
import com.querydsl.core.types.Projections;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.util.Optional;

/**
 * packageName    : com.chat_server.user.repository.impl
 * fileName       : UserRepositoryCustomImpl
 * author         : parkminsu
 * date           : 25. 2. 28.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 2. 28.        parkminsu       최초 생성
 */

public class UserRepositoryCustomImpl extends QuerydslRepositorySupport implements UserRepositoryCustom {
    private final QUser qUser = QUser.user;
    private final QUserProfile qUserProfile= QUserProfile.userProfile;
    private final QFriend qFriend = QFriend.friend;
    private final QUserProfileImage qUserProfileImage = QUserProfileImage.userProfileImage;
    public UserRepositoryCustomImpl() {
        super(QUser.class);
    }


    @Override
    public Optional<UserAuthenticationResponse> getUserByUserId(String userId) {

        return Optional.ofNullable(
                from(qUser)
                        .select(Projections.constructor(
                                UserAuthenticationResponse.class,
                                qUser.status
                        ))
                        .where(qUser.status.eq(UserStatus.ACTIVE).and(qUser.uuid.eq(userId)))
                        .fetchOne()
        );
    }

    /**
     * 유저 권한 확인 메서드
     * @param userId 식별할 수 있는 id
     * @return 권한 체크된
     */
    @Override
    public Optional<AuthenticatedUser> authorizeUserByUserId(String userId, String roleName) {

        // todo 활성이라는 데이터를 하드 코딩 하는 게 아니라 yml 파일 등 따로 변수로 관리
        Long id  =
                from(qUser)
                        .select(
                                qUser.id
                        )
                        .where(qUser.uuid.eq(userId).and(qUser.status.eq(UserStatus.ACTIVE)))
                        .fetchOne();

        if (id == null) {
            return Optional.empty();
        }
        return Optional.of(new AuthenticatedUser(id,"유저"));

    }

    @Override
    public SearchUserResponse getSearchUserByUserId(Long userId, String searchUserId) {
        return
            from(qUser)
                .select(Projections.constructor(
                    SearchUserResponse.class,
                    qUser.uuid,
                    qUser.name,
                        qUserProfileImage.imageUrl,
                        qFriend.id.isNotNull()
                ))
                    .innerJoin(qUserProfile).on(qUserProfile.user.eq(qUser))
                    .leftJoin(qUserProfileImage).on(qUserProfileImage.userProfile.eq(qUserProfile).and(qUserProfileImage.current.isTrue()))
                    .leftJoin(qFriend).on(
                            qFriend.user.id.eq(userId)
                                    .and(qFriend.friendUser.inputId.eq(searchUserId))
                    )
                .where(qUser.inputId.eq(searchUserId)
                    .and(qUser.status.eq(UserStatus.ACTIVE)
                    ))
                .fetchOne();
    }

    @Override
    public Optional<Long> getUserIdByUserUuid(String userUuid) {
        return Optional.ofNullable(
                from(qUser)
                        .select(qUser.id)
                        .where(qUser.uuid.eq(userUuid))
                        .fetchOne()
        );
    }


    @Override
    public SearchUserResponse searchUserByFriendCodeOrInputId(Long viewerUserId, String keyword) {
        return from(qUser)
                .select(Projections.constructor(
                        SearchUserResponse.class,
                        qUser.uuid,
                        qUser.nickname,
                        qUser.friendCode,
                        qUserProfileImage.imageUrl,
                        qFriend.id.isNotNull(),
                        qUser.id.eq(viewerUserId)
                ))
                .innerJoin(qUserProfile).on(qUserProfile.user.eq(qUser))
                .leftJoin(qUserProfileImage).on(
                        qUserProfileImage.userProfile.eq(qUserProfile)
                                .and(qUserProfileImage.current.isTrue())
                )
                .leftJoin(qFriend).on(
                        qFriend.user.id.eq(viewerUserId)
                                .and(qFriend.friendUser.eq(qUser))
                )
                .where(
                        qUser.status.eq(UserStatus.ACTIVE)
                                .and(
                                        qUser.friendCode.eq(keyword)
                                                .or(qUser.inputId.eq(keyword))
                                )
                )
                .fetchOne();
    }


}
