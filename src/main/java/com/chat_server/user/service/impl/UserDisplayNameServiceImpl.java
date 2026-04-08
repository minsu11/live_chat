package com.chat_server.user.service.impl;

import com.chat_server.friend.repository.FriendRepository;
import com.chat_server.user.entity.User;
import com.chat_server.user.repository.UserRepository;
import com.chat_server.user.service.UserDisplayNameService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserDisplayNameServiceImpl implements UserDisplayNameService {

    private final FriendRepository friendRepository;
    private final UserRepository userRepository;

    /**
     * 메시지 수신자 관점에서 발신자 display name을 결정한다.
     *
     * <p>우선순위:
     * <ol>
     *   <li>수신자가 발신자에게 지정한 친구 커스텀 닉네임</li>
     *   <li>발신자의 기본 닉네임</li>
     * </ol>
     *
     * @param senderId 발신자 사용자 ID
     * @param receiverId 수신자 사용자 ID
     * @return 우선순위에 따라 결정된 표시 이름 Optional
     */
    @Override
    public Optional<String> resolveDisplayName(Long senderId, Long receiverId) {
        // 메서드 진입 로그는 info 레벨로 간단히 남긴다.
        log.info("resolveDisplayName 호출");
        // 파라미터 상세 값은 debug 레벨로 남겨 추적 가능성을 높인다.
        log.debug("resolveDisplayName params - senderId: {}, receiverId: {}", senderId, receiverId);

        // 1순위: 수신자 기준 친구 커스텀 닉네임(내가 상대에게 붙인 이름)
        Optional<String> friendCustomNickname = friendRepository.findCustomNickname(receiverId, senderId);
        log.debug("resolveDisplayName friendCustomNickname: {}", friendCustomNickname.orElse(null));
        if (friendCustomNickname.isPresent()) {
            log.debug("resolveDisplayName return(친구 커스텀 닉네임): {}", friendCustomNickname.get());
            return friendCustomNickname;
        }

        // 2순위: 발신자가 설정한 기본 닉네임
        Optional<String> senderNickname = userRepository.findById(senderId)
                .map(User::getNickname)
                .filter(nickname -> !nickname.isBlank());
        log.debug("resolveDisplayName senderNickname: {}", senderNickname.orElse(null));
        log.debug("resolveDisplayName return(기본 닉네임): {}", senderNickname.orElse(null));
        return senderNickname;
    }
}
