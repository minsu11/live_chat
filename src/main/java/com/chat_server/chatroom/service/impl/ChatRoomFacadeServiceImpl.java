package com.chat_server.chatroom.service.impl;

import com.chat_server.chatlist.service.ChatListService;
import com.chat_server.chatmessage.dto.response.ChatMessageItemResponse;
import com.chat_server.chatmessage.dto.response.ChatMessageResponse;
import com.chat_server.chatmessage.dto.response.ChatMessageSenderResponse;
import com.chat_server.chatmessage.service.ChatMessageService;
import com.chat_server.chatread.service.ChatReadFacadeService;
import com.chat_server.chatread.service.ChatReadService;
import com.chat_server.chatroom.dto.response.ChatRoomEnterResponse;
import com.chat_server.chatroom.dto.response.ChatRoomResult;
import com.chat_server.chatroom.dto.response.ChatRoomSummaryResponse;
import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.chatroom.service.ChatRoomFacadeService;
import com.chat_server.chatroom.service.ChatRoomQueryService;
import com.chat_server.chatroom.service.ChatRoomService;
import com.chat_server.chatroommember.service.ChatRoomMemberService;
import com.chat_server.common.cursor.ChatMessageCursorCodec;
import com.chat_server.common.cursor.ChatMessageCursorKey;
import com.chat_server.user.service.UserDisplayNameService;
import com.chat_server.user.service.UserService;
import java.time.ZoneOffset;
import java.util.*;

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
    private final ChatReadFacadeService chatReadFacadeService;

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
     * 채팅방 진입 시 메시지 커서 페이지를 조회하고, 프론트 공통 응답 형태로 변환하여 반환한다.
     *
     * <p>처리 순서:
     * <ol>
     *   <li>요청 사용자의 채팅방 멤버십을 검증한다.</li>
     *   <li>cursor와 limit를 보정하여 메시지 Slice를 조회한다.</li>
     *   <li>조회한 메시지 조회 DTO를 {@link com.chat_server.chatmessage.dto.response.ChatMessageResponse}
     *       형태로 변환한다.</li>
     *   <li>메시지 발신자 닉네임은 요청 사용자 관점의 표시 이름으로 변환한다.
     *       (친구 커스텀 닉네임 &gt; 발신자 기본 닉네임)</li>
     *   <li>같은 발신자에 대한 표시 이름 계산 중복을 줄이기 위해 메서드 내부에서
     *       요청 범위의 임시 캐시를 사용한다.</li>
     *   <li>프론트 표시 순서를 위해 메시지를 생성 시각 오름차순
     *       (동일 시각이면 messageId 오름차순)으로 정렬한다.</li>
     *   <li>다음 페이지가 존재하면 마지막 메시지 기준으로 nextCursor를 생성한다.</li>
     * </ol>
     *
     * @param roomId 채팅방 ID
     * @param userId 요청 사용자 ID
     * @param cursor 메시지 커서(Base64). null이거나 디코딩 실패 시 첫 페이지처럼 동작한다.
     * @param limit 페이지 크기. 1~100 범위로 보정하여 사용한다.
     * @return 채팅방 진입 응답 DTO.
     *         채팅방 기본 정보(roomId, roomType, title), 프론트 공통 메시지 응답 목록(messages),
     *         다음 페이지 커서(nextCursor)를 포함한다.
     *
     * <p>메시지 응답의 특징:
     * <ul>
     *   <li>messages는 {@link com.chat_server.chatmessage.dto.response.ChatMessageResponse} 목록으로 반환된다.</li>
     *   <li>각 메시지의 sender 정보에는 요청 사용자 기준 표시 닉네임, 프로필 이미지 URL,
     *       본인 메시지 여부(mine)가 반영된다.</li>
     * </ul>
     *
     * <p>예외 상황:
     * <ul>
     *   <li>요청 사용자가 채팅방 멤버가 아니면 예외를 발생시킨다.</li>
     *   <li>roomId가 유효하지 않으면 예외를 발생시킨다.</li>
     *   <li>cursor가 깨져 있어도 decode는 null을 반환하며 첫 페이지처럼 동작한다.</li>
     * </ul>
     */
    @Override
    public ChatRoomEnterResponse enterChatRoom(Long roomId, Long userId, String cursor, int limit) {
        chatRoomQueryService.validateMemberOrThrow(roomId, userId);
        boolean isInitialEnter = (cursor == null || cursor.isBlank());
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        ChatMessageCursorKey decoded = ChatMessageCursorCodec.decode(cursor);

        var room = chatRoomQueryService.getRoomOrThrow(roomId);
        var slice = chatMessageService.getEnterMessagesByCursor(roomId, safeLimit, decoded);

        // 읽음 처리
        if (isInitialEnter) {
            chatReadFacadeService.markAsReadOnEnter(roomId, userId, room.getLastMessageId());
        }

        Map<Long, String> displayNameCache = new HashMap<>();

        List<ChatMessageResponse> messages = slice.getContent().stream()
                .map(item -> toChatMessageResponse(item, roomId, userId, displayNameCache))
                .sorted((a, b) -> a.createdAt().equals(b.createdAt())
                        ? Long.compare(a.messageId(), b.messageId())
                        : a.createdAt().compareTo(b.createdAt()))
                .toList();

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

    @Override
    @Transactional(readOnly = true)
    public ChatRoomEnterResponse getChatRoomMessages(Long roomId, Long userId, String cursor, int limit) {
        chatRoomQueryService.validateMemberOrThrow(roomId, userId);

        int safeLimit = Math.min(Math.max(limit, 1), 100);
        ChatMessageCursorKey decoded = ChatMessageCursorCodec.decode(cursor);

        var room = chatRoomQueryService.getRoomOrThrow(roomId);
        var slice = chatMessageService.getEnterMessagesByCursor(roomId, safeLimit, decoded);

        Map<Long, String> displayNameCache = new HashMap<>();

        List<ChatMessageResponse> messages = slice.getContent().stream()
                .map(item -> toChatMessageResponse(item, roomId, userId, displayNameCache))
                .sorted((a, b) -> a.createdAt().equals(b.createdAt())
                        ? Long.compare(a.messageId(), b.messageId())
                        : a.createdAt().compareTo(b.createdAt()))
                .toList();

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
     * 채팅방 진입 조회용 메시지 DTO를 프론트 공통 메시지 응답 DTO로 변환한다.
     *
     * <p>변환 시 다음 규칙을 적용한다.
     * <ul>
     *   <li>발신자 닉네임은 요청 사용자(viewerUserId) 관점의 표시 이름으로 결정한다.</li>
     *   <li>표시 이름은 메서드 호출 중 전달받은 displayNameCache를 우선 사용하고,
     *       없으면 {@code userDisplayNameService.resolveDisplayName(...)} 결과를 저장하여 재사용한다.</li>
     *   <li>표시 이름 조회 결과가 없으면 조회 DTO의 기본 발신자 닉네임(senderNickname)을 사용한다.</li>
     *   <li>발신자 ID와 요청 사용자 ID가 같으면 sender.mine 값을 true로 설정한다.</li>
     * </ul>
     *
     * @param item 채팅방 진입 조회용 메시지 DTO
     * @param roomId 채팅방 ID
     * @param viewerUserId 현재 메시지를 조회 중인 사용자 ID
     * @param displayNameCache 동일 요청 내 발신자별 표시 이름 재사용을 위한 임시 캐시
     * @return 프론트 공통 메시지 응답 DTO
     */
    private ChatMessageResponse toChatMessageResponse(
            ChatMessageItemResponse item,
            Long roomId,
            Long viewerUserId,
            Map<Long, String> displayNameCache
    ) {
        String displayNickname = displayNameCache.computeIfAbsent(
                item.senderId(),
                senderId -> userDisplayNameService
                        .resolveDisplayName(senderId, viewerUserId)
                        .orElse(item.senderNickname())
        );

        return new ChatMessageResponse(
                item.messageId(),
                roomId,
                item.messageType(),
                new ChatMessageSenderResponse(
                        item.senderUuid(),
                        displayNickname,
                        item.profileImageUrl()
                ),
                item.content(),
                item.createdAt(),
                item.senderId().equals(viewerUserId),
                item.unreadCount()
        );
    }
}
