package com.hivemind.meeting.controller;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

/**
 * WebSocket controller for real-time chat messages.
 * Supports group chat, private messages, and meeting room chat.
 *
 * Clients subscribe to:
 *   /topic/chat/{conversationId}    — group or private chat
 *   /topic/meeting/{meetingId}/chat — meeting room live chat
 *
 * Clients send to:
 *   /app/chat/{conversationId}       — send message to a chat
 *   /app/meeting/{meetingId}/chat    — send message to meeting room
 */
@Controller
public class ChatController
{
    /**
     * Group / Private chat — broadcasts message to all subscribers of the conversation.
     */
    @MessageMapping("/chat/{conversationId}")
    @SendTo("/topic/chat/{conversationId}")
    public ChatMessage sendChatMessage(
            @DestinationVariable String conversationId,
            ChatMessage message)
    {
        message.setTimestamp(LocalDateTime.now().toString());
        return message;
    }

    /**
     * Meeting room live chat — broadcasts to all participants in the meeting.
     */
    @MessageMapping("/meeting/{meetingId}/chat")
    @SendTo("/topic/meeting/{meetingId}/chat")
    public ChatMessage sendMeetingMessage(
            @DestinationVariable String meetingId,
            ChatMessage message)
    {
        message.setTimestamp(LocalDateTime.now().toString());
        return message;
    }

    /**
     * Meeting room reactions — broadcasts reactions to all participants.
     */
    @MessageMapping("/meeting/{meetingId}/reaction")
    @SendTo("/topic/meeting/{meetingId}/reaction")
    public ReactionMessage sendReaction(
            @DestinationVariable String meetingId,
            ReactionMessage reaction)
    {
        return reaction;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessage
    {
        private String id;
        private String senderId;
        private String senderName;
        private String content;
        private String imageUrl;
        private String timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReactionMessage
    {
        private String userId;
        private String emoji;
    }
}
