package com.nexusos.api.calendar.domain;

import com.nexusos.api.common.domain.BaseEntity;
import com.nexusos.api.workspace.domain.Workspace;
import jakarta.persistence.*;
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "meetings")
public class Meeting extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    @JsonIgnore
    private Workspace workspace;

    @Column(nullable = false)
    private String title;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "video_url")
    private String videoUrl;

    protected Meeting() {}

    public Meeting(Workspace workspace, String title, Instant startTime, Instant endTime, String videoUrl) {
        this.workspace = workspace;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.videoUrl = videoUrl;
    }

    // Getters and Setters
}
