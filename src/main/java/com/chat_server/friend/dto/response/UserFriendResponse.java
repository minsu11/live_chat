package com.chat_server.friend.dto.response;


// id: 친구의 아이디(uuid), name: 친구 이름, profileUrl: 친구 프로필
public record UserFriendResponse (Long id,String name, String profileUrl){
}
