package com.smhrd.hometraining.user.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_resource_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserResourceHistory {

    public enum ResourceType {
        POINT,
        EXPERIENCE,
        RETAKE_TICKET,
        NICKNAME_TICKET,
        EXTRA_SET
    }

    public enum Reason {
        ATTENDANCE,
        STREAK_REWARD,
        EXERCISE_REWARD,
        MISSION_REWARD,
        SHOP_PURCHASE,
        CREW_CREATE,
        EXERCISE_SESSION,
        PROFILE_UPDATE,
        SYSTEM_REWARD
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 30)
    private ResourceType resourceType;

    @Column(name = "change_amount", nullable = false)
    private long changeAmount;

    @Column(name = "balance_before", nullable = false)
    private long balanceBefore;

    @Column(name = "balance_after", nullable = false)
    private long balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Reason reason;

    @Column(name = "source_id", length = 100)
    private String sourceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static UserResourceHistory create(
            User user,
            ResourceType resourceType,
            long changeAmount,
            long balanceBefore,
            long balanceAfter,
            Reason reason,
            String sourceId
    ) {

        if (user == null || resourceType == null || reason == null) {
            throw new IllegalArgumentException(
                    "User, resource type and reason are required."
            );
        }

        UserResourceHistory history =
                new UserResourceHistory();

        history.user = user;
        history.resourceType = resourceType;
        history.changeAmount = changeAmount;
        history.balanceBefore = balanceBefore;
        history.balanceAfter = balanceAfter;
        history.reason = reason;
        history.sourceId = normalizeSourceId(sourceId);

        return history;
    }

    private static String normalizeSourceId(String sourceId) {

        if (sourceId == null || sourceId.isBlank()) {
            return null;
        }

        String trimmed = sourceId.trim();
        return trimmed.length() <= 100
                ? trimmed
                : trimmed.substring(0, 100);
    }

    @PrePersist
    private void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
