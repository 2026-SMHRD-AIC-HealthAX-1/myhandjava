package com.smhrd.hometraining.crew.battle.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.smhrd.hometraining.crew.battle.entity.CrewBattle;

public record CrewBattleResultResponse(

        Long battleId,
        CrewBattle.Status status,

        String exerciseType,
        int teamSize,
        int durationMinutes,

        LocalDateTime createdAt,
        LocalDateTime matchedAt,
        LocalDateTime startedAt,
        LocalDateTime endsAt,
        LocalDateTime finishedAt,

        TeamResult challenger,
        TeamResult opponent,

        Long winnerCrewId,
        boolean drawResult,
        boolean resultRecorded,
        boolean rewardRecorded

) {

    /**
     * 크루 한 팀의 대전 결과입니다.
     */
    public record TeamResult(

            Long crewId,
            String crewName,

            CrewBattle.BattleResult result,

            long totalReps,
            long totalScore,

            int rewardExp,

            List<CrewBattleParticipantResponse> participants

    ) {

        public TeamResult {

            participants =
                    participants == null
                            ? List.of()
                            : List.copyOf(participants);
        }
    }
}