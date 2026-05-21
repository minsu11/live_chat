package com.chat_server.logintype.repository;

import com.chat_server.logintype.entity.LoginType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoginTypeRepository extends JpaRepository<LoginType, Integer> {
    Optional<LoginType> findByName(String name);
}
