package com.chat_server.chatroommember.repository;

import com.chat_server.chatroommember.dto.response.ChatRoomMemberInfoDto;

import java.util.List;

public interface ChatRoomMemberRepositoryCustom {
    List<ChatRoomMemberInfoDto> findMemberInfosByRoomId(Long roomId);

}
