package com.nexusos.api.calendar.service;

import com.nexusos.api.calendar.domain.Meeting;
import com.nexusos.api.calendar.repository.MeetingRepository;
import com.nexusos.api.workspace.domain.Workspace;
import com.nexusos.api.workspace.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final WorkspaceRepository workspaceRepository;

    public MeetingService(MeetingRepository meetingRepository, WorkspaceRepository workspaceRepository) {
        this.meetingRepository = meetingRepository;
        this.workspaceRepository = workspaceRepository;
    }

    @Transactional(readOnly = true)
    public List<Meeting> listMeetings(UUID workspaceId) {
        return meetingRepository.findByWorkspaceIdOrderByStartTimeAsc(workspaceId);
    }

    @Transactional
    public Meeting createMeeting(UUID workspaceId, String title, Instant startTime, Instant endTime, String videoUrl) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));
        Meeting meeting = new Meeting(workspace, title, startTime, endTime, videoUrl);
        return meetingRepository.save(meeting);
    }
}
