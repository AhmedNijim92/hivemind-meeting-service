package com.hivemind.meeting.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Generates LiveKit access tokens for participants joining a meeting room.
 * The token includes room permissions and participant identity.
 */
@RestController
@RequestMapping("/api/v1/meetings")
public class LiveKitTokenController
{
    @Value("${livekit.api-key:APIhivemind123}")
    private String apiKey;

    @Value("${livekit.api-secret:secrethivemind456789012345678901234567}")
    private String apiSecret;

    @Value("${livekit.url:}")
    private String livekitUrl;

    /**
     * Generate a LiveKit token for a participant to join a room.
     */
    @PostMapping("/{meetingId}/token")
    public ResponseEntity<TokenResponse> getToken(
            @PathVariable String meetingId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-User-Name", defaultValue = "User") String userName,
            @RequestBody(required = false) TokenRequest request)
    {
        boolean isHost = request != null && Boolean.TRUE.equals(request.getIsHost());
        String roomName = "meeting_" + meetingId;
        String identity = userId.toString();

        // Build video grant
        Map<String, Object> videoGrant = new HashMap<>();
        videoGrant.put("room", roomName);
        videoGrant.put("roomJoin", true);
        videoGrant.put("canSubscribe", true);
        videoGrant.put("canPublish", true);
        videoGrant.put("canPublishData", true);

        if (isHost) {
            videoGrant.put("roomAdmin", true);
            videoGrant.put("roomCreate", true);
        }

        // Build JWT claims for LiveKit
        Map<String, Object> claims = new HashMap<>();
        claims.put("video", videoGrant);
        claims.put("sub", identity);
        claims.put("name", userName);
        claims.put("iss", apiKey);
        claims.put("nbf", new Date().getTime() / 1000);
        claims.put("exp", (new Date().getTime() / 1000) + 86400);

        SecretKey key = Keys.hmacShaKeyFor(apiSecret.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .claims(claims)
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .signWith(key)
                .compact();

        return ResponseEntity.ok(TokenResponse.builder()
                .token(token)
                .url(livekitUrl.isEmpty() ? "__RESOLVE_FROM_ORIGIN__" : livekitUrl)
                .room(roomName)
                .identity(identity)
                .build());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenRequest
    {
        private Boolean isHost;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenResponse
    {
        private String token;
        private String url;
        private String room;
        private String identity;
    }
}
