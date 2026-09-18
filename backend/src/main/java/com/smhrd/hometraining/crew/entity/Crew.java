package com.smhrd.hometraining.crew.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "crews",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_crews_name",
                columnNames = "name"
        )
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Crew {

    public static final int CREATE_COST = 1000;
    public static final int MAX_MEMBERS = 5;
    public static final int DEFAULT_GROUP_MISSION_TARGET = 300;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String name;

    @Column(length = 200)
    private String description;

    @Column(length = 20)
    private String concept;

    @Column(name = "region_city", length = 30)
    private String regionCity;

    @Column(name = "region_gu", length = 30)
    private String regionGu;

    @Column(name = "region_dong", length = 30)
    private String regionDong;

    @Column(nullable = false)
    private int level = 1;

    @Column(nullable = false)
    private int exp = 0;

    /**
     * 현재 레벨과 경험치에 도달한 시간입니다.
     *
     * 레벨과 경험치가 같은 크루의 순위를 정할 때
     * 먼저 도달한 크루가 상위 순위가 됩니다.
     *
     * 기존 크루 데이터와의 호환을 위해
     * DB 컬럼에는 null이 허용됩니다.
     */
    @Column(name = "ranking_achieved_at")
    private LocalDateTime rankingAchievedAt;

    @Column(
            name = "group_mission_exercise",
            nullable = false,
            length = 20
    )
    private String groupMissionExercise = "스쿼트";

    /**
     * 신규 가입 신청을 받을 수 있는지 나타냅니다.
     *
     * true  : 가입 신청 가능
     * false : 검색과 상세조회는 가능하지만 가입 신청은 불가능
     */
    @Column(
            name = "join_enabled",
            nullable = false,
            columnDefinition = "boolean default true"
    )
    private boolean joinEnabled = true;

    @Column(
            name = "group_mission_target",
            nullable = false
    )
    private int groupMissionTarget =
            DEFAULT_GROUP_MISSION_TARGET;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {

        LocalDateTime now =
                LocalDateTime.now();

        this.createdAt = now;

        if (this.rankingAchievedAt == null) {
            this.rankingAchievedAt = now;
        }
    }

    public static Crew create(
            String name,
            String description,
            String concept,
            String regionCity,
            String regionGu,
            String regionDong
    ) {

        Crew crew = new Crew();

        crew.name = name;
        crew.description = description;
        crew.concept = concept;
        crew.regionCity = regionCity;
        crew.regionGu = regionGu;
        crew.regionDong = regionDong;

        return crew;
    }

    /**
     * 크루 경험치가 지급될 때마다
     * 현재 레벨과 경험치에 도달한 시간을 갱신합니다.
     */
    public void markRankingAchievedNow() {
        this.rankingAchievedAt =
                LocalDateTime.now();
    }

    /**
     * 기존 크루의 rankingAchievedAt 값이 없는 경우
     * 크루 생성 시간을 순위 달성 시간으로 사용합니다.
     */
    public LocalDateTime rankingAchievedAtOrCreatedAt() {

        if (rankingAchievedAt != null) {
            return rankingAchievedAt;
        }

        return createdAt;
    }

    public String regionLabel() {

        return String.join(
                " ",
                regionCity == null ? "" : regionCity,
                regionGu == null ? "" : regionGu,
                regionDong == null ? "" : regionDong
        ).trim();
    }
}