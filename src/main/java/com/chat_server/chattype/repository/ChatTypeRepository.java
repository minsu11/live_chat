package com.chat_server.chattype.repository;

import com.chat_server.chattype.entity.ChatType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatTypeRepository extends JpaRepository<ChatType, Long> {

}
