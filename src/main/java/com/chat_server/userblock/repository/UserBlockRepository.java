package com.chat_server.userblock.repository;

import com.chat_server.userblock.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBlockRepository extends JpaRepository<UserBlock,Long> {
}
