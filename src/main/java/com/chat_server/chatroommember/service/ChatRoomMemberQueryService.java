package com.chat_server.chatroommember.service;

import java.util.List;

public interface ChatRoomMemberQueryService {
    List<Long> getMemberUserIds(Long roomId);

}
