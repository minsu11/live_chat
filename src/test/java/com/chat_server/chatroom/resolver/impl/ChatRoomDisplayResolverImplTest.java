package com.chat_server.chatroom.resolver.impl;

import com.chat_server.chatlist.repository.ChatListRepository;
import com.chat_server.chatroom.dto.response.ChatRoomSummaryResponse;
import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.chatroom.enums.RoomType;
import com.chat_server.chatroom.repository.ChatRoomRepository;
import com.chat_server.user.entity.User;
import com.chat_server.user.service.UserDisplayNameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ChatRoomDisplayResolverImplTest {

    private ChatRoomRepository chatRoomRepository;
    private ChatListRepository chatListRepository;
    private UserDisplayNameService userDisplayNameService;
    private ChatRoomDisplayResolverImpl resolver;

    @BeforeEach
    void setUp() {
        chatRoomRepository = mock(ChatRoomRepository.class);
        chatListRepository = mock(ChatListRepository.class);
        userDisplayNameService = mock(UserDisplayNameService.class);
        resolver = new ChatRoomDisplayResolverImpl(
                chatRoomRepository,
                chatListRepository,
                userDisplayNameService
        );
    }

    /**
     * roomId로 제목을 조회할 때 채팅방 자체가 존재하지 않는 실패 상황을 검증한다.
     * 채팅방이 없으면 사용자별 커스텀 이름이나 표시 이름을 조회하지 않아야 한다.
     */
    @Test
    @DisplayName("채팅방 제목 조회 실패 - roomId에 해당하는 채팅방이 없으면 명확한 예외를 발생시킨다")
    void resolveTitleShouldThrowWhenRoomDoesNotExist() {
        when(chatRoomRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolveTitle(10L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("roomId=10");

        verifyNoInteractions(chatListRepository, userDisplayNameService);
    }

    /**
     * 사용자가 설정한 커스텀 이름이 있으면 방 타입과 기본 이름보다 우선하는지 검증한다.
     * 조기 반환되어 DM 상대나 그룹 참여자 조회가 실행되지 않아야 한다.
     */
    @Test
    @DisplayName("채팅방 제목 조회 성공 - 공백이 아닌 커스텀 이름이 있으면 최우선으로 반환한다")
    void resolveTitleShouldPreferCustomRoomName() {
        ChatRoom room = room(10L, RoomType.DM, "기본 DM 이름");
        when(chatListRepository.findCustomNameByUserIdAndRoomId(10L, 1L))
                .thenReturn(Optional.of("내가 정한 방 이름"));

        String result = resolver.resolveTitle(10L, 1L, room);

        assertThat(result).isEqualTo("내가 정한 방 이름");
        verify(chatRoomRepository, never()).findMemberIdByRoomId(anyLong(), anyLong());
        verifyNoInteractions(userDisplayNameService);
    }

    /**
     * DM 상대가 존재하고 사용자별 표시 이름을 계산할 수 있는 정상 흐름을 검증한다.
     * 상대방 ID와 현재 조회자 ID가 표시 이름 서비스에 정확히 전달되어야 한다.
     */
    @Test
    @DisplayName("DM 제목 조회 성공 - 상대방의 사용자별 표시 이름을 반환한다")
    void resolveTitleShouldUsePartnerDisplayNameForDm() {
        ChatRoom room = room(20L, RoomType.DM, "기본 이름");
        when(chatListRepository.findCustomNameByUserIdAndRoomId(20L, 1L))
                .thenReturn(Optional.empty());
        when(chatRoomRepository.findMemberIdByRoomId(20L, 1L))
                .thenReturn(Optional.of(2L));
        when(userDisplayNameService.resolveDisplayName(2L, 1L))
                .thenReturn(Optional.of("친구 별명"));

        String result = resolver.resolveTitle(20L, 1L, room);

        assertThat(result).isEqualTo("친구 별명");
    }

    /**
     * DM 상대 멤버를 찾지 못한 경계 상황을 검증한다.
     * 상대 정보가 없으면 저장된 채팅방 기본 이름을 안전하게 사용해야 한다.
     */
    @Test
    @DisplayName("DM 제목 조회 경계값 - 상대 멤버가 없으면 채팅방 기본 이름을 반환한다")
    void resolveTitleShouldUseFallbackWhenDmPartnerIsMissing() {
        ChatRoom room = room(21L, RoomType.DM, "알 수 없는 상대");
        when(chatListRepository.findCustomNameByUserIdAndRoomId(21L, 1L))
                .thenReturn(Optional.of("   "));
        when(chatRoomRepository.findMemberIdByRoomId(21L, 1L))
                .thenReturn(Optional.empty());

        String result = resolver.resolveTitle(21L, 1L, room);

        assertThat(result).isEqualTo("알 수 없는 상대");
        verifyNoInteractions(userDisplayNameService);
    }

    /**
     * DM 상대는 존재하지만 표시 이름 서비스가 값을 반환하지 않는 경계 상황을 검증한다.
     * 이 경우에도 null 대신 채팅방 기본 이름으로 fallback 해야 한다.
     */
    @Test
    @DisplayName("DM 제목 조회 경계값 - 상대 표시 이름이 없으면 채팅방 기본 이름을 반환한다")
    void resolveTitleShouldUseFallbackWhenDmDisplayNameIsMissing() {
        ChatRoom room = room(22L, RoomType.DM, "기본 DM");
        when(chatListRepository.findCustomNameByUserIdAndRoomId(22L, 1L))
                .thenReturn(Optional.empty());
        when(chatRoomRepository.findMemberIdByRoomId(22L, 1L))
                .thenReturn(Optional.of(2L));
        when(userDisplayNameService.resolveDisplayName(2L, 1L))
                .thenReturn(Optional.empty());

        assertThat(resolver.resolveTitle(22L, 1L, room)).isEqualTo("기본 DM");
    }

    /**
     * 그룹 채팅방에 기본 이름이 저장되어 있으면 참여자 이름 조합보다 우선하는지 검증한다.
     */
    @Test
    @DisplayName("그룹 제목 조회 성공 - 채팅방 기본 이름이 있으면 참여자 조회 없이 반환한다")
    void resolveTitleShouldUseStoredGroupName() {
        ChatRoom room = room(30L, RoomType.GROUP, "개발팀");
        when(chatListRepository.findCustomNameByUserIdAndRoomId(30L, 1L))
                .thenReturn(Optional.empty());

        assertThat(resolver.resolveTitle(30L, 1L, room)).isEqualTo("개발팀");
        verify(chatRoomRepository, never()).findOtherMemberIdsByRoomId(anyLong(), anyLong());
    }

    /**
     * 이름 없는 그룹 채팅방에서 참여자 표시 이름을 정제하고 중복 제거하는지 검증한다.
     * null, 공백 이름은 제외하고 유효한 이름만 쉼표로 연결해야 한다.
     */
    @Test
    @DisplayName("그룹 제목 조회 성공 - 유효한 참여자 이름만 정제하고 중복 제거해 조합한다")
    void resolveTitleShouldCombineDistinctValidParticipantNames() {
        ChatRoom room = room(31L, RoomType.GROUP, null);
        when(chatListRepository.findCustomNameByUserIdAndRoomId(31L, 1L))
                .thenReturn(Optional.empty());
        when(chatRoomRepository.findOtherMemberIdsByRoomId(31L, 1L))
                .thenReturn(List.of(2L, 3L, 4L, 5L));
        when(userDisplayNameService.resolveDisplayName(2L, 1L)).thenReturn(Optional.of(" 민수 "));
        when(userDisplayNameService.resolveDisplayName(3L, 1L)).thenReturn(Optional.empty());
        when(userDisplayNameService.resolveDisplayName(4L, 1L)).thenReturn(Optional.of(""));
        when(userDisplayNameService.resolveDisplayName(5L, 1L)).thenReturn(Optional.of("민수"));

        String result = resolver.resolveTitle(31L, 1L, room);

        assertThat(result).isEqualTo("민수");
    }

    /**
     * 그룹 참여자 이름이 3명을 초과하는 한계 상황을 검증한다.
     * 앞의 세 명만 노출하고 나머지 인원 수를 '외 N명' 형식으로 표시해야 한다.
     */
    @Test
    @DisplayName("그룹 제목 조회 한계값 - 유효한 참여자 이름이 4명 이상이면 앞 3명과 나머지 인원 수를 표시한다")
    void resolveTitleShouldLimitParticipantNamesToThree() {
        ChatRoom room = room(32L, RoomType.GROUP, "");
        when(chatListRepository.findCustomNameByUserIdAndRoomId(32L, 1L))
                .thenReturn(Optional.empty());
        when(chatRoomRepository.findOtherMemberIdsByRoomId(32L, 1L))
                .thenReturn(List.of(2L, 3L, 4L, 5L, 6L));
        when(userDisplayNameService.resolveDisplayName(2L, 1L)).thenReturn(Optional.of("A"));
        when(userDisplayNameService.resolveDisplayName(3L, 1L)).thenReturn(Optional.of("B"));
        when(userDisplayNameService.resolveDisplayName(4L, 1L)).thenReturn(Optional.of("C"));
        when(userDisplayNameService.resolveDisplayName(5L, 1L)).thenReturn(Optional.of("D"));
        when(userDisplayNameService.resolveDisplayName(6L, 1L)).thenReturn(Optional.of("E"));

        assertThat(resolver.resolveTitle(32L, 1L, room)).isEqualTo("A, B, C 외 2명");
    }

    /**
     * 이름 없는 그룹 채팅방에서 유효한 참여자 이름도 없는 경계 상황을 검증한다.
     * 사용자에게 빈 제목을 노출하지 않고 기본 문구를 반환해야 한다.
     */
    @Test
    @DisplayName("그룹 제목 조회 경계값 - 유효한 참여자 이름이 없으면 그룹 기본 문구를 반환한다")
    void resolveTitleShouldUseDefaultGroupTitleWhenParticipantNamesAreEmpty() {
        ChatRoom room = room(33L, RoomType.GROUP, null);
        when(chatListRepository.findCustomNameByUserIdAndRoomId(33L, 1L))
                .thenReturn(Optional.empty());
        when(chatRoomRepository.findOtherMemberIdsByRoomId(33L, 1L))
                .thenReturn(List.of());

        assertThat(resolver.resolveTitle(33L, 1L, room)).isEqualTo("그룹 채팅방");
    }

    /**
     * OPEN 채팅방의 기본 이름 유무에 따른 두 분기를 함께 검증한다.
     */
    @Test
    @DisplayName("오픈 채팅방 제목 조회 - 저장 이름이 있으면 사용하고 없으면 기본 문구를 반환한다")
    void resolveTitleShouldApplyOpenRoomFallbackPolicy() {
        ChatRoom namedRoom = room(40L, RoomType.OPEN, "공개 토론방");
        ChatRoom unnamedRoom = room(41L, RoomType.OPEN, null);
        when(chatListRepository.findCustomNameByUserIdAndRoomId(anyLong(), eq(1L)))
                .thenReturn(Optional.empty());

        assertThat(resolver.resolveTitle(40L, 1L, namedRoom)).isEqualTo("공개 토론방");
        assertThat(resolver.resolveTitle(41L, 1L, unnamedRoom)).isEqualTo("오픈 채팅방");
    }

    /**
     * 채팅방 요약 응답이 제목 정책과 멤버 수를 함께 조립하는지 검증한다.
     * 현재 프로필 정책이 미구현 상태이므로 profileUrl은 null이어야 한다.
     */
    @Test
    @DisplayName("채팅방 요약 조회 성공 - 타입, 제목, 멤버 수와 현재 프로필 정책을 응답에 반영한다")
    void resolveSummaryShouldComposeRoomSummary() {
        ChatRoom room = room(50L, RoomType.GROUP, "프로젝트방");
        when(chatListRepository.findCustomNameByUserIdAndRoomId(50L, 1L))
                .thenReturn(Optional.empty());
        when(chatRoomRepository.countMembersByRoomId(50L)).thenReturn(4L);

        ChatRoomSummaryResponse result = resolver.resolveSummary(50L, 1L, room);

        assertThat(result.roomId()).isEqualTo(50L);
        assertThat(result.chatType()).isEqualTo("GROUP");
        assertThat(result.title()).isEqualTo("프로젝트방");
        assertThat(result.profileUrl()).isNull();
        assertThat(result.memberCount()).isEqualTo(4);
    }

    private ChatRoom room(Long id, RoomType roomType, String name) {
        return ChatRoom.builder()
                .id(id)
                .roomType(roomType)
                .name(name)
                .createdBy(user(99L))
                .build();
    }

    private User user(Long id) {
        return User.builder()
                .id(id)
                .uuid("uuid-" + id)
                .nickname("user-" + id)
                .name("user-" + id)
                .inputId("input-" + id)
                .friendCode("friend-" + id)
                .build();
    }
}