package com.chat_server.chatroom.repository.impl;

import com.chat_server.chatroom.entity.QChatRoom;
import com.chat_server.chatroom.repository.ChatRoomRepositoryCustom;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

public class ChatRoomRepositoryCustomImpl extends QuerydslRepositorySupport implements ChatRoomRepositoryCustom {
    private final QChatRoom qChatRoom = QChatRoom.chatRoom;

    public ChatRoomRepositoryCustomImpl() {
        super(QChatRoom.class);
    }


}
