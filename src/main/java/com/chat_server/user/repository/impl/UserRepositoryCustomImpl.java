package com.chat_server.user.repository.impl;

import com.chat_server.user.dto.response.AuthenticatedUser;
import com.chat_server.user.dto.response.UserAuthenticationResponse;
import com.chat_server.user.entity.QUser;
import com.chat_server.user.repository.UserRepositoryCustom;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
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

        return Optional.ofNullable(
                from(qUser)
                        .select(
                                Projections.constructor(
                                        AuthenticatedUser.class,
                                        qUser.id,
                                        Expressions.constant(roleName)
                                )
                        ).fetchOne()



        );
    }
}
