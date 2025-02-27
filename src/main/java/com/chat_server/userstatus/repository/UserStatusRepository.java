package com.chat_server.userstatus.repository;

import com.chat_server.userstatus.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * packageName    : com.chat_server.userstatus.repository
 * fileName       : UserStatusRepository
 * author         : parkminsu
 * date           : 25. 2. 27.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 2. 27.        parkminsu       최초 생성
 */
public interface UserStatusRepository extends JpaRepository<UserStatus, Long> {
    Optional<UserStatus> findByUserStatusName(String userStatusName);
}
