package com.chat_server.chatroom.service.impl;

import com.chat_server.chatlist.service.ChatListService;
import com.chat_server.chatmessage.dto.response.ChatMessageItemResponse;
import com.chat_server.chatmessage.service.ChatMessageService;
import com.chat_server.chatroom.dto.response.ChatRoomEnterResponse;
import com.chat_server.chatroom.dto.response.ChatRoomResult;
import com.chat_server.chatroom.dto.response.ChatRoomSummaryResponse;
import com.chat_server.chatroom.service.ChatRoomFacadeService;
import com.chat_server.chatroom.service.ChatRoomQueryService;
import com.chat_server.chatroom.service.ChatRoomService;
import com.chat_server.chatroommember.service.ChatRoomMemberService;
import com.chat_server.common.cursor.ChatMessageCursorCodec;
import com.chat_server.common.cursor.ChatMessageCursorKey;
import com.chat_server.user.service.UserService;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatRoomFacadeServiceImpl implements ChatRoomFacadeService {

    private final ChatRoomService chatRoomService;
    private final ChatListService chatListService;
    private final ChatRoomMemberService chatRoomMemberService;
    private final ChatMessageService chatMessageService;
    private final ChatRoomQueryService chatRoomQueryService;
    private final UserService userService;

    @Override
    public ChatRoomSummaryResponse getChatRoomSummary(Long roomId, Long userId) {
        log.info("get chat room start");
        log.info("Get chat room by id:{}", roomId);

        return chatRoomService.getChatRoomSummary(roomId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public ChatRoomEnterResponse enterChatRoom(Long roomId, Long userId, String cursor, int limit) {
        chatRoomQueryService.validateMemberOrThrow(roomId, userId);

        int safeLimit = Math.min(Math.max(limit, 1), 100);
        ChatMessageCursorKey decoded = ChatMessageCursorCodec.decode(cursor);

        var room = chatRoomQueryService.getRoomOrThrow(roomId);

        var slice = chatMessageService.getEnterMessagesByCursor(roomId, safeLimit, decoded);
        List<ChatMessageItemResponse> messages = new ArrayList<>(slice.getContent());
        messages.sort((a, b) -> a.createdAt().equals(b.createdAt())
                ? Long.compare(a.messageId(), b.messageId())
                : a.createdAt().compareTo(b.createdAt()));

        String nextCursor = null;
        if (slice.hasNext() && !slice.getContent().isEmpty()) {
            ChatMessageItemResponse last = slice.getContent().get(slice.getContent().size() - 1);
            long lastAtEpochMillis = last.createdAt()
                    .atOffset(ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli();
            nextCursor = ChatMessageCursorCodec.encode(lastAtEpochMillis, last.messageId());
        }

        String title = room.getName() != null ? room.getName() : "";

        return new ChatRoomEnterResponse(
                room.getId(),
                room.getRoomType().name(),
                title,
                messages,
                nextCursor
        );
    }

    @Override
    public ChatRoomResult getOrCreateOneToOneChatRoom(Long userId, String friendUuid) {
        log.info("1:1 chat create start");

        Long friendId = userService.getUserIdByUserUuid(friendUuid);
        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("자기 자신과는 1:1 채팅방을 만들 수 없습니다.");
        }

        log.debug("chat room service createOneToOneChatRoom start");
        ChatRoomResult chatRoomResult = chatRoomService.getOrCreateOneToOneChatRoom(userId, friendId);
        Long roomId = chatRoomResult.roomId();
        log.debug("chat room service createOneToOneChatRoom end");

        chatListService.ensureMembership(roomId, userId);
        chatListService.ensureMembership(roomId, friendId);
        chatRoomMemberService.ensureMembership(userId, roomId);
        chatRoomMemberService.ensureMembership(friendId, roomId);

        log.info("1:1 chat create end");
        return chatRoomResult;
    }
}
