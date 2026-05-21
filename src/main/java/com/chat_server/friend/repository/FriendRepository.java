package com.chat_server.friend.repository;

import com.chat_server.friend.entity.Friend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FriendRepository extends JpaRepository<Friend, Long>, FriendRepositoryCustom {
    /**
     * 사용자(userId)가 친구(friendUserId)에게 지정한 커스텀 닉네임을 조회한다.
     *
     * @param userId 닉네임을 설정한 사용자 ID(조회 주체)
     * @param friendUserId 닉네임 대상 친구 사용자 ID
     * @return 커스텀 닉네임 Optional(없거나 빈 문자열이면 empty)
     */
    @Query("""
        select f.customNickname
        from Friend f
        where f.user.id = :userId
          and f.friendUser.id = :friendUserId
          and f.customNickname is not null
          and f.customNickname <> ''
        """)
    Optional<String> findCustomNickname(@Param("userId") Long userId, @Param("friendUserId") Long friendUserId);

    @Query("SELECT f FROM Friend f WHERE f.user.id IN :receiverIds AND f.friendUser.id = :senderId")
    List<Friend> findFriendsByReceiverIdsAndSenderId(
            @Param("receiverIds") List<Long> receiverIds,
            @Param("senderId") Long senderId
    );

    @Query("SELECT f FROM Friend f WHERE f.user.id = :userId AND f.friendUser.id IN :friendUserIds")
    List<Friend> findFriendsByUserIdAndFriendUserIds(
            @Param("userId") Long userId,
            @Param("friendUserIds") List<Long> friendUserIds
    );

    boolean existsByUser_IdAndFriendUser_Id(Long userId, Long friendUserId);
}
