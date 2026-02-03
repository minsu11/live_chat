package com.chat_server.chatroom.service.impl;

import com.chat_server.chatmessage.entity.ChatMessage;
import com.chat_server.chatroom.dto.response.ChatRoomSummaryResponse;
import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.chatroom.exception.ChatRoomNotFoundException;
import com.chat_server.chatroom.repository.ChatRoomRepository;
import com.chat_server.chatroom.service.ChatRoomService;
import com.chat_server.chattype.enumulation.ChatRoomKind;
import com.chat_server.chattype.repository.ChatTypeRepository;
import com.chat_server.friend.dto.response.CursorPageResponse;
import com.chat_server.friend.dto.response.UserFriendResponse;
import com.chat_server.user.repository.UserRepository;
import com.chat_server.util.ChatRoomHashUtil;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ChatTypeRepository chatTypeRepository;

    @Override
    public CursorPageResponse<UserFriendResponse> getFriendsByCursor(Long userId, int limit,
        @Nullable String cursor) {
        // room list는 대화 목록이 있는 경우만 대화 목록에 끌고 오게 하기
        return null;
    }

    @Override
    public Long createOneToOneChatRoom(Long userId, Long friendId) {

        // chat type
        var ref = chatTypeRepository.findByCode(ChatRoomKind.DM.getCode());

        // 식별 해쉬 데이터
        String key = ChatRoomHashUtil.createUserPairHash(userId,friendId);

        Long existRoomId = chatRoomRepository.findRoomIdByParticipantsHashAndChatType(key,ref)
                .orElse(null);
        if (existRoomId != null) {
            return existRoomId;
        }

        // 존재하지 않을떄
        try{

            ChatRoom newChatRoom = ChatRoom.builder()
                    .chatType(ref)
                    .createdAt(LocalDateTime.now())
                    .participantsHash(key)
                    .maxPerson(2)
                    .build() ;
            ChatRoom room = chatRoomRepository.save(newChatRoom);
            return room.getId();
        }catch (DataIntegrityViolationException e){
            return chatRoomRepository.findRoomIdByParticipantsHashAndChatType(key,ref)
                    .orElseThrow(()->e);
        }

    }

    @Override
    public ChatRoomSummaryResponse getChatRoomSummary(Long roomId, Long userId) {
        // room id, title, preview message, date, unread count, role
        log.info("get chat room summary");
        return  chatRoomRepository.findChatRoomSummaryByRoomId(roomId, userId)
                .orElseThrow(ChatRoomNotFoundException::new);
    }

    @Override
    public void updateLastMessageInfo(ChatRoom chatRoom, ChatMessage chatMessage) {
        Long senderId = chatMessage.getUser().getId();
        Long messageId = chatMessage.getId();
        LocalDateTime now = chatMessage.getCreatedAt();
        String preview = chatMessage.getMessageContent();
        chatRoom.updateLastMessageAt(senderId, messageId, preview, now);


    }

}
