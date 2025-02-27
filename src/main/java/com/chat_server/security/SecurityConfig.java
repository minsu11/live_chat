package com.chat_server.security;

import com.chat_server.security.handler.SuccessHandler;
import com.chat_server.security.service.UserAuthService;
import com.sun.net.httpserver.Authenticator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.AbstractConfiguredSecurityBuilder;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * packageName    : com.chat_server.security
 * fileName       : SecurityConfig
 * author         : parkminsu
 * date           : 25. 2. 25.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 2. 25.        parkminsu       최초 생성
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final AuthenticationConfiguration authenticationConfiguration;
    private final UserAuthService userAuthService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests
                        (
                                authorizeRequests ->
                                        authorizeRequests.requestMatchers("/login","/api/v1/users/register").permitAll()
                        )
                .formLogin(AbstractHttpConfigurer::disable);



        UserAuthenticationFilter userAuthenticationFilter = new UserAuthenticationFilter(passwordEncoder());
        userAuthenticationFilter.setFilterProcessesUrl("/login");
        userAuthenticationFilter.setAuthenticationSuccessHandler(successHandler());
        http.addFilterBefore(userAuthenticationFilter, UserAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationManager userAuthenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }



    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        authenticationProvider.setUserDetailsService(userAuthService);
        return authenticationProvider;
    }

    @Bean
    public SuccessHandler successHandler() {
        return new SuccessHandler();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
