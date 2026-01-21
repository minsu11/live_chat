package com.chat_server.userblock.repository.impl;

import com.chat_server.userblock.entity.QUserBlock;
import com.chat_server.userblock.entity.UserBlock;
import com.chat_server.userblock.repository.UserBlockRepositoryCustom;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

public class UserBlockRepositoryCustomImpl extends QuerydslRepositorySupport implements
    UserBlockRepositoryCustom {
    private final QUserBlock qUserBlock  = QUserBlock.userBlock;
    public UserBlockRepositoryCustomImpl() {
        super(UserBlock.class);
    }


    @Override
    public boolean existsByUserBlock(Long blockerId, Long userId) {
        Long result = from(qUserBlock)
            .select(qUserBlock.id)
            .where(qUserBlock.blocker.id.eq(blockerId)
                .and(qUserBlock.blocked.id.eq(userId)))
            .fetchOne();


        return result !=null;

    }
}
