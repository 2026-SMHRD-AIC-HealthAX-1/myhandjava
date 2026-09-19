package com.smhrd.hometraining.crew.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.smhrd.hometraining.crew.entity.CrewExperienceHistory;

/**
 * 크루 경험치 지급 내역 응답입니다.
 */
public record CrewExperienceHistoryResponse(

        Long id,

        String sourceType,
        Long sourceId,

        int expAmount,

        int levelBefore,
        int levelAfter,

        int expBefore,
        int expAfter,

        LocalDate weekStart,
        LocalDateTime createdAt

) {

    public static CrewExperienceHistoryResponse from(
            CrewExperienceHistory history
    ) {

        return new CrewExperienceHistoryResponse(
                history.getId(),

                history.getSourceType().name(),
                history.getSourceId(),

                history.getExpAmount(),

                history.getLevelBefore(),
                history.getLevelAfter(),

                history.getExpBefore(),
                history.getExpAfter(),

                history.getWeekStart(),
                history.getCreatedAt()
        );
    }
}