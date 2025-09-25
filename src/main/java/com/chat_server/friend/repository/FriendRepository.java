package com.chat_server.friend.repository;

import com.chat_server.friend.entity.Friend;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FriendRepository extends JpaRepository<Friend, Long>, FriendRepositoryCustom {

}
