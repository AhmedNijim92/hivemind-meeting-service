package com.hivemind.meeting.repository;

import com.hivemind.meeting.entity.Meeting;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeetingRepository extends CassandraRepository<Meeting, Object>
{
    @Query("SELECT * FROM meetings WHERE group_id = ?0")
    List<Meeting> findByGroupId(UUID groupId);

    @Query("SELECT * FROM meetings WHERE group_id = ?0 AND meeting_id = ?1")
    Optional<Meeting> findByGroupIdAndMeetingId(UUID groupId, UUID meetingId);
}
