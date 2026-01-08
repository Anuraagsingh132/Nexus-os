package com.nexusos.api.calendar.controller;

import com.nexusos.api.calendar.domain.Meeting;
import com.nexusos.api.calendar.service.MeetingService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/meetings")
public class MeetingController {

    private final MeetingService meetingService;

    public MeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @GetMapping
    @PreAuthorize("@workspaceSecurity.isMember(#workspaceId)")
    public List<Meeting> listMeetings(@PathVariable UUID workspaceId) {
        return meetingService.listMeetings(workspaceId);
    }

    @PostMapping
    @PreAuthorize("@workspaceSecurity.isMember(#workspaceId)")
    public Meeting createMeeting(@PathVariable UUID workspaceId, @RequestBody CreateMeetingRequest request) {
        return meetingService.createMeeting(workspaceId, request.title(), request.startTime(), request.endTime(), request.videoUrl());
    }
}

record CreateMeetingRequest(String title, Instant startTime, Instant endTime, String videoUrl) {}
