package com.chat_server.security.service.impl;

import com.chat_server.security.service.UserLoginFailureService;
import org.springframework.stereotype.Service;

/**
 * packageName    : com.chat_server.security.service.impl
 * fileName       : UserLoginFailureServiceImpl
 * author         : parkminsu
 * date           : 25. 3. 12.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 3. 12.        parkminsu       최초 생성
 */

@Service
public class UserLoginFailureServiceImpl implements UserLoginFailureService {
    // todo 기본적인 레디스 서비스 만들기



    @Override
    public void incrementFailedAttempts(String userId) {

    }

    @Override
    public boolean isAccountLocked(String userId) {
        return false;
    }

    @Override
    public void clearFailedAttempts(String userId) {

    }

    @Override
    public Integer getFailedAttempts(String userId) {
        return 0;
    }

    @Override
    public void lockFailedAttempts(String userId) {

    }
}
