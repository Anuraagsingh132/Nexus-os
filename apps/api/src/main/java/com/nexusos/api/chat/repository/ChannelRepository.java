package com.nexusos.api.chat.repository;

import com.nexusos.api.chat.domain.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChannelRepository extends JpaRepository<Channel, UUID> {
    Optional<Channel> findByWorkspaceIdAndName(UUID workspaceId, String name);
    List<Channel> findByWorkspaceId(UUID workspaceId);
}
