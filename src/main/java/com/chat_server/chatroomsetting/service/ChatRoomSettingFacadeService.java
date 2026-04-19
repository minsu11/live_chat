package com.chat_server.chatroomsetting.service;

import com.chat_server.chatlist.dto.reqeust.ChatRoomNotificationUpdateRequest;
import com.chat_server.chatlist.dto.response.ChatRoomNotificationUpdateResponse;
import com.chat_server.chatroomsetting.dto.request.ChatRoomDisplayNameUpdateRequest;
import com.chat_server.chatroomsetting.dto.response.ChatRoomNameUpdateResponse;

public interface ChatRoomSettingFacadeService {
    ChatRoomNameUpdateResponse updateChatRoomName(Long userId, Long roomId, ChatRoomDisplayNameUpdateRequest request);

    ChatRoomNotificationUpdateResponse updateNotification(Long roomId, Long userId, ChatRoomNotificationUpdateRequest request);
}
