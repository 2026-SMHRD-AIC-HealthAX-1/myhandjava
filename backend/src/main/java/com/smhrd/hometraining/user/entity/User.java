package com.smhrd.hometraining.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_login_id", columnNames = "login_id"),
        @UniqueConstraint(name = "uk_users_nickname", columnNames = "nickname"),
        @UniqueConstraint(name = "uk_users_email", columnNames = "email")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    public enum Gender { MALE, FEMALE }
    public enum Role { USER, ADMIN }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", nullable = false, length = 50)
    private String loginId;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 120)
    private String email;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender = Gender.MALE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role = Role.USER;

    @Column(name = "avatar_index", nullable = false)
    private int avatarIndex = 0;

    @Column(name = "region_city", length = 30)
    private String regionCity;

    @Column(name = "region_gu", length = 30)
    private String regionGu;

    @Column(name = "region_dong", length = 30)
    private String regionDong;

    @Column(nullable = false)
    private int level = 1;

    /** 현재 레벨 안에서의 누적 경험치 (0 ~ EXP_PER_LEVEL 미만) */
    @Column(nullable = false)
    private int exp = 0;

    @Column(nullable = false)
    private long points = 0;

    @Column(nullable = false)
    private int streak = 0;

    @Column(name = "last_attendance_date")
    private java.time.LocalDate lastAttendanceDate;

    @Column(name = "streak_reward_claimed", nullable = false)
    private boolean streakRewardClaimed = false;

    @Column(name = "retake_tickets", nullable = false)
    private int retakeTickets = 0;

    @Column(name = "nickname_tickets", nullable = false)
    private int nicknameTickets = 2;

    @Column(name = "extra_sets", nullable = false)
    private int extraSets = 0;

    @Column(name = "sets_used_today", nullable = false)
    private int setsUsedToday = 0;

    @Column(name = "sets_reset_date")
    private java.time.LocalDate setsResetDate;

    @Column(length = 200)
    private String bio;

    @Column(name = "profile_public", nullable = false, columnDefinition = "boolean default true")
    private boolean profilePublic = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public static User register(String loginId, String passwordHash, String email, String nickname, Gender gender) {
        User u = new User();
        u.loginId = loginId;
        u.passwordHash = passwordHash;
        u.email = email;
        u.nickname = nickname;
        u.gender = gender;
        return u;
    }

    public static final int DAILY_SETS_BASE = 3;

    public int getDailySetLimit() {
        return DAILY_SETS_BASE + (level / 5) + extraSets;
    }

    public void resetDailySetsIfNeeded() {
        java.time.LocalDate today = java.time.LocalDate.now();
        if (!today.equals(setsResetDate)) {
            setsUsedToday = 0;
            setsResetDate = today;
        }
    }

    public String regionLabel() {
        StringBuilder sb = new StringBuilder();
        if (regionCity != null) sb.append(regionCity).append(' ');
        if (regionGu != null) sb.append(regionGu).append(' ');
        if (regionDong != null) sb.append(regionDong);
        return sb.toString().trim();
    }
}
