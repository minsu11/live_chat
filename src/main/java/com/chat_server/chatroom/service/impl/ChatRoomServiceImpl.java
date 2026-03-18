package com.chat_server.chatroom.service.impl;

import com.chat_server.chatmessage.entity.ChatMessage;
import com.chat_server.chatroom.dto.response.ChatRoomSummaryResponse;
import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.chatroom.enums.RoomType;
import com.chat_server.chatroom.exception.ChatRoomNotFoundException;
import com.chat_server.chatroom.repository.ChatRoomRepository;
import com.chat_server.chatroom.service.ChatRoomService;
import com.chat_server.friend.dto.response.CursorPageResponse;
import com.chat_server.friend.dto.response.UserFriendResponse;
import com.chat_server.user.entity.User;
import com.chat_server.user.exception.UserNotFoundException;
import com.chat_server.user.repository.UserRepository;
import com.chat_server.util.ChatRoomHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomServiceImpl implements ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

    @Override
    public CursorPageResponse<UserFriendResponse> getFriendsByCursor(
            Long userId,
            int limit,
            @Nullable String cursor
    ) {
        // room list는 대화 목록이 있는 경우만 대화 목록에 끌고 오게 하기
        return null;
    }

    @Override
    @Transactional
    public Long createOneToOneChatRoom(Long userId, Long friendId) {
        String dmKey = ChatRoomHashUtil.createUserPairHash(userId, friendId);

        Long existRoomId = chatRoomRepository.findRoomIdByDmKeyAndRoomType(dmKey, RoomType.DM)
                .orElse(null);

        if (existRoomId != null) {
            return existRoomId;
        }

        User creator = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        try {
            ChatRoom newChatRoom = ChatRoom.builder()
                    .roomType(RoomType.DM)
                    .dmKey(dmKey)
                    .createdBy(creator)
                    .maxPerson(2)
                    .build();

            ChatRoom savedRoom = chatRoomRepository.save(newChatRoom);
            return savedRoom.getId();

        } catch (DataIntegrityViolationException e) {
            return chatRoomRepository.findRoomIdByDmKeyAndRoomType(dmKey, RoomType.DM)
                    .orElseThrow(() -> e);
        }
    }

    @Override
    public ChatRoomSummaryResponse getChatRoomSummary(Long roomId, Long userId) {
        log.info("get chat room summary");
        return chatRoomRepository.findChatRoomSummaryByRoomId(roomId, userId)
                .orElseThrow(ChatRoomNotFoundException::new);
    }

    @Override
    @Transactional
    public void updateLastMessageInfo(ChatRoom chatRoom, ChatMessage chatMessage) {
        Long senderId = chatMessage.getSender().getId();
        Long messageId = chatMessage.getId();
        String preview = chatMessage.getMessageContent();

        chatRoom.updateLastMessageAt(
                senderId,
                messageId,
                preview,
                chatMessage.getCreatedAt()
        );
    }
}