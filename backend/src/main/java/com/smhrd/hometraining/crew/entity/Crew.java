package com.smhrd.hometraining.crew.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "crews", uniqueConstraints = @UniqueConstraint(name = "uk_crews_name", columnNames = "name"))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Crew {

    public static final int CREATE_COST = 100;
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

    @Column(name = "group_mission_exercise", nullable = false, length = 20)
    private String groupMissionExercise = "스쿼트";

    @Column(name = "group_mission_target", nullable = false)
    private int groupMissionTarget = DEFAULT_GROUP_MISSION_TARGET;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { this.createdAt = LocalDateTime.now(); }

    public static Crew create(String name, String description, String concept,
                               String regionCity, String regionGu, String regionDong) {
        Crew c = new Crew();
        c.name = name;
        c.description = description;
        c.concept = concept;
        c.regionCity = regionCity;
        c.regionGu = regionGu;
        c.regionDong = regionDong;
        return c;
    }

    public String regionLabel() {
        return String.join(" ", regionCity == null ? "" : regionCity,
                regionGu == null ? "" : regionGu, regionDong == null ? "" : regionDong).trim();
    }
}
