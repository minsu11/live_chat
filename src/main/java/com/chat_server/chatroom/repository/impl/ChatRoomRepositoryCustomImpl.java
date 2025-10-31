package com.chat_server.chatroom.repository.impl;

import com.chat_server.chatroom.entity.QChatRoom;
import com.chat_server.chatroom.repository.ChatRoomRepositoryCustom;
import com.chat_server.chattype.entity.ChatType;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.util.Optional;

public class ChatRoomRepositoryCustomImpl extends QuerydslRepositorySupport implements ChatRoomRepositoryCustom {
    private final QChatRoom qChatRoom = QChatRoom.chatRoom;

    public ChatRoomRepositoryCustomImpl() {
        super(QChatRoom.class);
    }

    @Override
    public Optional<Long> findRoomIdByParticipantsHashAndChatType(String participantsHash, ChatType chatType) {
        return Optional.ofNullable(
                from(qChatRoom)
                        .select(qChatRoom.id)
                        .where(qChatRoom.chatType.eq(chatType)
                                .and(qChatRoom.participantsHash.eq(participantsHash)))
                .fetchOne()
        );
    }
}
