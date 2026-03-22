package com.chat_server.user.repository;

import com.chat_server.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, UserRepositoryCustom {
    Optional<User> findByInputId(String inputId);
    Optional<User> findByUuid(String uuid);

    boolean existsByInputId(String inputId);
}