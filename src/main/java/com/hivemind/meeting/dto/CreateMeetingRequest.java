package com.hivemind.meeting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateMeetingRequest
{
    @NotNull(message = "Group ID is required")
    private UUID groupId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private String privacy = "PUBLIC";

    private LocalDateTime scheduledAt;
}
