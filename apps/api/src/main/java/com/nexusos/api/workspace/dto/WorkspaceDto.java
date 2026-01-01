package com.nexusos.api.workspace.dto;

import java.util.UUID;

public class WorkspaceDto {
    private UUID id;
    private String name;
    private String slug;

    public WorkspaceDto() {}

    public WorkspaceDto(UUID id, String name, String slug) {
        this.id = id;
        this.name = name;
        this.slug = slug;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
}
