package com.chat_server.user.repository;

import com.chat_server.user.entity.User;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, UserRepositoryCustom {
    Optional<User> findByInputId(String inputId);
    Optional<User> findByUuid(String uuid);
    @Query("select u.uuid from User u where u.id = :id")
    Optional<String> findUuidById(@Param("id") Long id);

    boolean existsByInputId(String inputId);
}