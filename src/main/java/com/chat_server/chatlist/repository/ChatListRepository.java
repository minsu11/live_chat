package com.chat_server.chatlist.repository;

import com.chat_server.chatlist.entity.ChatList;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatListRepository extends JpaRepository<ChatList, Long>,ChatListRepositoryCustom {

}
