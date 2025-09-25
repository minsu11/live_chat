package com.chat_server.friend.dto.response;

import jakarta.annotation.Nullable;

import java.util.List;

public record CursorPageResponse<T>(List<T> items, @Nullable String next, boolean hasNext) {}

