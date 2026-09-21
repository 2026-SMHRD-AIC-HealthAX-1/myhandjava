package com.smhrd.hometraining.crew.battle.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.smhrd.hometraining.crew.entity.Crew;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "crew_battles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrewBattle {

    /**
     * 크루대전 제한시간은 2분입니다.
     */
    public static final int BATTLE_DURATION_MINUTES = 2;

    /**
     * 팀원 1명당 목표 점수. 이 점수(teamSize * TARGET_SCORE_PER_MEMBER)를 먼저
     * 채우는 크루가 있으면 2분을 다 기다리지 않고 즉시 종료됩니다.
     *
     * 렙 1회당 점수는 CrewBattleParticipant 기준 PERFECT 100 / GREAT 80 / GOOD 50점이라,
     * 예전 값(20)으로는 GOOD 렙 단 1회만으로도 즉시 종료돼버렸다(2vs2 기준 목표 40점).
     * 평균 80점짜리 렙 기준으로 팀원당 약 12~13회는 채워야 끝나도록 1000으로 올려서,
     * 압도적으로 앞서는 경우에만 조기 종료되고 보통은 2분을 채우도록 했다.
     */
    public static final int TARGET_SCORE_PER_MEMBER = 1000;

    public static int targetScoreFor(int teamSize) {
        return teamSize * TARGET_SCORE_PER_MEMBER;
    }

    public enum Status {
        REQUESTED,
        WAITING,
        MATCHED,
        ACTIVE,
        FINISHED,
        CANCELLED
    }

    /**
     * 각 크루 입장에서의 최종 대전 결과입니다.
     */
    public enum BattleResult {
        WIN,
        LOSS,
        DRAW
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 자동 매칭을 먼저 신청한 크루입니다.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "challenger_crew_id",
            nullable = false
    )
    private Crew challenger;

    /**
     * 자동 매칭으로 연결된 상대 크루입니다.
     *
     * WAITING 상태에서는 null입니다.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = true
    )
    @JoinColumn(
            name = "opponent_crew_id",
            nullable = true
    )
    private Crew opponent;

    /**
     * 자동 매칭을 신청한 사용자 ID입니다.
     */
    @Column(name = "requester_user_id")
    private Long requesterUserId;

    @Column(
            name = "exercise_type",
            nullable = false,
            length = 20
    )
    private String exerciseType;

    @Column(
            name = "duration_minutes",
            nullable = false
    )
    private int durationMinutes =
            BATTLE_DURATION_MINUTES;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 12
    )
    private Status status =
            Status.WAITING;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    /**
     * 상대 크루와 매칭된 시간입니다.
     */
    @Column(name = "matched_at")
    private LocalDateTime matchedAt;

    /**
     * 실제 대전을 시작한 시간입니다.
     */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /**
     * 대전 종료 예정 시간입니다.
     */
    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    /**
     * 실제 종료 처리 시간입니다.
     */
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    /**
     * 신청 크루의 최종 인정 운동 횟수입니다.
     */
    @Column(
            name = "challenger_total_reps",
            nullable = false
    )
    private long challengerTotalReps = 0L;

    /**
     * 상대 크루의 최종 인정 운동 횟수입니다.
     */
    @Column(
            name = "opponent_total_reps",
            nullable = false
    )
    private long opponentTotalReps = 0L;

    /**
     * 신청 크루 참가자들의 최종 합산 점수입니다.
     */
    @Column(
            name = "challenger_total_score",
            nullable = false
    )
    private long challengerTotalScore = 0L;

    /**
     * 상대 크루 참가자들의 최종 합산 점수입니다.
     */
    @Column(
            name = "opponent_total_score",
            nullable = false
    )
    private long opponentTotalScore = 0L;

    /**
     * 신청 크루 입장에서의 최종 결과입니다.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "challenger_result",
            length = 10
    )
    private BattleResult challengerResult;

    /**
     * 상대 크루 입장에서의 최종 결과입니다.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "opponent_result",
            length = 10
    )
    private BattleResult opponentResult;

    /**
     * 신청 크루에 실제로 지급된 경험치입니다.
     */
    @Column(
            name = "challenger_reward_exp",
            nullable = false
    )
    private int challengerRewardExp = 0;

    /**
     * 상대 크루에 실제로 지급된 경험치입니다.
     */
    @Column(
            name = "opponent_reward_exp",
            nullable = false
    )
    private int opponentRewardExp = 0;

    /**
     * 승리한 크루의 ID입니다.
     *
     * 무승부이거나 결과가 아직 없다면 null입니다.
     */
    @Column(name = "winner_crew_id")
    private Long winnerCrewId;

    /**
     * 무승부 여부입니다.
     */
    @Column(
            name = "draw_result",
            nullable = false
    )
    private boolean drawResult = false;

    /**
     * 최종 결과 저장 완료 여부입니다.
     */
    @Column(
            name = "result_recorded",
            nullable = false
    )
    private boolean resultRecorded = false;

    /**
     * 양쪽 크루 경험치 지급 결과가 저장됐는지 나타냅니다.
     */
    @Column(
            name = "reward_recorded",
            nullable = false
    )
    private boolean rewardRecorded = false;

    /**
     * 먼저 신청한 크루의 참가자 ID입니다.
     */
    @ElementCollection
    @CollectionTable(
            name = "crew_battle_challenger_members",
            joinColumns = @JoinColumn(name = "battle_id")
    )
    @Column(
            name = "user_id",
            nullable = false
    )
    private Set<Long> challengerUserIds =
            new HashSet<>();

    /**
     * 자동 매칭된 상대 크루의 참가자 ID입니다.
     */
    @ElementCollection
    @CollectionTable(
            name = "crew_battle_opponent_members",
            joinColumns = @JoinColumn(name = "battle_id")
    )
    @Column(
            name = "user_id",
            nullable = false
    )
    private Set<Long> opponentUserIds =
            new HashSet<>();

    /**
     * 동시에 같은 대전을 수정하는 요청을 감지합니다.
     */
    @Version
    private long version;

    @PrePersist
    void onCreate() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /**
     * 자동 매칭 대기 상태의 크루대전을 생성합니다.
     */
    public static CrewBattle waitForMatching(
            Crew challenger,
            Long requesterUserId,
            String exerciseType,
            Set<Long> challengerUserIds
    ) {

        if (challenger == null) {
            throw new IllegalArgumentException(
                    "신청 크루가 필요합니다."
            );
        }

        if (requesterUserId == null) {
            throw new IllegalArgumentException(
                    "매칭 신청자 ID가 필요합니다."
            );
        }

        if (exerciseType == null
                || exerciseType.isBlank()) {

            throw new IllegalArgumentException(
                    "운동 종류가 필요합니다."
            );
        }

        if (challengerUserIds == null
                || challengerUserIds.isEmpty()) {

            throw new IllegalArgumentException(
                    "대전 참가자가 필요합니다."
            );
        }

        if (!challengerUserIds.contains(
                requesterUserId
        )) {

            throw new IllegalArgumentException(
                    "매칭 신청자는 참가자에 포함되어야 합니다."
            );
        }

        CrewBattle battle =
                new CrewBattle();

        battle.challenger =
                challenger;

        battle.requesterUserId =
                requesterUserId;

        battle.exerciseType =
                exerciseType.trim();

        battle.durationMinutes =
                BATTLE_DURATION_MINUTES;

        battle.status =
                Status.WAITING;

        battle.challengerUserIds.addAll(
                challengerUserIds
        );

        return battle;
    }

    /**
     * 대기 중인 대전에 상대 크루를 연결합니다.
     */
    public void matchOpponent(
            Crew opponent,
            Set<Long> opponentUserIds
    ) {

        if (status != Status.WAITING) {
            throw new IllegalStateException(
                    "매칭 대기 중인 대전만 상대 크루를 연결할 수 있습니다."
            );
        }

        if (opponent == null) {
            throw new IllegalArgumentException(
                    "상대 크루가 필요합니다."
            );
        }

        if (challenger == null
                || opponent.getId().equals(
                challenger.getId()
        )) {

            throw new IllegalArgumentException(
                    "같은 크루끼리는 매칭할 수 없습니다."
            );
        }

        if (opponentUserIds == null
                || opponentUserIds.size()
                != challengerUserIds.size()) {

            throw new IllegalArgumentException(
                    "양쪽 크루의 참가 인원수가 같아야 합니다."
            );
        }

        this.opponent =
                opponent;

        this.opponentUserIds.clear();

        this.opponentUserIds.addAll(
                opponentUserIds
        );

        this.matchedAt =
                LocalDateTime.now();

        this.status =
                Status.MATCHED;
    }

    /**
     * 매칭이 완료된 크루대전을 시작합니다.
     */
    public void start() {

        if (status != Status.MATCHED) {
            throw new IllegalStateException(
                    "매칭이 완료된 대전만 시작할 수 있습니다."
            );
        }

        this.startedAt =
                LocalDateTime.now();

        this.endsAt =
                startedAt.plusMinutes(
                        BATTLE_DURATION_MINUTES
                );

        this.status =
                Status.ACTIVE;
    }

    /**
     * 매칭 대기를 취소합니다.
     */
    public void cancelWaiting() {

        if (status != Status.WAITING) {
            throw new IllegalStateException(
                    "매칭 대기 중인 대전만 취소할 수 있습니다."
            );
        }

        this.status =
                Status.CANCELLED;
    }

    /**
     * 최종 횟수와 점수를 저장하고 대전을 종료합니다.
     */
    public void finishWithResult(
            long challengerTotalReps,
            long opponentTotalReps,
            long challengerTotalScore,
            long opponentTotalScore
    ) {

        if (resultRecorded) {
            return;
        }

        if (status != Status.ACTIVE) {
            throw new IllegalStateException(
                    "진행 중인 대전만 결과를 저장할 수 있습니다."
            );
        }

        if (opponent == null) {
            throw new IllegalStateException(
                    "상대 크루 정보가 없습니다."
            );
        }

        this.challengerTotalReps =
                Math.max(challengerTotalReps, 0L);

        this.opponentTotalReps =
                Math.max(opponentTotalReps, 0L);

        this.challengerTotalScore =
                Math.max(challengerTotalScore, 0L);

        this.opponentTotalScore =
                Math.max(opponentTotalScore, 0L);

        if (this.challengerTotalScore
                > this.opponentTotalScore) {

            this.winnerCrewId =
                    challenger.getId();

            this.drawResult =
                    false;

            this.challengerResult =
                    BattleResult.WIN;

            this.opponentResult =
                    BattleResult.LOSS;

        } else if (this.opponentTotalScore
                > this.challengerTotalScore) {

            this.winnerCrewId =
                    opponent.getId();

            this.drawResult =
                    false;

            this.challengerResult =
                    BattleResult.LOSS;

            this.opponentResult =
                    BattleResult.WIN;

        } else {

            this.winnerCrewId =
                    null;

            this.drawResult =
                    true;

            this.challengerResult =
                    BattleResult.DRAW;

            this.opponentResult =
                    BattleResult.DRAW;
        }

        this.resultRecorded =
                true;

        this.status =
                Status.FINISHED;

        this.finishedAt =
                LocalDateTime.now();
    }

    /**
     * 양쪽 크루에 실제로 지급된 경험치를 저장합니다.
     */
    public void recordRewardExperience(
            int challengerRewardExp,
            int opponentRewardExp
    ) {

        if (!resultRecorded) {
            throw new IllegalStateException(
                    "대전 결과 저장 후 경험치를 기록할 수 있습니다."
            );
        }

        if (rewardRecorded) {
            return;
        }

        this.challengerRewardExp =
                Math.max(challengerRewardExp, 0);

        this.opponentRewardExp =
                Math.max(opponentRewardExp, 0);

        this.rewardRecorded =
                true;
    }

    /**
     * 기존 코드와 테스트 호환을 위한 종료 메서드입니다.
     */
    @Deprecated
    public void finish() {

        if (status == Status.FINISHED) {
            return;
        }

        this.status =
                Status.FINISHED;

        this.finishedAt =
                LocalDateTime.now();
    }

    public int getTeamSize() {
        return challengerUserIds.size();
    }

    public boolean isWaiting() {
        return status == Status.WAITING;
    }

    public boolean isMatched() {
        return status == Status.MATCHED;
    }

    public boolean isActive() {
        return status == Status.ACTIVE;
    }

    public boolean isFinished() {
        return status == Status.FINISHED;
    }

    /**
     * 기존 상대 지정 대전 코드 호환용 메서드입니다.
     */
    @Deprecated
    public static CrewBattle request(
            Crew challenger,
            Crew opponent,
            String exerciseType,
            int durationMinutes,
            Set<Long> challengerUserIds
    ) {

        CrewBattle battle =
                new CrewBattle();

        battle.challenger =
                challenger;

        battle.opponent =
                opponent;

        battle.exerciseType =
                exerciseType;

        battle.durationMinutes =
                durationMinutes;

        battle.status =
                Status.REQUESTED;

        battle.challengerUserIds.addAll(
                challengerUserIds
        );

        return battle;
    }

    /**
     * 기존 상대 지정 대전 코드 호환용 메서드입니다.
     */
    @Deprecated
    public void accept(
            Set<Long> opponentUserIds
    ) {

        this.opponentUserIds.clear();

        this.opponentUserIds.addAll(
                opponentUserIds
        );

        this.matchedAt =
                LocalDateTime.now();

        this.startedAt =
                matchedAt;

        this.endsAt =
                startedAt.plusMinutes(
                        durationMinutes
                );

        this.status =
                Status.ACTIVE;
    }
}