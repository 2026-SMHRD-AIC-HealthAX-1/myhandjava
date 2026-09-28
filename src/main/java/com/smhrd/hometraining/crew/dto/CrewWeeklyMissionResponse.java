package com.smhrd.hometraining.crew.dto;

import java.time.LocalDate;
import java.util.List;

import com.smhrd.hometraining.crew.entity.CrewWeeklyContribution;
import com.smhrd.hometraining.crew.entity.CrewWeeklyMission;

public record CrewWeeklyMissionResponse(

        Long id,
        Long crewId,
        Long definitionId,
        String exerciseType,
        String label,

        LocalDate weekStart,
        LocalDate weekEnd,

        int targetReps,
        int currentReps,
        int remainingReps,

        int rewardExp,

        boolean completed,
        boolean rewardGranted,

        List<MemberContribution> contributions

) {

    public static CrewWeeklyMissionResponse of(
            CrewWeeklyMission mission,
            List<CrewWeeklyContribution> contributions
    ) {

        List<MemberContribution> memberContributions =
                contributions == null
                        ? List.of()
                        : contributions.stream()
                                .map(MemberContribution::from)
                                .toList();

        int remainingReps =
                Math.max(
                        mission.getTargetReps()
                                - mission.getCurrentReps(),
                        0
                );

        return new CrewWeeklyMissionResponse(
                mission.getId(),
                mission.getCrew().getId(),
                mission.getDefinition().getId(),
                mission.getExerciseType(),
                mission.getLabel(),

                mission.getWeekStart(),
                mission.getWeekEnd(),

                mission.getTargetReps(),
                mission.getCurrentReps(),
                remainingReps,

                mission.getRewardExp(),

                mission.isCompleted(),
                mission.isRewardGranted(),

                memberContributions
        );
    }

    /**
     * 크루원 한 명의 주간 기여도입니다.
     */
    public record MemberContribution(

            Long userId,
            String nickname,

            int totalReps,
            int recognizedReps

    ) {

        public static MemberContribution from(
                CrewWeeklyContribution contribution
        ) {

            return new MemberContribution(
                    contribution.getUser().getId(),
                    contribution.getUser().getNickname(),

                    contribution.getTotalReps(),
                    contribution.getRecognizedReps()
            );
        }
    }
}
