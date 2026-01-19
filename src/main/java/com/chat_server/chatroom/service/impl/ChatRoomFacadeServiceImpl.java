package com.chat_server.chatroom.service.impl;

import com.chat_server.chatlist.service.ChatListService;
import com.chat_server.chatroom.dto.response.ChatRoomSummaryResponse;
import com.chat_server.chatroom.service.ChatRoomFacadeService;
import com.chat_server.chatroom.service.ChatRoomService;
import com.chat_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatRoomFacadeServiceImpl implements ChatRoomFacadeService {

    private final ChatRoomService chatRoomService;

    private final ChatListService chatListService;



    private final UserService userService;

    @Override
    public ChatRoomSummaryResponse getChatRoomSummary(Long roomId, Long userId) {
        // chat room name
        log.info("get chat room start");
        log.info("Get chat room by id:{}", roomId);
        // summary 데이터가 뭐가 있나?

        // summary
        return chatRoomService.getChatRoomSummary(roomId, userId);
    }

    @Override
    public void createOneToOneChatRoom(Long userId, String friendUuid) {
        log.info("1:1 chat create start");
        Long friendId= userService.getUserIdByUserUuid(friendUuid);

        // room hash string setting

        // chat room 생성
        Long roomId = chatRoomService.createOneToOneChatRoom(userId, friendId);

        // chat list 즉 ensure chat list 할 예정
        chatListService.ensureMembership(roomId, userId, friendId);
        chatListService.ensureMembership(roomId, friendId, friendId);
        log.info("1:1 chat create end");

    }

}
