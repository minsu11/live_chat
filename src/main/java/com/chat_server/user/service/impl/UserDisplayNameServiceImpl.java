package com.chat_server.user.service.impl;

import com.chat_server.user.repository.UserRepository;
import com.chat_server.user.service.UserDisplayNameService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserDisplayNameServiceImpl implements UserDisplayNameService {

    private final UserRepository userRepository;

    @Override
    public Optional<String> resolveDisplayName(Long senderId, Long receiverId) {


        return null;
    }
}
