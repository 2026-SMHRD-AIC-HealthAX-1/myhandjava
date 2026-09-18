package com.smhrd.hometraining.crew.dto;

import com.smhrd.hometraining.crew.entity.CrewChatMessage;

import java.time.LocalDateTime;

public record CrewChatMessageDto(
        Long id,
        Long crewId,
        Long senderId,
        String senderNickname,
        String text,
        LocalDateTime sentAt
) {
    public static CrewChatMessageDto from(CrewChatMessage m) {
        return new CrewChatMessageDto(m.getId(), m.getCrew().getId(), m.getSender().getId(),
                m.getSender().getNickname(), m.getText(), m.getSentAt());
    }
}
