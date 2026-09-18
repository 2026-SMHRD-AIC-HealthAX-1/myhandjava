package com.smhrd.hometraining.mission.entity;

import com.smhrd.hometraining.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "missions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MissionMetric metric;

    @Column(nullable = false)
    private int target;

    @Column(nullable = false)
    private int reward;

    @Column(nullable = false, length = 60)
    private String label;

    @Column(name = "assigned_date", nullable = false)
    private LocalDate assignedDate;

    @Column(nullable = false)
    private boolean claimed = false;

    public static Mission generate(User user, MissionMetric metric, int target, LocalDate date) {
        Mission m = new Mission();
        m.user = user;
        m.metric = metric;
        m.target = target;
        m.reward = metric.rewardFor(target);
        m.label = metric.label(target);
        m.assignedDate = date;
        return m;
    }
}
