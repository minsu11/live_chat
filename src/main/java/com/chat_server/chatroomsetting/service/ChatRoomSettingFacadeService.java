package com.chat_server.chatroomsetting.service;

import com.chat_server.chatroomsetting.dto.request.ChatRoomDisplayNameUpdateRequest;
import com.chat_server.chatroomsetting.dto.response.ChatRoomNameUpdateResponse;

public interface ChatRoomSettingFacadeService {
    ChatRoomNameUpdateResponse updateChatRoomName(Long userId, Long roomId, ChatRoomDisplayNameUpdateRequest request);
}
