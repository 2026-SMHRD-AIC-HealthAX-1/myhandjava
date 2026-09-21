package com.smhrd.hometraining.user.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_users_login_id",
                        columnNames = "login_id"
                ),
                @UniqueConstraint(
                        name = "uk_users_nickname",
                        columnNames = "nickname"
                ),
                @UniqueConstraint(
                        name = "uk_users_email",
                        columnNames = "email"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    public enum Gender {
        MALE,
        FEMALE
    }

    public enum Role {
        USER,
        ADMIN
    }

    public enum Status {
        ACTIVE,
        SUSPENDED
    }

    /**
     * 무료 운동 횟수 계산에 사용하는 대한민국 시간대입니다.
     */
    public static final ZoneId KOREA_ZONE_ID =
            ZoneId.of("Asia/Seoul");

    /**
     * 하루 무료 운동 횟수입니다.
     *
     * 레벨과 관계없이 하루 총 3회로 고정합니다.
     */
    public static final int DAILY_FREE_SET_LIMIT = 3;

    /**
     * 기존 코드와의 호환성을 위해 유지합니다.
     */
    public static final int DAILY_SETS_BASE =
            DAILY_FREE_SET_LIMIT;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "login_id",
            nullable = false,
            length = 50
    )
    private String loginId;

    @Column(
            name = "password_hash",
            nullable = false
    )
    private String passwordHash;

    @Column(
            nullable = false,
            length = 120
    )
    private String email;

    @Column(
            nullable = false,
            length = 30
    )
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 10
    )
    private Gender gender = Gender.MALE;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "grade",
            nullable = false,
            length = 20,
            columnDefinition = "varchar(20) default 'IRON'"
    )
    private UserGrade grade = UserGrade.IRON;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 10
    )
    private Role role = Role.USER;

    /**
     * 관리자가 정지시킨 계정인지 나타냅니다.
     *
     * SUSPENDED 계정은 로그인이 거부됩니다(AuthService.issueToken 참고).
     */
    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 10,
            columnDefinition = "varchar(10) default 'ACTIVE'"
    )
    private Status status = Status.ACTIVE;

    @Column(
            name = "avatar_index",
            nullable = false
    )
    private int avatarIndex = 0;

    @Column(
            name = "region_city",
            length = 30
    )
    private String regionCity;

    @Column(
            name = "region_gu",
            length = 30
    )
    private String regionGu;

    @Column(
            name = "region_dong",
            length = 30
    )
    private String regionDong;

    @Column(nullable = false)
    private int level = 1;

    /**
     * 현재 레벨 안에서 누적된 경험치입니다.
     */
    @Column(nullable = false)
    private int exp = 0;

    @Column(nullable = false)
    private long points = 0;

    @Column(nullable = false)
    private int streak = 0;

    @Column(name = "last_attendance_date")
    private LocalDate lastAttendanceDate;

    @Column(
            name = "streak_reward_claimed",
            nullable = false
    )
    private boolean streakRewardClaimed = false;

    @Column(
            name = "retake_tickets",
            nullable = false
    )
    private int retakeTickets = 0;

    @Column(
            name = "nickname_tickets",
            nullable = false
    )
    private int nicknameTickets = 2;

    @Column(
            name = "sets_used_today",
            nullable = false
    )
    private int setsUsedToday = 0;

    @Column(name = "sets_reset_date")
    private LocalDate setsResetDate;

    @Column(length = 200)
    private String bio;

    @Column(
            name = "profile_public",
            nullable = false,
            columnDefinition = "boolean default true"
    )
    private boolean profilePublic = true;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    /**
     * 새 사용자 저장 전 기본값을 설정합니다.
     */
    @PrePersist
    void onCreate() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (grade == null) {
            grade = UserGrade.IRON;
        }
    }

    /**
     * 새로운 일반 회원을 생성합니다.
     */
    public static User register(
            String loginId,
            String passwordHash,
            String email,
            String nickname,
            Gender gender
    ) {

        User user = new User();

        user.loginId = loginId;
        user.passwordHash = passwordHash;
        user.email = email;
        user.nickname = nickname;
        user.gender =
                gender == null
                        ? Gender.MALE
                        : gender;

        /*
         * 선택한 성별에 맞는 기본 캐릭터를 설정합니다.
         *
         * 남성: 0
         * 여성: 1
         */
        user.avatarIndex =
                user.gender == Gender.FEMALE
                        ? 1
                        : 0;

        user.grade = UserGrade.IRON;
        user.level = 1;
        user.exp = 0;

        return user;
    }

    /**
     * 하루 무료 운동 한도를 반환합니다.
     *
     * 레벨과 관계없이 3회입니다.
     */
    public int getDailySetLimit() {
        return DAILY_FREE_SET_LIMIT;
    }

    /**
     * 오늘 남은 무료 운동 횟수를 반환합니다.
     */
    public int getRemainingDailyFreeSets() {

        return Math.max(
                getDailySetLimit() - setsUsedToday,
                0
        );
    }

    /**
     * 오늘 무료 운동을 사용할 수 있는지 확인합니다.
     */
    public boolean hasRemainingDailyFreeSet() {

        return getRemainingDailyFreeSets() > 0;
    }

    /**
     * 대한민국 날짜가 변경됐다면
     * 오늘 사용한 무료 운동 횟수를 초기화합니다.
     */
    public void resetDailySetsIfNeeded() {

        LocalDate today =
                LocalDate.now(KOREA_ZONE_ID);

        if (!today.equals(setsResetDate)) {
            setsUsedToday = 0;
            setsResetDate = today;
        }
    }

    /**
     * 지역 정보를 하나의 문자열로 반환합니다.
     */
    public String regionLabel() {

        StringBuilder region =
                new StringBuilder();

        if (regionCity != null) {
            region.append(regionCity).append(' ');
        }

        if (regionGu != null) {
            region.append(regionGu).append(' ');
        }

        if (regionDong != null) {
            region.append(regionDong);
        }

        return region.toString().trim();
    }
}