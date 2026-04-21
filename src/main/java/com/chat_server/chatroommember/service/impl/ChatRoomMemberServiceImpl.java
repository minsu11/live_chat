package com.chat_server.chatroommember.service.impl;

import com.chat_server.chatroom.entity.ChatRoom;
import com.chat_server.chatroom.exception.ChatRoomNotFoundException;
import com.chat_server.chatroom.repository.ChatRoomRepository;
import com.chat_server.chatroommember.dto.response.ChatRoomMemberInfoDto;
import com.chat_server.chatroommember.entity.ChatRoomMember;
import com.chat_server.chatroommember.enums.RoomMemberRole;
import com.chat_server.chatroommember.repository.ChatRoomMemberRepository;
import com.chat_server.chatroommember.service.ChatRoomMemberService;
import com.chat_server.error.enumulation.ErrorCode;
import com.chat_server.error.exception.BusinessException;
import com.chat_server.user.entity.User;
import com.chat_server.user.exception.UserNotFoundException;
import com.chat_server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatRoomMemberServiceImpl implements ChatRoomMemberService {
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

    @Override
    public void ensureMembership(Long userId, Long chatRoomId) {
        chatRoomMemberRepository.upsertMembership(chatRoomId,userId,RoomMemberRole.MEMBER.name());
    }

    @Override
    public void leaveRoomMember(Long roomId, Long userId) {
        ChatRoomMember member = chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId,userId)
                .orElseThrow(()-> new BusinessException(ErrorCode.NOT_FOUND,"채팅방 멤버를 찾을 수 없습니다."));

        member.leave();
    }

    @Transactional(readOnly = true)
    @Override
    public List<ChatRoomMemberInfoDto> getChatRoomMemberIds(Long roomId) {

        return chatRoomMemberRepository.findMemberInfosByRoomId(roomId);
    }

    // 멤버 추가
    @Override
    public void addMembers(Long roomId, List<String> inviteeUuid) {
        ChatRoom room = chatRoomRepository.findById(roomId).orElseThrow(ChatRoomNotFoundException::new);
        for(String uuid : inviteeUuid) {
            User user = userRepository.findByUuid(uuid).orElseThrow(UserNotFoundException::new);
            ChatRoomMember chatRoomMember = ChatRoomMember.builder()
                    .chatRoom(room)
                    .user(user)
                    .role(RoomMemberRole.MEMBER)
                    .joinedAt(LocalDateTime.now())
                    .build();
            chatRoomMemberRepository.save(chatRoomMember);
            room.incrementParticipantCount();
        }
    }

}
