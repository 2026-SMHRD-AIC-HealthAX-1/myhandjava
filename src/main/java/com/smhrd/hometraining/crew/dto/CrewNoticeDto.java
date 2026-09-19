package com.smhrd.hometraining.crew.dto;

import com.smhrd.hometraining.crew.entity.CrewNotice;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record CrewNoticeDto(
        Long id,
        String authorNickname,
        String title,
        String body,
        LocalDateTime createdAt
) {
    public static CrewNoticeDto from(CrewNotice n) {
        return new CrewNoticeDto(n.getId(), n.getAuthor().getNickname(), n.getTitle(), n.getBody(), n.getCreatedAt());
    }

    public record Create(@NotBlank String title, @NotBlank String body) {}
}
