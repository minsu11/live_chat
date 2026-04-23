package com.chat_server.chatroom.service.impl;

import com.chat_server.chatlist.dto.response.ChatListItemResponse;
import com.chat_server.chatlist.entity.ChatList;
import com.chat_server.chatlist.service.ChatListService;
import com.chat_server.chatmessage.dto.response.ChatMessageCatchUpResponse;
import com.chat_server.chatmessage.dto.response.ChatMessageItemResponse;
import com.chat_server.chatmessage.dto.response.ChatMessageResponse;
import com.chat_server.chatmessage.dto.response.ChatMessageSenderResponse;
import com.chat_server.chatmessage.enums.MessageType;
import com.chat_server.chatmessage.service.ChatMessageFacadeService;
import com.chat_server.chatmessage.service.ChatMessageService;
import com.chat_server.chatread.service.ChatReadFacadeService;
import com.chat_server.chatlist.dto.event.ChatListUpsertEvent;
import com.chat_server.chatroom.dto.request.CreateGroupChatRoomRequest;
import com.chat_server.chatroom.dto.response.ChatRoomEnterResponse;
import com.chat_server.chatroom.dto.response.ChatRoomResult;
import com.chat_server.chatroom.dto.response.ChatRoomSummaryResponse;
import com.chat_server.chatroom.dto.response.CreateChatRoomResponse;
import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.chatroom.resolver.ChatRoomDisplayResolver;
import com.chat_server.chatroom.service.ChatRoomFacadeService;
import com.chat_server.chatroom.service.ChatRoomQueryService;
import com.chat_server.chatroom.service.ChatRoomService;
import com.chat_server.chatroommember.dto.response.ChatRoomMemberInfoDto;
import com.chat_server.chatroommember.dto.response.ChatRoomMemberResponse;
import com.chat_server.chatroommember.service.ChatRoomMemberService;
import com.chat_server.common.cursor.ChatMessageCursorCodec;
import com.chat_server.common.cursor.ChatMessageCursorKey;
import com.chat_server.common.mapper.ChatListUpsertEventMapper;
import com.chat_server.user.entity.User;
import com.chat_server.user.service.UserDisplayNameService;
import com.chat_server.user.service.UserService;
import com.chat_server.websocket.broadcaster.chatmessage.ChatListEventBroadcaster;

import java.time.ZoneOffset;
import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.parameters.P;
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
    private final ChatListEventBroadcaster chatListEventBroadcaster;
    private final ChatRoomDisplayResolver chatRoomDisplayResolver;
    private final ChatListUpsertEventMapper chatListUpsertEventMapper;
    private final ChatMessageFacadeService chatMessageFacadeService;
    private final ObjectMapper objectMapper;


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
        chatRoomQueryService.validateMemberOrThrow(roomId, userId);
        ChatRoom room = chatRoomQueryService.getRoomOrThrow(roomId);
        return chatRoomDisplayResolver.resolveSummary(roomId, userId, room);
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
     * @param limit  페이지 크기. 1~100 범위로 보정하여 사용한다.
     * @return 채팅방 진입 응답 DTO.
     * 채팅방 기본 정보(roomId, roomType, title), 프론트 공통 메시지 응답 목록(messages),
     * 다음 페이지 커서(nextCursor)를 포함한다.
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
        // 읽음 처리
        if (isInitialEnter) {
            chatReadFacadeService.markAsReadOnEnter(roomId, userId, room.getLastMessageId());
        }

        var slice = chatMessageService.getEnterMessagesByCursor(roomId, safeLimit, decoded);

        List<ChatMessageItemResponse> messageItems = slice.getContent();

        List<Long> senderIds = messageItems.stream()
                .map(ChatMessageItemResponse::senderId)
                .distinct().toList();

        Map<Long, String> displayNameCache = userDisplayNameService.resolveDisplayNamesBulk(
                userId,
                senderIds
        );

        List<ChatMessageResponse> messages = messageItems.stream()
                .map(item -> {
                    // Map에 커스텀 닉네임이 있으면 쓰고, 없으면 원래 닉네임(senderNickname) 사용
                    String displayNickname = displayNameCache.getOrDefault(item.senderId(), item.senderNickname());
                    log.info("item id: {}, item type: {}",item.messageId(), item.messageType());
                    return toChatMessageResponse(item, roomId, userId, displayNickname);
                })
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

        String title = chatRoomDisplayResolver.resolveTitle(roomId, userId, room);


        return new ChatRoomEnterResponse(
                room.getId(),
                room.getRoomType().name(),
                title,
                messages,
                nextCursor,
                chatListService.getMuted(roomId,userId)
        );
    }

    /**
     * 1:1 채팅방을 조회/생성하고 멤버십을 보장한다.
     *
     * @param userId     요청 유저 ID
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
        List<ChatMessageItemResponse> messageItems = slice.getContent();

        List<Long> senderIds = messageItems.stream()
                .map(ChatMessageItemResponse::senderId)
                .distinct().toList();

        Map<Long, String> customNameMap = userDisplayNameService.resolveDisplayNamesBulk(
                userId, // 조회하는 사람 (나)
                senderIds// 메세지를 보낸 사람들 목록
        );

        List<ChatMessageResponse> messages = messageItems.stream()
                .map(item -> {
                    String finalDisplayName = customNameMap.getOrDefault(item.senderId(), item.senderNickname());
                    return toChatMessageResponse(item, roomId, userId, finalDisplayName);
                })
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

        String title = chatRoomDisplayResolver.resolveTitle(roomId, userId, room);

        return new ChatRoomEnterResponse(
                room.getId(),
                room.getRoomType().name(),
                title,
                messages,
                nextCursor,
                chatListService.getMuted(roomId,userId)
        );
    }

    @Override
    public CreateChatRoomResponse createGroupChatRoom(Long requesterUserId, CreateGroupChatRoomRequest request) {
        log.info("createGroupChatRoom start requesterUserId={}", requesterUserId);

        if (request == null) {
            throw new IllegalArgumentException("그룹 채팅방 요청 값이 없습니다.");
        }

        List<String> requestedMemberUuids = request.memberUuids();

        if (requestedMemberUuids == null || requestedMemberUuids.isEmpty()) {
            throw new IllegalArgumentException("그룹 채팅방에 초대할 멤버를 선택해야 합니다.");
        }

        // 1. uuid 정리: null/blank 제거 + trim + 중복 제거
        List<String> normalizedUuids = requestedMemberUuids.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(uuid -> !uuid.isBlank())
                .distinct()
                .toList();

        if (normalizedUuids.size() < 2) {
            throw new IllegalArgumentException("그룹 채팅방은 본인을 제외한 2명 이상의 멤버가 필요합니다.");
        }

        // 2. uuid -> userId 변환
        // 현재 UserService가 단건만 있다면 일단 반복 호출로 맞춘다.
        List<Long> targetUserIds = normalizedUuids.stream()
                .map(userService::getUserIdByUserUuid)
                .distinct()
                .toList();

        // 3. 자기 자신 제거
        List<Long> filteredTargetUserIds = targetUserIds.stream()
                .filter(targetUserId -> !targetUserId.equals(requesterUserId))
                .distinct()
                .toList();

        if (filteredTargetUserIds.size() < 2) {
            throw new IllegalArgumentException("그룹 채팅방은 본인을 제외한 2명 이상의 멤버가 필요합니다.");
        }

        // 4. 최종 참여자 구성: 생성자 + 초대 대상
        LinkedHashSet<Long> participantUserIds = new LinkedHashSet<>();
        participantUserIds.add(requesterUserId);
        participantUserIds.addAll(filteredTargetUserIds);

        if (participantUserIds.size() < 3) {
            throw new IllegalArgumentException("그룹 채팅방은 최소 3명 이상이어야 합니다.");
        }

        String title = normalizeRoomTitle(request.title());

        // 5. 채팅방 생성
        ChatRoom createdRoom = chatRoomService.createGroupChatRoom(title, requesterUserId);

        Long roomId = createdRoom.getId();

        // 6. 멤버십 생성
        for (Long participantUserId : participantUserIds) {
            chatRoomMemberService.ensureMembership(participantUserId, roomId);
        }

        // 7. chat_list row 생성
        for (Long participantUserId : participantUserIds) {
            chatListService.ensureMembership(roomId, participantUserId);
        }

        broadcastChatListUpsertEvents(roomId, participantUserIds);

        log.info("createGroupChatRoom end roomId={}, participantCount={}", roomId, participantUserIds.size());

        return new CreateChatRoomResponse(
                roomId,
                createdRoom.getRoomType().name(),
                title
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ChatMessageCatchUpResponse getMessagesAfter(Long roomId, Long userId, Long afterMessageId, int limit) {
        chatRoomQueryService.validateMemberOrThrow(roomId, userId);

        int safeLimit = Math.min(Math.max(limit, 1), 100);

        var room = chatRoomQueryService.getRoomOrThrow(roomId);

        var slice = chatMessageService.getMessagesAfter(roomId, afterMessageId, safeLimit);

        List<ChatMessageItemResponse> messageItems = slice.getContent();

        List<Long> senderIds = messageItems.stream()
                .map(ChatMessageItemResponse::senderId)
                .distinct().toList();

        Map<Long, String> displayNameCache = userDisplayNameService.resolveDisplayNamesBulk(
                userId,
                senderIds
        );

        List<ChatMessageResponse> messages = messageItems.stream()
                .map(item -> {
                    // Map에 커스텀 닉네임이 있으면 쓰고, 없으면 원래 닉네임(senderNickname) 사용
                    String displayNickname = displayNameCache.getOrDefault(item.senderId(), item.senderNickname());
                    return toChatMessageResponse(item, roomId, userId, displayNickname);
                })
                .sorted((a, b) -> a.createdAt().equals(b.createdAt())
                        ? Long.compare(a.messageId(), b.messageId())
                        : a.createdAt().compareTo(b.createdAt()))
                .toList();

        Long lastMessageId = messages.isEmpty()
                ? afterMessageId
                : messages.get(messages.size() - 1).messageId();

        return new ChatMessageCatchUpResponse(
                room.getId(),
                messages,
                slice.hasNext(),
                lastMessageId
        );
    }

    @Override
    public List<ChatRoomMemberResponse> getChatroomMembers(Long roomId, Long userId) {

        List<ChatRoomMemberInfoDto> memberInfos = chatRoomMemberService.getChatRoomMemberIds(roomId);

        List<Long> memberUserIds = memberInfos.stream()
                .map(ChatRoomMemberInfoDto::userId)
                .toList();

        Map<Long, String> displayNameCache = userDisplayNameService.resolveDisplayNamesBulk(userId, memberUserIds);

        return memberInfos.stream()
                .map(info -> {
                    // 커스텀 닉네임이 있으면 쓰고, 없으면 DTO에 있는 원래 닉네임 사용
                    String finalName = displayNameCache.getOrDefault(info.userId(), info.nickname());

                    return new ChatRoomMemberResponse(
                            info.uuid(),
                            finalName,
                            info.profileUrl(),
                            info.userId().equals(userId) // isMe 판단
                    );
                })
                .toList();
    }

    @Override
    public void inviteMembers(Long roomId, Long inviterId, List<String> inviteeUuids) {
        chatRoomQueryService.validateMemberOrThrow(roomId, inviterId);
        ChatRoom chatRoom = chatRoomQueryService.getRoomOrThrow(roomId);

        chatRoomMemberService.addMembers(roomId,inviteeUuids, chatRoom);

        List<User> invitees = userService.getUserIdByUserUuids(inviteeUuids);
        List<Long> inviteeIds = invitees.stream().map(User::getId).toList();

        chatListService.ensureMembershipsBulk(roomId,inviteeIds);

        User inviter = userService.getUserById(inviterId);


        Map<String, Object> payload = new HashMap<>();

        payload.put("inviter", Map.of(
                "uuid",inviter.getUuid(),
                "name", inviter.getNickname()
        ) );

        // 2-2. 피초대자들 정보 리스트 (UUID + 원래 닉네임)
        List<Map<String, String>> inviteeInfos = invitees.stream()
                .map(u -> Map.of("uuid", u.getUuid(), "name", u.getNickname()))
                .toList();
        payload.put("invitees", inviteeInfos);

        // 3. JSON 문자열로 직렬화 (ObjectMapper 활용)
        String content = "";
        try {
            // 💡 클래스 상단에 private final ObjectMapper objectMapper; 주입 필요
            content = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("시스템 초대 메시지 JSON 변환 실패", e);
            // 에러 발생 시 Fallback으로 단순 문자열 저장
            content = String.format("{\"fallback\": \"%s님이 %d명을 초대했습니다.\"}", inviter.getNickname(), invitees.size());
        }
        chatMessageFacadeService.saveAndBroadcastSystemMessage(roomId, inviterId, MessageType.SYSTEM_INVITE, content);
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
     * @param item             채팅방 진입 조회용 메시지 DTO
     * @param roomId           채팅방 ID
     * @param viewerUserId     현재 메시지를 조회 중인 사용자 ID
     * @param displayNickname  화면에 나오는 이름
     * @return 프론트 공통 메시지 응답 DTO
     */
    private ChatMessageResponse toChatMessageResponse(
            ChatMessageItemResponse item,
            Long roomId,
            Long viewerUserId,
            String displayNickname
    ) {
        String content = item.content();
        if(item.messageType().equalsIgnoreCase("SYSTEM_LEAVE")){
            content = displayNickname + content;
        }

        return new ChatMessageResponse(
                item.messageId(),
                roomId,
                item.messageType(),
                new ChatMessageSenderResponse(
                        item.senderUuid(),
                        displayNickname,
                        item.profileImageUrl()
                ),
                content,
                item.createdAt(),
                item.senderId().equals(viewerUserId),
                item.unreadCount()
        );
    }

    private void broadcastChatListUpsertEvents(Long roomId, Set<Long> participantUserIds) {
        for (Long participantUserId : participantUserIds) {
            ChatListItemResponse item = chatListService.getChatListItem(roomId, participantUserId);
            ChatListUpsertEvent event = chatListUpsertEventMapper.toChatListUpsertEvent(item);
            chatListEventBroadcaster.broadcastUpsertToUser(participantUserId, event);
        }
    }

    private String normalizeRoomTitle(String title) {
        if (title == null) {
            return null;
        }

        String trimmed = title.trim();

        if (trimmed.isBlank()) {
            return null;
        }

        String normalized = trimmed.replaceAll("[,\\s]+", "");
        if (normalized.isBlank()) {
            return null;
        }

        return trimmed;
    }
}
