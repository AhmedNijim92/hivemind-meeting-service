package com.hivemind.meeting.controller;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * REST API for chat messages stored in Redis.
 * Supports group chat, private chat, and meeting room chat.
 * Messages are stored as Redis lists with a TTL of 24 hours.
 * Frontend polls for new messages.
 */
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatRestController
{
    private final RedisTemplate<String, String> redisTemplate;
    private static final String CHAT_KEY_PREFIX = "chat:messages:";
    private static final long MESSAGE_TTL_HOURS = 24;
    private static final int MAX_MESSAGES = 200;

    /**
     * Send a message to a conversation (group, private, or meeting).
     */
    @PostMapping("/{conversationId}")
    public ResponseEntity<ChatMessageDto> sendMessage(
            @PathVariable String conversationId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-User-Name", defaultValue = "Unknown") String userName,
            @RequestBody SendMessageRequest request)
    {
        ChatMessageDto message = ChatMessageDto.builder()
                .id(UUID.randomUUID().toString())
                .conversationId(conversationId)
                .senderId(userId.toString())
                .senderName(userName)
                .content(request.getContent())
                .imageUrl(request.getImageUrl())
                .timestamp(LocalDateTime.now().toString())
                .build();

        String key = CHAT_KEY_PREFIX + conversationId;
        // Store as JSON string in Redis list
        String json = toJson(message);
        redisTemplate.opsForList().rightPush(key, json);
        // Trim to max messages
        redisTemplate.opsForList().trim(key, -MAX_MESSAGES, -1);
        // Set TTL
        redisTemplate.expire(key, MESSAGE_TTL_HOURS, TimeUnit.HOURS);

        return ResponseEntity.ok(message);
    }

    /**
     * Get messages for a conversation.
     * Optional 'after' param to get only messages after a certain index (for polling).
     */
    @GetMapping("/{conversationId}")
    public ResponseEntity<List<String>> getMessages(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "0") long after)
    {
        String key = CHAT_KEY_PREFIX + conversationId;
        List<String> messages = redisTemplate.opsForList().range(key, after, -1);
        return ResponseEntity.ok(messages != null ? messages : List.of());
    }

    /**
     * Get message count (for checking if there are new messages).
     */
    @GetMapping("/{conversationId}/count")
    public ResponseEntity<Long> getMessageCount(@PathVariable String conversationId)
    {
        String key = CHAT_KEY_PREFIX + conversationId;
        Long size = redisTemplate.opsForList().size(key);
        return ResponseEntity.ok(size != null ? size : 0);
    }

    private String toJson(ChatMessageDto msg)
    {
        return String.format(
            "{\"id\":\"%s\",\"conversationId\":\"%s\",\"senderId\":\"%s\",\"senderName\":\"%s\",\"content\":\"%s\",\"imageUrl\":%s,\"timestamp\":\"%s\"}",
            msg.getId(), msg.getConversationId(), msg.getSenderId(),
            msg.getSenderName().replace("\"", "\\\""),
            msg.getContent().replace("\"", "\\\"").replace("\n", "\\n"),
            msg.getImageUrl() != null ? "\"" + msg.getImageUrl() + "\"" : "null",
            msg.getTimestamp()
        );
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessageDto
    {
        private String id;
        private String conversationId;
        private String senderId;
        private String senderName;
        private String content;
        private String imageUrl;
        private String timestamp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendMessageRequest
    {
        private String content;
        private String imageUrl;
    }
}
