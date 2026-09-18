package com.smhrd.hometraining.support.dto;

import com.smhrd.hometraining.support.entity.SupportTicket;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record SupportTicketDto(
        Long id,
        String authorNickname,
        String type,
        String title,
        String body,
        String status,
        String reply,
        LocalDateTime createdAt,
        LocalDateTime answeredAt
) {
    public static SupportTicketDto from(SupportTicket t) {
        return new SupportTicketDto(t.getId(), t.getAuthor().getNickname(), t.getType().name(), t.getTitle(),
                t.getBody(), t.getStatus().name(), t.getReply(), t.getCreatedAt(), t.getAnsweredAt());
    }

    public record Create(@NotBlank String type, @NotBlank String title, @NotBlank String body) {}

    public record Reply(@NotBlank String reply) {}
}
