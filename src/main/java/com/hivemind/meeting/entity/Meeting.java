package com.hivemind.meeting.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("meetings")
public class Meeting
{
    @PrimaryKeyColumn(name = "group_id", type = PrimaryKeyType.PARTITIONED)
    private UUID groupId;

    @PrimaryKeyColumn(name = "meeting_id", type = PrimaryKeyType.CLUSTERED, ordering = org.springframework.data.cassandra.core.cql.Ordering.DESCENDING)
    private UUID meetingId;

    @Column("host_id")
    private UUID hostId;

    @Column("title")
    private String title;

    @Column("description")
    private String description;

    @Column("status")
    private String status; // SCHEDULED, ACTIVE, ENDED

    @Column("privacy")
    private String privacy; // PUBLIC, PRIVATE

    @Column("scheduled_at")
    private LocalDateTime scheduledAt;

    @Column("started_at")
    private LocalDateTime startedAt;

    @Column("ended_at")
    private LocalDateTime endedAt;

    @Column("created_at")
    private LocalDateTime createdAt;
}
