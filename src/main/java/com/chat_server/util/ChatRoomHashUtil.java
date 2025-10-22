package com.chat_server.util;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChatRoomHashUtil {

    /**
     * 두 유저의 ID로 1:1 채팅방용 고유 해시 생성
     * (1,2)와 (2,1)은 항상 같은 값이 됨
     */
    public static String createUserPairHash(Long userId1, Long userId2) {
        long first = Math.min(userId1, userId2);
        long second = Math.max(userId1, userId2);
        String raw = first + ":" + second;
        return sha256(raw);
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not found", e);
        }
    }

}
