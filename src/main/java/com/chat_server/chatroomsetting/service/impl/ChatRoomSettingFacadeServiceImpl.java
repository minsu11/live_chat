package com.chat_server.chatroomsetting.service.impl;

import com.chat_server.chatlist.dto.reqeust.ChatRoomNotificationUpdateRequest;
import com.chat_server.chatlist.dto.response.ChatRoomNotificationUpdateResponse;
import com.chat_server.chatlist.entity.ChatList;
import com.chat_server.chatlist.service.ChatListService;
import com.chat_server.chatroomsetting.dto.request.ChatRoomDisplayNameUpdateRequest;
import com.chat_server.chatroomsetting.dto.response.ChatRoomNameUpdateResponse;
import com.chat_server.chatroomsetting.service.ChatRoomSettingFacadeService;
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
}
