package com.chat_server.chatlist.service.impl;

import com.chat_server.chatlist.dto.response.ChatRoomListResponse;
import com.chat_server.chatlist.repository.ChatListRepository;
import com.chat_server.chatlist.service.ChatListService;
import com.chat_server.common.cursor.ChatListCursorCodec;
import com.chat_server.common.cursor.ChatListCursorKey;
import com.chat_server.friend.dto.response.CursorPageResponse;
import jakarta.annotation.Nullable;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatListServiceImpl implements ChatListService {
    private final ChatListRepository chatListRepository;

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<ChatRoomListResponse> getChatRoomListsByCursor(Long userId, int limit,
        @Nullable String cursor) {
        // 1) 커서 디코드 (없거나 깨졌으면 null 반환되어 첫 페이지로 처리됨)
        ChatListCursorKey decoded = ChatListCursorCodec.decode(cursor);

        // 2) 레포지토리에서 +1건 조회 (hasNext 판별용)
        Slice<ChatRoomListResponse> slice =
            chatListRepository.getChatRoomListByCursor(userId, limit, decoded);

        // 3) next 커서 생성
        String next = null;
        if (slice.hasNext() && !slice.getContent().isEmpty()) {
            ChatRoomListResponse last = slice.getContent().get(slice.getContent().size() - 1);

            long lastAtEpochMillis = last.lastMessageAt()
                .atOffset(ZoneOffset.UTC)   // DB를 UTC 기준 LocalDateTime으로 본다는 가정
                .toInstant()
                .toEpochMilli();

            next = ChatListCursorCodec.encode(lastAtEpochMillis, last.chatRoomId());
        }

        // 4) 공통 응답 래핑
        return new CursorPageResponse<>(slice.getContent(), next, slice.hasNext());
    }
}
