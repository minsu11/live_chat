package com.chat_server.chatroommember.service;

import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.chatroommember.dto.response.ChatRoomMemberInfoDto;
import com.chat_server.chatroommember.dto.response.ChatRoomMemberResponse;
import com.chat_server.chatroommember.entity.ChatRoomMember;
import com.chat_server.user.entity.User;

import java.util.List;

public interface ChatRoomMemberService {
    void ensureMembership(Long userId, Long chatRoomId);

    void leaveRoomMember(Long roomId, Long userId);

    List<ChatRoomMemberInfoDto> getChatRoomMemberIds(Long roomId);
    void addMembers(Long roomId, List<String> inviteeUuid, ChatRoom chatRoom);

    List<Long> getRoomMemberIdsByRoomId(Long roomId);

}
