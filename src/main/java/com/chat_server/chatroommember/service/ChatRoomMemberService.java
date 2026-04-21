package com.chat_server.chatroommember.service;

import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.chatroommember.entity.ChatRoomMember;
import com.chat_server.user.entity.User;

public interface ChatRoomMemberService {
    void ensureMembership(Long userId, Long chatRoomId);

    void leaveRoomMember(Long roomId, Long userId);
}
