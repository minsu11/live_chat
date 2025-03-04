package com.chat_server.user.repository.impl;

import com.chat_server.security.dto.UserAuthDto;
import com.chat_server.user.entity.QUser;
import com.chat_server.user.repository.UserRepository;
import com.chat_server.user.repository.UserRepositoryCustom;
import com.querydsl.core.types.Projections;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import javax.swing.text.html.Option;
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

public class UserRepositoryCustomImpl extends QuerydslRepositorySupport implements UserRepositoryCustom{
    private final QUser qUser = QUser.user;
    public UserRepositoryCustomImpl() {
        super(QUser.class);
    }


    @Override
    public Optional<UserAuthDto> findByUserName(String userName) {


        return Optional.ofNullable(
                from(qUser)
                        .select(Projections.constructor(
                                UserAuthDto.class,
                                qUser.userInputId,
                                qUser.userInputPassword
                        ))
                        .where(qUser.userInputId.eq(userName)
                                .and(qUser.userStatus.userStatusName.eq("활성"))
                        ).fetchOne()
        );
    }
}
