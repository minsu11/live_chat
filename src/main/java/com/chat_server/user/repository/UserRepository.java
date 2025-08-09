package com.chat_server.user.repository;

import com.chat_server.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * packageName    : com.chat_server.user.repository
 * fileName       : UserRepository
 * author         : parkminsu
 * date           : 25. 2. 26.
 * description    : 회원의 레포지토리
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 2. 26.        parkminsu       최초 생성
 */
public interface UserRepository extends JpaRepository<User, Long>, UserRepositoryCustom {
    boolean existsByUserInputId(String inputId);
    Optional<User> findByUserInputId(String inputId);
}
