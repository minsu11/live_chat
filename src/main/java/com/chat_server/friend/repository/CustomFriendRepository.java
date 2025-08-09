package com.chat_server.friend.repository;

import com.chat_server.common.cursor.CursorKey;
import com.chat_server.friend.dto.response.UserFriendResponse;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Slice;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface CustomFriendRepository {

    /**
     * @param userId 내 사용자 ID
     * @param limit  페이지 크기
     * @param cursor 커서(마지막 항목의 정렬키) - null이면 첫 페이지
     */
    Slice<UserFriendResponse> findFriendsWithProfileByCursor(Long userId, int limit, @Nullable CursorKey cursor);



}
