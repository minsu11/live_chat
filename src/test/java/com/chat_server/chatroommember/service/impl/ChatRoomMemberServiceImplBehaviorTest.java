package com.chat_server.chatroommember.service.impl;

import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.chatroom.enums.RoomType;
import com.chat_server.chatroom.repository.ChatRoomRepository;
import com.chat_server.chatroommember.dto.response.ChatRoomMemberInfoDto;
import com.chat_server.chatroommember.entity.ChatRoomMember;
import com.chat_server.chatroommember.repository.ChatRoomMemberRepository;
import com.chat_server.error.exception.BusinessException;
import com.chat_server.user.entity.User;
import com.chat_server.user.exception.UserNotFoundException;
import com.chat_server.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChatRoomMemberServiceImplBehaviorTest {

    private ChatRoomMemberRepository memberRepository;
    private UserRepository userRepository;
    private ChatRoomMemberServiceImpl service;

    @BeforeEach
    void setUp() {
        memberRepository = mock(ChatRoomMemberRepository.class);
        userRepository = mock(UserRepository.class);
        service = new ChatRoomMemberServiceImpl(
                memberRepository,
                mock(ChatRoomRepository.class),
                userRepository
        );
    }

    /** 멤버십 upsert가 정확한 역할값으로 위임되는지 검증한다. */
    @Test
    @DisplayName("채팅방 멤버십 생성 성공 - MEMBER 역할로 upsert를 호출한다")
    void ensureMembershipShouldDelegateWithMemberRole() {
        service.ensureMembership(1L, 10L);

        verify(memberRepository).upsertMembership(10L, 1L, "MEMBER");
    }

    /** 방 나가기 시 멤버의 active 상태와 leftAt이 변경되는지 검증한다. */
    @Test
    @DisplayName("채팅방 나가기 성공 - 멤버를 비활성화하고 퇴장 시각을 기록한다")
    void leaveRoomMemberShouldDeactivateMember() {
        ChatRoomMember member = ChatRoomMember.builder()
                .chatRoom(room(10L))
                .user(user(1L))
                .active(true)
                .build();
        when(memberRepository.findByChatRoomIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(member));

        service.leaveRoomMember(10L, 1L);

        assertThat(member.isActive()).isFalse();
        assertThat(member.getLeftAt()).isNotNull();
    }

    /** 존재하지 않는 멤버가 나가기를 요청하면 BusinessException이 발생하는지 검증한다. */
    @Test
    @DisplayName("채팅방 나가기 실패 - 멤버가 없으면 NOT_FOUND 예외를 발생시킨다")
    void leaveRoomMemberShouldThrowWhenMemberMissing() {
        when(memberRepository.findByChatRoomIdAndUserId(10L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.leaveRoomMember(10L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("채팅방 멤버");
    }

    /** 멤버 정보 목록 조회 결과를 그대로 반환하는지 검증한다. */
    @Test
    @DisplayName("채팅방 멤버 목록 조회 성공 - Repository 조회 결과를 반환한다")
    void getChatRoomMemberIdsShouldReturnRepositoryResult() {
        List<ChatRoomMemberInfoDto> expected = List.of(
                new ChatRoomMemberInfoDto(1L, "uuid-1", "민수", null)
        );
        when(memberRepository.findMemberInfosByRoomId(10L)).thenReturn(expected);

        assertThat(service.getChatRoomMemberIds(10L)).isSameAs(expected);
    }

    /** 여러 사용자를 초대할 때 각 멤버를 저장하고 참여자 수를 증가시키는지 검증한다. */
    @Test
    @DisplayName("채팅방 멤버 추가 성공 - 초대 사용자마다 멤버를 저장하고 참여자 수를 증가시킨다")
    void addMembersShouldSaveEveryInviteeAndIncreaseParticipantCount() {
        ChatRoom room = room(10L);
        when(userRepository.findByUuid("uuid-1")).thenReturn(Optional.of(user(1L)));
        when(userRepository.findByUuid("uuid-2")).thenReturn(Optional.of(user(2L)));

        service.addMembers(10L, List.of("uuid-1", "uuid-2"), room);

        ArgumentCaptor<ChatRoomMember> captor = ArgumentCaptor.forClass(ChatRoomMember.class);
        verify(memberRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(member -> {
            assertThat(member.getChatRoom()).isSameAs(room);
            assertThat(member.isActive()).isTrue();
            assertThat(member.getJoinedAt()).isNotNull();
        });
        assertThat(room.getParticipantCount()).isEqualTo(2);
    }

    /** 초대 목록이 비어 있으면 사용자 조회와 저장을 하지 않는 경계값을 검증한다. */
    @Test
    @DisplayName("채팅방 멤버 추가 경계값 - 초대 목록이 비어 있으면 아무 작업도 하지 않는다")
    void addMembersShouldDoNothingWhenInviteeListIsEmpty() {
        ChatRoom room = room(10L);

        service.addMembers(10L, List.of(), room);

        verifyNoInteractions(userRepository);
        verify(memberRepository, never()).save(any(ChatRoomMember.class));
        assertThat(room.getParticipantCount()).isZero();
    }

    /** 초대 사용자 중 하나라도 존재하지 않으면 해당 시점에서 예외를 전파하는지 검증한다. */
    @Test
    @DisplayName("채팅방 멤버 추가 실패 - 초대 사용자 UUID가 없으면 예외를 발생시킨다")
    void addMembersShouldThrowWhenInviteeMissing() {
        ChatRoom room = room(10L);
        when(userRepository.findByUuid("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addMembers(10L, List.of("missing"), room))
                .isInstanceOf(UserNotFoundException.class);

        verify(memberRepository, never()).save(any(ChatRoomMember.class));
        assertThat(room.getParticipantCount()).isZero();
    }

    /** 방 멤버 ID 목록 조회를 정확히 위임하는지 검증한다. */
    @Test
    @DisplayName("채팅방 멤버 ID 조회 성공 - Repository 결과를 반환한다")
    void getRoomMemberIdsByRoomIdShouldReturnRepositoryResult() {
        when(memberRepository.findRoomMemberIdsByRoomId(10L)).thenReturn(List.of(1L, 2L));

        assertThat(service.getRoomMemberIdsByRoomId(10L)).containsExactly(1L, 2L);
    }

    private ChatRoom room(Long id) {
        return ChatRoom.builder()
                .id(id)
                .roomType(RoomType.GROUP)
                .createdBy(user(99L))
                .participantCount(0)
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
