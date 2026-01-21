package com.chat_server.userblock.repository;

public interface UserBlockRepositoryCustom {
    boolean existsByUserBlock(Long blockerId, Long userId);
}
