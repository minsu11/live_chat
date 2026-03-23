package com.chat_server.chatroom.service.impl;

import com.chat_server.chatlist.service.ChatListService;
import com.chat_server.chatmessage.dto.response.ChatMessageItemResponse;
import com.chat_server.chatmessage.service.ChatMessageService;
import com.chat_server.chatroom.dto.response.ChatRoomEnterResponse;
import com.chat_server.chatroom.dto.response.ChatRoomResult;
import com.chat_server.chatroom.dto.response.ChatRoomSummaryResponse;
import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.chatroom.enums.RoomType;
import com.chat_server.chatroom.service.ChatRoomFacadeService;
import com.chat_server.chatroom.service.ChatRoomQueryService;
import com.chat_server.chatroom.service.ChatRoomService;
import com.chat_server.chatroommember.service.ChatRoomMemberService;
import com.chat_server.common.cursor.ChatMessageCursorCodec;
import com.chat_server.common.cursor.ChatMessageCursorKey;
import com.chat_server.user.service.UserDisplayNameService;
import com.chat_server.user.service.UserService;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    private final UserDisplayNameService userDisplayNameService;

    /**
     * 채팅방 summary 정보를 조회한다.
     *
     * @param roomId 채팅방 ID
     * @param userId 요청 유저 ID
     * @return 채팅방 요약 DTO
     */
    @Override
    public ChatRoomSummaryResponse getChatRoomSummary(Long roomId, Long userId) {
        log.info("get chat room start");
        log.info("Get chat room by id:{}", roomId);

        return chatRoomService.getChatRoomSummary(roomId, userId);
    }

    /**
     * 채팅방 진입 시 메시지 커서 페이지를 조회한다.
     *
     * @param roomId 채팅방 ID
     * @param userId 요청 유저 ID
     * @param cursor 메시지 커서(Base64)
     * @param limit 페이지 크기
     * @return 채팅방 진입 응답(room 정보, 메시지, nextCursor)
     *
     * <p>예외 상황:
     * <ul>
     *   <li>요청 유저가 채팅방 멤버가 아니면 예외</li>
     *   <li>roomId가 유효하지 않으면 예외</li>
     *   <li>cursor가 깨져 있어도 decode는 null을 반환하며 첫 페이지처럼 동작</li>
     * </ul>
     */
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

        String title = resolveEnterTitle(roomId, userId, room);

        return new ChatRoomEnterResponse(
                room.getId(),
                room.getRoomType().name(),
                title,
                messages,
                nextCursor
        );
    }


    /**
     * 채팅방 진입 응답에서 사용할 title을 room type 정책에 맞춰 결정한다.
     *
     * <p>규칙:
     * <ul>
     *   <li>DM: 요청 사용자 기준 상대 표시 이름(친구 별칭 > 상대 기본 닉네임 > room title)</li>
     *   <li>GROUP: 요청 사용자가 설정한 채팅방 커스텀 이름 > room title</li>
     *   <li>OPEN: room title</li>
     * </ul>
     *
     * @param roomId 채팅방 ID
     * @param userId 요청 사용자 ID
     * @param room 채팅방 엔티티
     * @return room type 정책이 반영된 표시 제목
     */
    private String resolveEnterTitle(Long roomId, Long userId, ChatRoom room) {
        String roomTitle = room.getName() != null ? room.getName() : "";

        return switch (room.getRoomType()) {
            case DM -> resolveDmTitle(roomId, userId, roomTitle);
            case GROUP -> resolveGroupTitle(roomId, userId, roomTitle);
            case OPEN -> roomTitle;
        };
    }

    /**
     * DM 방 title을 계산한다.
     *
     * @param roomId 채팅방 ID
     * @param userId 요청 사용자 ID
     * @param roomTitle 채팅방 기본 제목
     * @return DM 표시 제목
     */
    private String resolveDmTitle(Long roomId, Long userId, String roomTitle) {
        Long partnerId = chatRoomQueryService.getMemberId(roomId, userId);

        return userDisplayNameService.resolveDisplayName(partnerId, userId)
                .orElse(roomTitle);
    }

    /**
     * GROUP 방 title을 계산한다.
     *
     * @param roomId 채팅방 ID
     * @param userId 요청 사용자 ID
     * @param roomTitle 채팅방 기본 제목
     * @return GROUP 표시 제목
     */
    private String resolveGroupTitle(Long roomId, Long userId, String roomTitle) {
        Optional<String> customRoomName = chatListService.getCustomRoomName(roomId, userId);
        // TODO room title이 없으면 캐싱 컬럼을 통해서 본인 제외한 채팅방 멤버 이름으로 room title 하기
        return customRoomName.orElse(roomTitle);
    }
    /**
     * 1:1 채팅방을 조회/생성하고 멤버십을 보장한다.
     *
     * @param userId 요청 유저 ID
     * @param friendUuid 상대 유저 UUID
     * @return 채팅방 결과 DTO
     *
     * <p>예외 상황:
     * <ul>
     *   <li>자기 자신과 채팅방 생성 시 IllegalArgumentException</li>
     * </ul>
     */
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
