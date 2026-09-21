package com.smhrd.hometraining.crew.entity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
    public static final int MAX_CONCEPTS = 3;

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

    /**
     * 가입 신청이 크루장 승인 없이 바로 처리되는지 나타냅니다.
     *
     * true  : 가입 신청과 동시에 즉시 크루원이 됩니다(가입 화면에 "바로가입하기"로 표시).
     * false : 가입 신청 후 크루장이 승인해야 크루원이 됩니다(기존 방식, "가입요청하기").
     */
    @Column(
            name = "auto_approve",
            nullable = false,
            columnDefinition = "boolean default false"
    )
    private boolean autoApprove = false;

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

    /**
     * concept 컬럼 하나에 콤마로 이어붙여 저장한 여러 컨셉을 리스트로 돌려줍니다.
     *
     * DB 스키마를 바꾸지 않고(ddl-auto: none, VARCHAR(20)) 최대 3개까지의 태그를
     * "다이어트,친목" 형태로 저장하는 방식입니다.
     */
    public List<String> conceptList() {

        if (concept == null || concept.isBlank()) {
            return List.of();
        }

        return Arrays.stream(concept.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * 여러 컨셉을 concept 컬럼에 저장할 콤마 구분 문자열로 합칩니다.
     */
    public static String joinConcepts(List<String> concepts) {

        if (concepts == null) {
            return null;
        }

        return concepts.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .limit(MAX_CONCEPTS)
                .collect(Collectors.joining(","));
    }
}