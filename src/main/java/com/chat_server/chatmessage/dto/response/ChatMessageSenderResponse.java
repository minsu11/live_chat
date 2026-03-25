package com.chat_server.chatmessage.dto.response;

/**
 * 메시지 발신자 정보를 담는 DTO.
 *
 * @param senderUuid 발신자 UUID
 * @param senderNickname 발신자 닉네임
 * @param profileImageUrl 발신자 프로필 이미지 URL(없으면 null)
 * @param mine 현재 로그인 유저 본인 메시지 여부
 */
public record ChatMessageSenderResponse(
        String senderUuid,
        String senderNickname,
        String profileImageUrl,
        boolean mine
) {
}
