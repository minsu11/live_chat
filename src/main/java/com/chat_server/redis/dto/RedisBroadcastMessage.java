package com.chat_server.redis.dto;

public record RedisBroadcastMessage(String destination,
                                    Object payload) {
}
