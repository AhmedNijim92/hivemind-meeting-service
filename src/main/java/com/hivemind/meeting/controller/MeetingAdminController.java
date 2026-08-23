package com.hivemind.meeting.controller;

import com.hivemind.common.dto.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Admin controls for meeting rooms.
 * Manages: hand raises, speaker permissions, muting, kicking, blocking.
 * State is stored in Redis with meeting-scoped keys.
 */
@RestController
@RequestMapping("/api/v1/meetings")
@RequiredArgsConstructor
@Slf4j
public class MeetingAdminController
{
    private final RedisTemplate<String, String> redisTemplate;

    private static final String RAISED_HANDS_KEY = "meeting:hands:";
    private static final String SPEAKERS_KEY = "meeting:speakers:";
    private static final String BLOCKED_KEY = "meeting:blocked:";
    private static final String MUTED_KEY = "meeting:muted:";
    private static final long TTL_HOURS = 24;

    // ─── Hand Raise ──────────────────────────────────────────────────────────

    /** Participant raises hand */
    @PostMapping("/{meetingId}/raise-hand")
    public ResponseEntity<ApiResponse> raiseHand(
            @PathVariable String meetingId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-User-Name", defaultValue = "User") String userName)
    {
        String key = RAISED_HANDS_KEY + meetingId;
        redisTemplate.opsForHash().put(key, userId.toString(), userName);
        redisTemplate.expire(key, TTL_HOURS, TimeUnit.HOURS);
        log.info("User {} raised hand in meeting {}", userId, meetingId);
        return ResponseEntity.ok(new ApiResponse("Hand raised"));
    }

    /** Participant lowers hand */
    @DeleteMapping("/{meetingId}/raise-hand")
    public ResponseEntity<ApiResponse> lowerHand(
            @PathVariable String meetingId,
            @RequestHeader("X-User-Id") UUID userId)
    {
        String key = RAISED_HANDS_KEY + meetingId;
        redisTemplate.opsForHash().delete(key, userId.toString());
        return ResponseEntity.ok(new ApiResponse("Hand lowered"));
    }

    /** Admin gets list of raised hands */
    @GetMapping("/{meetingId}/raised-hands")
    public ResponseEntity<List<ParticipantInfo>> getRaisedHands(@PathVariable String meetingId)
    {
        String key = RAISED_HANDS_KEY + meetingId;
        Map<Object, Object> hands = redisTemplate.opsForHash().entries(key);
        List<ParticipantInfo> result = new ArrayList<>();
        hands.forEach((id, name) -> result.add(new ParticipantInfo(id.toString(), name.toString())));
        return ResponseEntity.ok(result);
    }

    // ─── Speaker Management ──────────────────────────────────────────────────

    /** Admin approves a participant to speak (adds to speakers set) */
    @PostMapping("/{meetingId}/speakers/{targetUserId}")
    public ResponseEntity<ApiResponse> approveSpeaker(
            @PathVariable String meetingId,
            @PathVariable String targetUserId)
    {
        String key = SPEAKERS_KEY + meetingId;
        redisTemplate.opsForSet().add(key, targetUserId);
        redisTemplate.expire(key, TTL_HOURS, TimeUnit.HOURS);
        // Remove from raised hands
        redisTemplate.opsForHash().delete(RAISED_HANDS_KEY + meetingId, targetUserId);
        log.info("User {} approved as speaker in meeting {}", targetUserId, meetingId);
        return ResponseEntity.ok(new ApiResponse("Speaker approved"));
    }

    /** Admin removes speaker permission */
    @DeleteMapping("/{meetingId}/speakers/{targetUserId}")
    public ResponseEntity<ApiResponse> removeSpeaker(
            @PathVariable String meetingId,
            @PathVariable String targetUserId)
    {
        String key = SPEAKERS_KEY + meetingId;
        redisTemplate.opsForSet().remove(key, targetUserId);
        log.info("User {} removed from speakers in meeting {}", targetUserId, meetingId);
        return ResponseEntity.ok(new ApiResponse("Speaker removed"));
    }

    /** Get list of approved speakers */
    @GetMapping("/{meetingId}/speakers")
    public ResponseEntity<Set<String>> getSpeakers(@PathVariable String meetingId)
    {
        String key = SPEAKERS_KEY + meetingId;
        Set<String> speakers = redisTemplate.opsForSet().members(key);
        return ResponseEntity.ok(speakers != null ? speakers : Set.of());
    }

    /** Check if a user is an approved speaker */
    @GetMapping("/{meetingId}/speakers/{targetUserId}/check")
    public ResponseEntity<Map<String, Boolean>> checkSpeaker(
            @PathVariable String meetingId,
            @PathVariable String targetUserId)
    {
        String key = SPEAKERS_KEY + meetingId;
        Boolean isSpeaker = redisTemplate.opsForSet().isMember(key, targetUserId);
        return ResponseEntity.ok(Map.of("isSpeaker", Boolean.TRUE.equals(isSpeaker)));
    }

    // ─── Mute ────────────────────────────────────────────────────────────────

    /** Admin mutes a participant */
    @PostMapping("/{meetingId}/mute/{targetUserId}")
    public ResponseEntity<ApiResponse> muteParticipant(
            @PathVariable String meetingId,
            @PathVariable String targetUserId)
    {
        String key = MUTED_KEY + meetingId;
        redisTemplate.opsForSet().add(key, targetUserId);
        redisTemplate.expire(key, TTL_HOURS, TimeUnit.HOURS);
        log.info("User {} muted in meeting {}", targetUserId, meetingId);
        return ResponseEntity.ok(new ApiResponse("Participant muted"));
    }

    /** Admin unmutes a participant */
    @DeleteMapping("/{meetingId}/mute/{targetUserId}")
    public ResponseEntity<ApiResponse> unmuteParticipant(
            @PathVariable String meetingId,
            @PathVariable String targetUserId)
    {
        String key = MUTED_KEY + meetingId;
        redisTemplate.opsForSet().remove(key, targetUserId);
        return ResponseEntity.ok(new ApiResponse("Participant unmuted"));
    }

    // ─── Kick ────────────────────────────────────────────────────────────────

    /** Admin kicks a participant (removes from participants + speakers) */
    @PostMapping("/{meetingId}/kick/{targetUserId}")
    public ResponseEntity<ApiResponse> kickParticipant(
            @PathVariable String meetingId,
            @PathVariable String targetUserId)
    {
        // Remove from speakers, raised hands
        redisTemplate.opsForSet().remove(SPEAKERS_KEY + meetingId, targetUserId);
        redisTemplate.opsForHash().delete(RAISED_HANDS_KEY + meetingId, targetUserId);
        // Remove from participants
        redisTemplate.opsForSet().remove("meeting:participants:" + meetingId, targetUserId);
        log.info("User {} kicked from meeting {}", targetUserId, meetingId);
        return ResponseEntity.ok(new ApiResponse("Participant kicked"));
    }

    // ─── Block ───────────────────────────────────────────────────────────────

    /** Admin blocks a participant (kick + prevent rejoin) */
    @PostMapping("/{meetingId}/block/{targetUserId}")
    public ResponseEntity<ApiResponse> blockParticipant(
            @PathVariable String meetingId,
            @PathVariable String targetUserId)
    {
        // Add to blocked list
        String key = BLOCKED_KEY + meetingId;
        redisTemplate.opsForSet().add(key, targetUserId);
        redisTemplate.expire(key, TTL_HOURS, TimeUnit.HOURS);
        // Also kick
        redisTemplate.opsForSet().remove(SPEAKERS_KEY + meetingId, targetUserId);
        redisTemplate.opsForHash().delete(RAISED_HANDS_KEY + meetingId, targetUserId);
        redisTemplate.opsForSet().remove("meeting:participants:" + meetingId, targetUserId);
        log.info("User {} blocked from meeting {}", targetUserId, meetingId);
        return ResponseEntity.ok(new ApiResponse("Participant blocked"));
    }

    /** Check if a user is blocked */
    @GetMapping("/{meetingId}/blocked/{targetUserId}")
    public ResponseEntity<Map<String, Boolean>> checkBlocked(
            @PathVariable String meetingId,
            @PathVariable String targetUserId)
    {
        String key = BLOCKED_KEY + meetingId;
        Boolean isBlocked = redisTemplate.opsForSet().isMember(key, targetUserId);
        return ResponseEntity.ok(Map.of("isBlocked", Boolean.TRUE.equals(isBlocked)));
    }

    // ─── Room State (combined) ───────────────────────────────────────────────

    /** Get full room state for admin panel */
    @GetMapping("/{meetingId}/admin/state")
    public ResponseEntity<RoomState> getRoomState(@PathVariable String meetingId)
    {
        Set<String> speakers = redisTemplate.opsForSet().members(SPEAKERS_KEY + meetingId);
        Map<Object, Object> hands = redisTemplate.opsForHash().entries(RAISED_HANDS_KEY + meetingId);
        Set<String> muted = redisTemplate.opsForSet().members(MUTED_KEY + meetingId);
        Set<String> blocked = redisTemplate.opsForSet().members(BLOCKED_KEY + meetingId);

        List<ParticipantInfo> raisedHands = new ArrayList<>();
        hands.forEach((id, name) -> raisedHands.add(new ParticipantInfo(id.toString(), name.toString())));

        return ResponseEntity.ok(RoomState.builder()
                .speakers(speakers != null ? speakers : Set.of())
                .raisedHands(raisedHands)
                .muted(muted != null ? muted : Set.of())
                .blocked(blocked != null ? blocked : Set.of())
                .build());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantInfo
    {
        private String userId;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomState
    {
        private Set<String> speakers;
        private List<ParticipantInfo> raisedHands;
        private Set<String> muted;
        private Set<String> blocked;
    }
}
