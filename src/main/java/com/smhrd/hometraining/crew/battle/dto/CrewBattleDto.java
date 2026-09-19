package com.smhrd.hometraining.crew.battle.dto;

import java.time.LocalDateTime;
import java.util.Set;

import com.smhrd.hometraining.crew.battle.entity.CrewBattle;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class CrewBattleDto {

    private CrewBattleDto() {
    }

    /**
     * 자동 매칭 신청 요청입니다.
     *
     * participantUserIds에는 매칭 신청자 본인의 ID도
     * 반드시 포함되어야 합니다.
     */
    public record CreateRequest(

            @Min(
                    value = 2,
                    message = "대전 인원은 최소 2명입니다."
            )
            @Max(
                    value = 5,
                    message = "대전 인원은 최대 5명입니다."
            )
            int teamSize,

            @NotNull(
                    message = "참가자 정보가 필요합니다."
            )
            @Size(
                    min = 2,
                    max = 5,
                    message = "참가자는 2명부터 5명까지 선택할 수 있습니다."
            )
            Set<@NotNull Long> participantUserIds,

            @NotBlank(
                    message = "운동 종류가 필요합니다."
            )
            @Size(
                    max = 20,
                    message = "운동 종류는 20자 이하여야 합니다."
            )
            String exerciseType

    ) {

        /**
         * 기존 테스트 코드와의 임시 호환 생성자입니다.
         *
         * 상대 크루 ID와 대전 시간은 사용하지 않습니다.
         */
        @Deprecated
        public CreateRequest(
                Long ignoredOpponentCrewId,
                int teamSize,
                Set<Long> participantUserIds,
                String exerciseType,
                int ignoredDurationMinutes
        ) {

            this(
                    teamSize,
                    participantUserIds,
                    exerciseType
            );
        }

        /**
         * 자동 매칭에서는 상대 크루를 직접 받지 않습니다.
         */
        @Deprecated
        public Long opponentCrewId() {
            return null;
        }

        /**
         * 대전 시간은 서버에서 2분으로 고정합니다.
         */
        @Deprecated
        public int durationMinutes() {
            return CrewBattle.BATTLE_DURATION_MINUTES;
        }
    }

    /**
     * 기존 상대 크루 수락 방식과의 임시 호환 DTO입니다.
     *
     * 자동 매칭에서는 사용하지 않습니다.
     */
    @Deprecated
    public record AcceptRequest(

            @NotNull
            @Size(min = 2, max = 5)
            Set<@NotNull Long> participantUserIds

    ) {
    }

    /**
     * 크루대전 상태와 결과 응답입니다.
     */
    public record Response(

            Long id,
            CrewBattle.Status status,
            int teamSize,

            Long requesterUserId,

            Long challengerCrewId,
            String challengerCrewName,

            /**
             * 신청 크루의 인정 운동 횟수입니다.
             */
            long challengerReps,

            /**
             * 신청 크루 참가자들의 합산 점수입니다.
             */
            long challengerScore,

            Long opponentCrewId,
            String opponentCrewName,

            /**
             * 상대 크루의 인정 운동 횟수입니다.
             */
            long opponentReps,

            /**
             * 상대 크루 참가자들의 합산 점수입니다.
             */
            long opponentScore,

            String exerciseType,

            LocalDateTime createdAt,
            LocalDateTime matchedAt,
            LocalDateTime startedAt,
            LocalDateTime endsAt,
            LocalDateTime finishedAt,

            long remainingSeconds,

            /**
             * 승리한 크루의 ID입니다.
             *
             * 무승부이거나 결과가 아직 없다면 null입니다.
             */
            Long winnerCrewId,

            /**
             * 무승부 여부입니다.
             */
            boolean drawResult,

            /**
             * 최종 결과가 DB에 저장됐는지 나타냅니다.
             */
            boolean resultRecorded

    ) {

        /**
         * 직전 자동 매칭 응답 코드와의 임시 호환 생성자입니다.
         */
        @Deprecated
        public Response(
                Long id,
                CrewBattle.Status status,
                int teamSize,

                Long requesterUserId,

                Long challengerCrewId,
                String challengerCrewName,
                long challengerScore,

                Long opponentCrewId,
                String opponentCrewName,
                long opponentScore,

                String exerciseType,

                LocalDateTime createdAt,
                LocalDateTime matchedAt,
                LocalDateTime startedAt,
                LocalDateTime endsAt,
                LocalDateTime finishedAt,

                long remainingSeconds,
                Long winnerCrewId
        ) {

            this(
                    id,
                    status,
                    teamSize,

                    requesterUserId,

                    challengerCrewId,
                    challengerCrewName,
                    0L,
                    challengerScore,

                    opponentCrewId,
                    opponentCrewName,
                    0L,
                    opponentScore,

                    exerciseType,

                    createdAt,
                    matchedAt,
                    startedAt,
                    endsAt,
                    finishedAt,

                    remainingSeconds,
                    winnerCrewId,
                    status == CrewBattle.Status.FINISHED
                            && winnerCrewId == null,
                    false
            );
        }

        /**
         * 기존 상대 지정 대전 응답 코드와의 임시 호환 생성자입니다.
         */
        @Deprecated
        public Response(
                Long id,
                CrewBattle.Status status,
                int teamSize,

                Long challengerCrewId,
                String challengerCrewName,
                long challengerScore,

                Long opponentCrewId,
                String opponentCrewName,
                long opponentScore,

                String exerciseType,
                LocalDateTime startedAt,
                LocalDateTime endsAt,

                long remainingSeconds,
                Long winnerCrewId
        ) {

            this(
                    id,
                    status,
                    teamSize,

                    null,

                    challengerCrewId,
                    challengerCrewName,
                    0L,
                    challengerScore,

                    opponentCrewId,
                    opponentCrewName,
                    0L,
                    opponentScore,

                    exerciseType,

                    null,
                    null,
                    startedAt,
                    endsAt,
                    null,

                    remainingSeconds,
                    winnerCrewId,
                    status == CrewBattle.Status.FINISHED
                            && winnerCrewId == null,
                    false
            );
        }
    }
}