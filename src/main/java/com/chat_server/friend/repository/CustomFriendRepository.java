package com.chat_server.friend.repository;

import com.chat_server.friend.dto.response.UserFriendResponse;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface CustomFriendRepository extends FriendRepository {
    Optional<List<UserFriendResponse>> findUserFriendResponseByUserId(Long userId);
}
