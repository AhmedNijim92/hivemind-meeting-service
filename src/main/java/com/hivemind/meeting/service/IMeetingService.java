package com.hivemind.meeting.service;

import com.hivemind.meeting.dto.CreateMeetingRequest;
import com.hivemind.meeting.dto.MeetingDto;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface IMeetingService
{
    MeetingDto createMeeting(UUID hostId, CreateMeetingRequest request);

    MeetingDto getMeetingById(UUID groupId, UUID meetingId);

    List<MeetingDto> getMeetingsByGroup(UUID groupId);

    MeetingDto startMeeting(UUID groupId, UUID meetingId, UUID userId);

    void joinMeeting(UUID groupId, UUID meetingId, UUID userId);

    void leaveMeeting(UUID meetingId, UUID userId);

    MeetingDto endMeeting(UUID groupId, UUID meetingId, UUID userId);

    Set<String> getParticipants(UUID meetingId);
}
