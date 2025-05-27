package com.chat_server.authentication.dto;

import com.chat_server.user.domain.UserStatus;
import com.chat_server.user.domain.UserType;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * packageName    : com.chat_server.authentication.dto
 * fileName       : UserPrincipal
 * author         : parkminsu
 * date           : 25. 5. 26.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 5. 26.        parkminsu       최초 생성
 */

/**
 * 인증된 사용자 정보 (Spring Security 인증 객체)
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final String username;
    private final UserType userType;
    private final UserStatus userStatus;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(String username, UserType userType, UserStatus userStatus) {
        this.username = username;
        this.userType = userType;
        this.userStatus = userStatus;
        this.authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + userType.name()) // ex) ROLE_ADMIN, ROLE_USER
        );
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // 계정 만료 정책 없음
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.userStatus != UserStatus.BLOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 비밀번호 만료 정책 없음
    }

    @Override
    public boolean isEnabled() {
        return this.userStatus == UserStatus.ACTIVE;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public String getUsername() {
        return this.username;
    }
}
