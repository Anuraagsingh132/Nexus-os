package com.nexusos.api.chat.repository;

import com.nexusos.api.chat.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"author"})
    List<ChatMessage> findByChannelIdOrderByCreatedAtAsc(UUID channelId);
}
