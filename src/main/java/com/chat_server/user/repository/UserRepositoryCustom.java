package com.chat_server.user.repository;

import com.chat_server.security.dto.UserAuthDto;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;

/**
 * packageName    : com.chat_server.user.repository
 * fileName       : UserRepositoryCustom
 * author         : parkminsu
 * date           : 25. 2. 27.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 2. 27.        parkminsu       최초 생성
 */
@NoRepositoryBean
public interface UserRepositoryCustom {
    Optional<UserAuthDto> findByUserName(String userName);

}
