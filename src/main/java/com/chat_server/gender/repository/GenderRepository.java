package com.chat_server.gender.repository;

import com.chat_server.gender.entity.Gender;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * packageName    : com.chat_server.gender.repository
 * fileName       : GenderRepository
 * author         : parkminsu
 * date           : 25. 2. 27.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 2. 27.        parkminsu       최초 생성
 */
public interface GenderRepository extends JpaRepository<Gender, Integer> {
    Optional<Gender> findByGenderName(String genderName);

}
