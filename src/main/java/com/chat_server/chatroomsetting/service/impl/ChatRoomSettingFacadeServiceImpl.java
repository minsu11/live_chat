package com.chat_server.chatroomsetting.service.impl;

import com.chat_server.chatlist.dto.reqeust.ChatRoomNotificationUpdateRequest;
import com.chat_server.chatlist.dto.response.ChatRoomNotificationUpdateResponse;
import com.chat_server.chatlist.entity.ChatList;
import com.chat_server.chatlist.service.ChatListService;
import com.chat_server.chatmessage.enums.MessageType;
import com.chat_server.chatmessage.service.ChatMessageFacadeService;
import com.chat_server.chatroom.service.ChatRoomService;
import com.chat_server.chatroommember.service.ChatRoomMemberService;
import com.chat_server.chatroomsetting.dto.request.ChatRoomDisplayNameUpdateRequest;
import com.chat_server.chatroomsetting.dto.response.ChatRoomNameUpdateResponse;
import com.chat_server.chatroomsetting.service.ChatRoomSettingFacadeService;
import com.chat_server.user.entity.User;
import com.chat_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatRoomSettingFacadeServiceImpl implements ChatRoomSettingFacadeService {
    private final ChatListService chatListService;
    private final ChatRoomService chatRoomService;
    private final ChatMessageFacadeService messageFacadeService;
    private final ChatRoomMemberService chatRoomMemberService;


    @Override
    public ChatRoomNameUpdateResponse updateChatRoomName(Long userId, Long roomId, ChatRoomDisplayNameUpdateRequest request) {
        log.info("update chatroom name start");
        String newName = request.displayName();
        ChatList chatList = chatListService.updateCustomRoomName(userId, roomId, newName);
        return ChatRoomNameUpdateResponse.of(roomId,chatList.getCustomName());
    }

    @Override
    public ChatRoomNotificationUpdateResponse updateNotification(Long roomId, Long userId, ChatRoomNotificationUpdateRequest request) {
        ChatList updatedChatList = chatListService.updateMutedStatus(roomId, userId, request.muted());

        return ChatRoomNotificationUpdateResponse.of(roomId, updatedChatList.isMuted());    }


    // 방 떠나기
    @Override
    public void leaveRoom(Long roomId, Long userId) {
        log.info("leave room service start");
        chatListService.leaveChatRoom(roomId, userId);

        chatRoomMemberService.leaveRoomMember(roomId,userId);

        chatRoomService.decrementParticipantCount(roomId);

        // 추 후에 변경
        messageFacadeService.saveAndBroadcastSystemMessage(roomId, userId, MessageType.SYSTEM_LEAVE, "님이 나갔습니다.");

    }
}
