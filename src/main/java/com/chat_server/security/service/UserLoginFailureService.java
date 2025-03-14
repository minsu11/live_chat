package com.chat_server.security.service;

/**
 * packageName    : com.chat_server.security.service
 * fileName       : UserLoginFailureService
 * author         : parkminsu
 * date           : 25. 3. 12.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 3. 12.        parkminsu       최초 생성
 */
public interface UserLoginFailureService {
    void incrementFailedAttempts(String userId);
    boolean isAccountLocked(String userId);
    void clearFailedAttempts(String userId);
    Integer getFailedAttempts(String userId);
    void lockFailedAttempts(String userId);

}
