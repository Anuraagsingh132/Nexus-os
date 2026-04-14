package com.nexusos.api.ai.agent.tool;

import com.nexusos.api.calendar.service.MeetingService;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class DraftCalendarMeetingTool implements AgentTool {

    private final MeetingService meetingService;

    public DraftCalendarMeetingTool(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @Override
    public String getName() {
        return "draft_calendar_meeting";
    }

    @Override
    public String getDescription() {
        return "Drafts a new calendar meeting.";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "title", Map.of("type", "string", "description", "Title of the meeting"),
                "start_time", Map.of("type", "string", "description", "Start time in ISO 8601 format"),
                "end_time", Map.of("type", "string", "description", "End time in ISO 8601 format"),
                "video_url", Map.of("type", "string", "description", "Optional video meeting URL")
            ),
            "required", java.util.List.of("title", "start_time", "end_time")
        );
    }

    @Override
    public boolean isHighImpact() {
        return false;
    }

    @Override
    public ToolResult execute(UUID workspaceId, UUID requestingUserId, Map<String, Object> arguments) {
        String title = (String) arguments.get("title");
        Instant startTime = Instant.parse((String) arguments.get("start_time"));
        Instant endTime = Instant.parse((String) arguments.get("end_time"));
        String videoUrl = (String) arguments.get("video_url");

        meetingService.createMeeting(workspaceId, title, startTime, endTime, videoUrl);

        return ToolResult.builder()
                .success(true)
                .summary("Successfully drafted meeting: " + title)
                .build();
    }
}
