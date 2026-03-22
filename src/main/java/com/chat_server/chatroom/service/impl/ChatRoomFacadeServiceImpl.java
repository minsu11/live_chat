package com.chat_server.chatroom.service.impl;

import com.chat_server.chatlist.service.ChatListService;
import com.chat_server.chatroom.dto.response.ChatRoomResult;
import com.chat_server.chatroom.dto.response.ChatRoomSummaryResponse;
import com.chat_server.chatroom.service.ChatRoomFacadeService;
import com.chat_server.chatroom.service.ChatRoomService;
import com.chat_server.chatroommember.service.ChatRoomMemberService;
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

    private final ChatRoomMemberService chatRoomMemberService;

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
    public ChatRoomResult getOrCreateOneToOneChatRoom(Long userId, String friendUuid) {
        log.info("1:1 chat create start");

        Long friendId = userService.getUserIdByUserUuid(friendUuid);
        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("자기 자신과는 1:1 채팅방을 만들 수 없습니다.");
        }

        log.debug("chat room service createOneToOneChatRoom start");
        // chat room 생성
        ChatRoomResult chatRoomResult = chatRoomService.getOrCreateOneToOneChatRoom(userId, friendId);
        Long roomId = chatRoomResult.roomId();
        log.debug("chat room service createOneToOneChatRoom end");

        // chat list 즉 ensure chat list 할 예정
        chatListService.ensureMembership(roomId, userId);
        chatListService.ensureMembership(roomId, friendId);
        chatRoomMemberService.ensureMembership(userId,roomId);
        chatRoomMemberService.ensureMembership(friendId,roomId);

        log.info("1:1 chat create end");
        return chatRoomResult;
    }

}
