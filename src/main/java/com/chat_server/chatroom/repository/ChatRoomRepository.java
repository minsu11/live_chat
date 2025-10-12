package com.chat_server.chatroom.repository;

import com.chat_server.chatroom.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

// chat room repository
public interface ChatRoomRepository extends JpaRepository<Long, ChatRoom> {

}
