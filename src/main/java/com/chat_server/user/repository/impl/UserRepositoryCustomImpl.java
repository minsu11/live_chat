package com.chat_server.user.repository.impl;

import com.chat_server.search.dto.response.SearchUserResponse;
import com.chat_server.user.dto.response.AuthenticatedUser;
import com.chat_server.user.dto.response.UserAuthenticationResponse;
import com.chat_server.user.entity.QUser;
import com.chat_server.user.repository.UserRepositoryCustom;
import com.chat_server.userprofile.enrtity.QUserProfile;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.util.List;
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

    public UserRepositoryCustomImpl() {
        super(QUser.class);
    }


    @Override
    public Optional<UserAuthenticationResponse> getUserByUserId(String userId) {

        return Optional.ofNullable(
                from(qUser)
                        .select(Projections.constructor(
                                UserAuthenticationResponse.class,
                                qUser.userStatus.userStatusName
                        ))
                        .where(qUser.userStatus.userStatusName.eq("활성").and(qUser.userUuid.eq(userId)))
                        .fetchOne()
        );
    }

    /**
     * 유저 권한 확인 메서드
     * @param userId 식별할 수 있는 id
     * @return
     */
    @Override
    public Optional<AuthenticatedUser> authorizeUserByUserId(String userId, String roleName) {

        // todo 활성이라는 데이터를 하드 코딩 하는 게 아니라 yml 파일 등 따로 변수로 관리
        Long id  =
                from(qUser)
                        .select(
                                qUser.id
                        )
                        .where(qUser.userUuid.eq(userId).and(qUser.userStatus.userStatusName.eq("활성")))
                        .fetchOne();

        if (id == null) {
            return Optional.empty();
        }
        return Optional.of(new AuthenticatedUser(id,"유저"));

    }

    @Override
    public List<SearchUserResponse> getSearchUserByUserId(String userId) {

        return
            from(qUser)
                .select(Projections.constructor(
                    SearchUserResponse.class,
                    qUser.userUuid,
                    qUser.userName,
                    qUserProfile.imageUrl
                ))
                .leftJoin(qUserProfile).on(qUserProfile.user.eq(qUser))
                .where(qUser.userInputId.eq(userId)
                    .and(qUser.userStatus.userStatusName.eq("활성")))
                .fetch();
    }


}
