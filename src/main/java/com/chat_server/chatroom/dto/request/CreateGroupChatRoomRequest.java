package com.chat_server.chatroom.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateGroupChatRoomRequest(@Size(max = 50, message = "채팅방 이름은 50자를 초과할 수 없습니다.")
                                         String title,
                                         @Size(min = 2, message = "2명 이하 입니다.") List<String> memberUuids

                                         ) {
}
