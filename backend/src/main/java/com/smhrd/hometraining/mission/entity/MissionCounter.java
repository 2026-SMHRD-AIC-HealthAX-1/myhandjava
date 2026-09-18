package com.smhrd.hometraining.mission.entity;

import com.smhrd.hometraining.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** 하루 단위로 리셋되는 미션 진행 카운터 (스쿼트 세션 기준). */
@Entity
@Table(name = "mission_counters", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_mission_counters_user_date_exercise",
                columnNames = {"user_id", "counter_date", "exercise_type"}
        )
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissionCounter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "counter_date", nullable = false)
    private LocalDate counterDate;

    @Column(name = "exercise_type", nullable = false, length = 30)
    private String exerciseType;

    private int reps = 0;
    private int perfect = 0;
    private int sessions = 0;
    private int missFreeSession = 0;
    private int accSession = 0;

    public static MissionCounter startFor(
            User user,
            LocalDate date,
            String exerciseType
    ) {
        MissionCounter c = new MissionCounter();
        c.user = user;
        c.counterDate = date;
        c.exerciseType = exerciseType;
        return c;
    }

    public int valueOf(MissionMetric metric) {
        return switch (metric) {
            case REPS -> reps;
            case PERFECT -> perfect;
            case SESSIONS -> sessions;
            case MISS_FREE_SESSION -> missFreeSession;
            case ACC_SESSION -> accSession;
        };
    }
}
