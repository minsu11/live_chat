package com.chat_server.security.service;

import com.chat_server.security.PrincipalUser;
import com.chat_server.security.dto.UserAuthDto;
import com.chat_server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * packageName    : com.chat_server.security.service
 * fileName       : UserAuthService
 * author         : parkminsu
 * date           : 25. 2. 26.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 2. 26.        parkminsu       최초 생성
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserAuthService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // user name 유효성 검사
        if(Objects.isNull(username) || username.isEmpty()) {
            throw new UsernameNotFoundException("Username is null or empty");
        }
        UserAuthDto userAuthDto = userRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("Username " + username + " not found"));
        // principal user 생성
        return new PrincipalUser(userAuthDto.getUserInputId(), userAuthDto.getUserInputPassword());
    }

}
