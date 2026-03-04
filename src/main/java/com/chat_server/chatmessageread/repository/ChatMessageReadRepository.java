package com.chat_server.chatmessageread.repository;


import com.chat_server.chatmessageread.entity.ChatMessageRead;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageReadRepository extends JpaRepository<ChatMessageRead, Long> {
    Optional<ChatMessageRead> findByChatRoomIdAndUserId(Long chatRoomId, Long userId);
}
