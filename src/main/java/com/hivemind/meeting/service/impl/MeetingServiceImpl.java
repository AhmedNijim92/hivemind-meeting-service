package com.hivemind.meeting.service.impl;

import com.hivemind.common.event.MeetingStartedEvent;
import com.hivemind.meeting.dto.CreateMeetingRequest;
import com.hivemind.meeting.dto.MeetingDto;
import com.hivemind.meeting.entity.Meeting;
import com.hivemind.meeting.repository.MeetingRepository;
import com.hivemind.meeting.service.IMeetingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingServiceImpl implements IMeetingService
{
    private final MeetingRepository meetingRepository;
    private final KafkaTemplate<String, MeetingStartedEvent> kafkaTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String PARTICIPANTS_KEY_PREFIX = "meeting:participants:";

    @Override
    public MeetingDto createMeeting(UUID hostId, CreateMeetingRequest request)
    {
        Meeting meeting = Meeting.builder()
                .groupId(request.getGroupId())
                .meetingId(UUID.randomUUID())
                .hostId(hostId)
                .title(request.getTitle())
                .description(request.getDescription())
                .status("SCHEDULED")
                .privacy(request.getPrivacy())
                .scheduledAt(request.getScheduledAt())
                .createdAt(LocalDateTime.now())
                .build();

        meetingRepository.save(meeting);
        log.info("Meeting created: {} in group: {}", meeting.getMeetingId(), meeting.getGroupId());
        return toDto(meeting);
    }

    @Override
    public MeetingDto getMeetingById(UUID groupId, UUID meetingId)
    {
        Meeting meeting = meetingRepository.findByGroupIdAndMeetingId(groupId, meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found: " + meetingId));
        return toDto(meeting);
    }

    @Override
    public List<MeetingDto> getMeetingsByGroup(UUID groupId)
    {
        return meetingRepository.findByGroupId(groupId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public MeetingDto startMeeting(UUID groupId, UUID meetingId, UUID userId)
    {
        Meeting meeting = meetingRepository.findByGroupIdAndMeetingId(groupId, meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found: " + meetingId));

        if (!meeting.getHostId().equals(userId))
        {
            throw new RuntimeException("Only the host can start the meeting");
        }

        meeting.setStatus("ACTIVE");
        meeting.setStartedAt(LocalDateTime.now());
        meetingRepository.save(meeting);

        // Add host as first participant in Redis
        String key = PARTICIPANTS_KEY_PREFIX + meetingId;
        redisTemplate.opsForSet().add(key, userId.toString());

        // Publish event
        MeetingStartedEvent event = MeetingStartedEvent.builder()
                .meetingId(meetingId)
                .groupId(groupId)
                .hostId(userId)
                .title(meeting.getTitle())
                .timestamp(LocalDateTime.now())
                .build();
        kafkaTemplate.send("meeting-started-topic", event);

        log.info("Meeting {} started by host {}", meetingId, userId);
        return toDto(meeting);
    }

    @Override
    public void joinMeeting(UUID groupId, UUID meetingId, UUID userId)
    {
        Meeting meeting = meetingRepository.findByGroupIdAndMeetingId(groupId, meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found: " + meetingId));

        if (!"ACTIVE".equals(meeting.getStatus()))
        {
            throw new RuntimeException("Meeting is not active");
        }

        String key = PARTICIPANTS_KEY_PREFIX + meetingId;
        redisTemplate.opsForSet().add(key, userId.toString());
        log.info("User {} joined meeting {}", userId, meetingId);
    }

    @Override
    public void leaveMeeting(UUID meetingId, UUID userId)
    {
        String key = PARTICIPANTS_KEY_PREFIX + meetingId;
        redisTemplate.opsForSet().remove(key, userId.toString());
        log.info("User {} left meeting {}", userId, meetingId);
    }

    @Override
    public MeetingDto endMeeting(UUID groupId, UUID meetingId, UUID userId)
    {
        Meeting meeting = meetingRepository.findByGroupIdAndMeetingId(groupId, meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found: " + meetingId));

        if (!meeting.getHostId().equals(userId))
        {
            throw new RuntimeException("Only the host can end the meeting");
        }

        meeting.setStatus("ENDED");
        meeting.setEndedAt(LocalDateTime.now());
        meetingRepository.save(meeting);

        // Clean up Redis participants
        redisTemplate.delete(PARTICIPANTS_KEY_PREFIX + meetingId);
        log.info("Meeting {} ended by host {}", meetingId, userId);
        return toDto(meeting);
    }

    @Override
    public Set<String> getParticipants(UUID meetingId)
    {
        String key = PARTICIPANTS_KEY_PREFIX + meetingId;
        return redisTemplate.opsForSet().members(key);
    }

    private MeetingDto toDto(Meeting meeting)
    {
        String key = PARTICIPANTS_KEY_PREFIX + meeting.getMeetingId();
        Long participantCount = redisTemplate.opsForSet().size(key);

        return MeetingDto.builder()
                .meetingId(meeting.getMeetingId())
                .groupId(meeting.getGroupId())
                .hostId(meeting.getHostId())
                .title(meeting.getTitle())
                .description(meeting.getDescription())
                .status(meeting.getStatus())
                .privacy(meeting.getPrivacy())
                .scheduledAt(meeting.getScheduledAt())
                .startedAt(meeting.getStartedAt())
                .endedAt(meeting.getEndedAt())
                .createdAt(meeting.getCreatedAt())
                .participantCount(participantCount != null ? participantCount : 0)
                .build();
    }
}
