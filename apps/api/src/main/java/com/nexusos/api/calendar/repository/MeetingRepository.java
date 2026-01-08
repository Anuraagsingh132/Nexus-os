package com.nexusos.api.calendar.repository;

import com.nexusos.api.calendar.domain.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, UUID> {
    List<Meeting> findByWorkspaceIdOrderByStartTimeAsc(UUID workspaceId);
}
