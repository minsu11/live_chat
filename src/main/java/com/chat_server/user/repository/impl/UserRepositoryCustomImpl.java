package com.chat_server.user.repository.impl;

import com.chat_server.user.entity.QUser;
import com.chat_server.user.repository.UserRepositoryCustom;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

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


}
