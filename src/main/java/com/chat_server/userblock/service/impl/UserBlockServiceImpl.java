package com.chat_server.userblock.service.impl;

import com.chat_server.userblock.exception.UserBlockExistsException;
import com.chat_server.userblock.repository.UserBlockRepository;
import com.chat_server.userblock.service.UserBlockService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserBlockServiceImpl implements UserBlockService {
    private final UserBlockRepository userBlockRepository;


    @Override
    public void validateSenderNotBlocked(Long senderUserId, Long receiverUserId) {
        if(
            userBlockRepository.existsByUserBlock(senderUserId,receiverUserId)
            || userBlockRepository.existsByUserBlock(receiverUserId,senderUserId)
        ) {
            throw new UserBlockExistsException("차단된 관계입니다..");
        }
    }

    @Override
    public boolean isBlocked(Long senderUserId, Long receiverUserId) {
        return     userBlockRepository.existsByUserBlock(senderUserId,receiverUserId)
                || userBlockRepository.existsByUserBlock(receiverUserId,senderUserId);
    }
}
