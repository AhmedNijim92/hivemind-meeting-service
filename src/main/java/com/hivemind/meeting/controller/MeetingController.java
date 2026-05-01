package com.hivemind.meeting.controller;

import com.hivemind.common.dto.ApiResponse;
import com.hivemind.meeting.dto.CreateMeetingRequest;
import com.hivemind.meeting.dto.MeetingDto;
import com.hivemind.meeting.service.IMeetingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/meetings")
@RequiredArgsConstructor
public class MeetingController
{
    private final IMeetingService meetingService;

    @PostMapping
    public ResponseEntity<MeetingDto> createMeeting(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CreateMeetingRequest request)
    {
        return ResponseEntity.status(HttpStatus.CREATED).body(meetingService.createMeeting(userId, request));
    }

    @GetMapping("/{groupId}/{meetingId}")
    public ResponseEntity<MeetingDto> getMeetingById(
            @PathVariable UUID groupId,
            @PathVariable UUID meetingId)
    {
        return ResponseEntity.ok(meetingService.getMeetingById(groupId, meetingId));
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<MeetingDto>> getMeetingsByGroup(@PathVariable UUID groupId)
    {
        return ResponseEntity.ok(meetingService.getMeetingsByGroup(groupId));
    }

    @PostMapping("/{groupId}/{meetingId}/start")
    public ResponseEntity<MeetingDto> startMeeting(
            @PathVariable UUID groupId,
            @PathVariable UUID meetingId,
            @RequestHeader("X-User-Id") UUID userId)
    {
        return ResponseEntity.ok(meetingService.startMeeting(groupId, meetingId, userId));
    }

    @PostMapping("/{groupId}/{meetingId}/join")
    public ResponseEntity<ApiResponse> joinMeeting(
            @PathVariable UUID groupId,
            @PathVariable UUID meetingId,
            @RequestHeader("X-User-Id") UUID userId)
    {
        meetingService.joinMeeting(groupId, meetingId, userId);
        return ResponseEntity.ok(new ApiResponse("Joined meeting successfully"));
    }

    @PostMapping("/{meetingId}/leave")
    public ResponseEntity<ApiResponse> leaveMeeting(
            @PathVariable UUID meetingId,
            @RequestHeader("X-User-Id") UUID userId)
    {
        meetingService.leaveMeeting(meetingId, userId);
        return ResponseEntity.ok(new ApiResponse("Left meeting successfully"));
    }

    @PostMapping("/{groupId}/{meetingId}/end")
    public ResponseEntity<MeetingDto> endMeeting(
            @PathVariable UUID groupId,
            @PathVariable UUID meetingId,
            @RequestHeader("X-User-Id") UUID userId)
    {
        return ResponseEntity.ok(meetingService.endMeeting(groupId, meetingId, userId));
    }

    @GetMapping("/{meetingId}/participants")
    public ResponseEntity<Set<String>> getParticipants(@PathVariable UUID meetingId)
    {
        return ResponseEntity.ok(meetingService.getParticipants(meetingId));
    }
}
