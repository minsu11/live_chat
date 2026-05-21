package com.chat_server.userprofile.repository;

import com.chat_server.userprofile.enrtity.UserProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long>, UserProfileRepositoryCustom {

    Optional<UserProfile> findByUser_Id(Long userId);
}
