package com.nexusos.api.chat.service;

import com.nexusos.api.chat.domain.Channel;
import com.nexusos.api.chat.domain.ChatMessage;
import com.nexusos.api.chat.repository.ChannelRepository;
import com.nexusos.api.chat.repository.ChatMessageRepository;
import com.nexusos.api.identity.domain.User;
import com.nexusos.api.identity.repository.UserRepository;
import com.nexusos.api.workspace.domain.Workspace;
import com.nexusos.api.workspace.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ChatService {

    private final ChannelRepository channelRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;

    public ChatService(ChannelRepository channelRepository, ChatMessageRepository chatMessageRepository,
                       WorkspaceRepository workspaceRepository, UserRepository userRepository) {
        this.channelRepository = channelRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Channel getOrCreateChannel(UUID workspaceId, String name) {
        return channelRepository.findByWorkspaceIdAndName(workspaceId, name)
                .orElseGet(() -> {
                    Workspace workspace = workspaceRepository.findById(workspaceId)
                            .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));
                    return channelRepository.save(new Channel(workspace, name));
                });
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getMessages(UUID channelId) {
        return chatMessageRepository.findByChannelIdOrderByCreatedAtAsc(channelId);
    }

    @Transactional
    public ChatMessage sendMessage(UUID workspaceId, String channelName, UUID authorId, String content) {
        Channel channel = getOrCreateChannel(workspaceId, channelName);
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("Author not found"));
        ChatMessage message = new ChatMessage(channel, author, content);
        return chatMessageRepository.save(message);
    }
}
