package com.chat_server.friend.repository.impl;


import com.chat_server.common.cursor.CursorKey;
import com.chat_server.friend.dto.response.UserFriendResponse;
import com.chat_server.friend.entity.QFriend;
import com.chat_server.friend.repository.FriendRepositoryCustom;
import com.chat_server.user.entity.QUser;
import com.chat_server.userprofile.enrtity.QUserProfile; // ← 패키지명/오타 확인!
import com.chat_server.userprofile.url.entity.QUserProfileUrl;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.util.List;
@Slf4j
public class FriendRepositoryCustomImpl extends QuerydslRepositorySupport implements
    FriendRepositoryCustom {
    private final QFriend qFriend = QFriend.friend1;
    private final QUserProfile qUserProfile = QUserProfile.userProfile;
    private final QUser qUser = QUser.user;
    private final QUserProfileUrl qUserProfileUrl = QUserProfileUrl.userProfileUrl;

    public FriendRepositoryCustomImpl() {
        super(QFriend.class);
    }

    /**
     * 커서 기반 페이지네이션
     * - 정렬: LOWER(u.userName) ASC, u.id ASC
     * - 커서: (lastLowerName, lastId)  ← 예: (bob, 4)  → 즉, Bob(4) '다음부터'
     */
    @Override
    public Slice<UserFriendResponse> getFriendsWithProfileByCursor(
            Long userId, int limit, @Nullable CursorKey cursor) {

        // WHERE f.user_id = :me
        BooleanBuilder where = new BooleanBuilder().and(qFriend.user.id.eq(userId));

        // cursor가 있으면 “그 지점 다음부터” 조건 추가
        // (LOWER(name) > :lastLowerName) OR
        // (LOWER(name) = :lastLowerName AND id > :lastId)

        if (cursor != null && cursor.lastLowerName() != null && cursor.lastId() != null) {
            where.and(
                    qUser.userName.lower().gt(cursor.lastLowerName())
                            .or(
                                    qUser.userName.lower().eq(cursor.lastLowerName())
                                            .and(qUser.id.gt(cursor.lastId()))
                            )
            );
        }

        // Bob(4) 다음부터  limit + 1개 가져와서 hasNext 판별 (그림의 점선 오른쪽부터!)
        List<UserFriendResponse> rows = from(qFriend)
                .leftJoin(qFriend.friend, qUser)
                .leftJoin(qUserProfile).on(qUserProfile.user.eq(qUser))
                .leftJoin(qUserProfileUrl).on(qUserProfileUrl.userProfile.eq(qUserProfile))
                .where(where)
                .orderBy(qUser.userName.lower().asc(), qUser.id.asc())   // Alice(1) → Alice(3) → Bob(4) → Carol(7) ...
                .limit(limit + 1)                                // 한 개 더 가져와서 다음 페이지 존재 여부 확인
                .select(Projections.constructor(
                        UserFriendResponse.class,
                        qUser.id, qUser.userName, qUserProfileUrl.imageUrl
                ))
                .fetch();

        boolean hasNext = rows.size() > limit;
        if (hasNext) {
            rows = rows.subList(0, limit);          // 응답은 정확히 limit개만
        }
        return new SliceImpl<>(rows, PageRequest.of(0, limit), hasNext);
    }
}

