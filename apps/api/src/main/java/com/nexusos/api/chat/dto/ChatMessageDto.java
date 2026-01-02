package com.nexusos.api.chat.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageDto(
        UUID id,
        String content,
        AuthorDto author,
        Instant createdAt
) {
    public record AuthorDto(UUID id, String email, String name) {}
}
