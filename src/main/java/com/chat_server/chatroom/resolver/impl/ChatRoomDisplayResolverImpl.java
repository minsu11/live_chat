package com.chat_server.chatroom.resolver.impl;

import com.chat_server.chatlist.repository.ChatListRepository;
import com.chat_server.chatroom.dto.response.ChatRoomSummaryResponse;
import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.chatroom.repository.ChatRoomRepository;
import com.chat_server.chatroom.resolver.ChatRoomDisplayResolver;
import com.chat_server.user.service.UserDisplayNameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRoomDisplayResolverImpl implements ChatRoomDisplayResolver {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatListRepository chatListRepository;
    private final UserDisplayNameService userDisplayNameService;

    @Override
    public String resolveTitle(Long roomId, Long userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방이 없습니다. roomId=" + roomId));

        return resolveTitle(roomId, userId, room);
    }

    /**
     * room type 정책에 따라 표시 제목을 계산한다.
     *
     * <ul>
     *   <li>DM: 상대 표시 이름</li>
     *   <li>GROUP: custom room name > room name > 본인 제외 멤버 이름 조합</li>
     *   <li>OPEN: custom room name > room name > "오픈 채팅방"</li>
     * </ul>
     */
    public String resolveTitle(Long roomId, Long userId, ChatRoom room) {
        String roomTitle = room.getName() == null ? "" : room.getName();

        return switch (room.getRoomType()) {
            case DM -> resolveDmTitle(roomId, userId, roomTitle);
            case GROUP -> resolveGroupTitle(roomId, userId, roomTitle);
            case OPEN -> resolveOpenTitle(roomId, userId, roomTitle);
        };
    }

    /**
     * 채팅방 summary 응답을 조립한다.
     */
    public ChatRoomSummaryResponse resolveSummary(Long roomId, Long userId, ChatRoom room) {
        String title = resolveTitle(roomId, userId, room);
        String profileUrl = resolveProfileUrl(roomId, userId, room);
        Integer memberCount = resolveMemberCount(roomId);

        return new ChatRoomSummaryResponse(
                room.getId(),
                room.getRoomType().name(),
                title,
                profileUrl,
                memberCount
        );
    }

    private String resolveDmTitle(Long roomId, Long userId, String fallback) {
        Long partnerId = chatRoomRepository.findMemberIdByRoomId(roomId, userId)
                .orElse(null);

        if (partnerId == null) {
            return fallback;
        }

        return userDisplayNameService.resolveDisplayName(partnerId, userId)
                .orElse(fallback);
    }

    private String resolveGroupTitle(Long roomId, Long userId, String fallback) {
        Optional<String> customRoomName = chatListRepository.findCustomNameByUserIdAndRoomId(roomId, userId);

        if (customRoomName.isPresent() && !customRoomName.get().isBlank()) {
            return customRoomName.get();
        }

        if (!fallback.isBlank()) {
            return fallback;
        }

        return resolveParticipantTitle(roomId, userId, "그룹 채팅방");
    }

    private String resolveOpenTitle(Long roomId, Long userId, String fallback) {
        Optional<String> customRoomName = chatListRepository.findCustomNameByUserIdAndRoomId(roomId, userId);

        if (customRoomName.isPresent() && !customRoomName.get().isBlank()) {
            return customRoomName.get();
        }

        if (!fallback.isBlank()) {
            return fallback;
        }

        return "오픈 채팅방";
    }

    /**
     * 본인 제외 멤버 이름 조합
     *
     * <p>최대 3명까지 이름 표시, 초과 시 "외 N명"</p>
     */
    private String resolveParticipantTitle(Long roomId, Long userId, String fallback) {
        List<Long> otherMemberIds = chatRoomRepository.findOtherMemberIdsByRoomId(roomId, userId);

        List<String> names = otherMemberIds.stream()
                .map(memberId -> userDisplayNameService.resolveDisplayName(memberId, userId).orElse(null))
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .distinct()
                .toList();

        if (names.isEmpty()) {
            return fallback;
        }

        if (names.size() <= 3) {
            return String.join(", ", names);
        }

        String firstThree = String.join(", ", names.subList(0, 3));

        return firstThree + " 외 " + (names.size() - 3) + "명";
    }

    /**
     * summary용 프로필 이미지
     *
     * <p>지금은 null 유지.
     * DM 상대 프로필 / 그룹 대표 이미지 / open room 이미지 정책은 나중에 확장.</p>
     */
    private String resolveProfileUrl(Long roomId, Long userId, ChatRoom room) {
        return null;
    }

    private Integer resolveMemberCount(Long roomId) {
        return Math.toIntExact(chatRoomRepository.countMembersByRoomId(roomId));
    }


}
