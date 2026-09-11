package com.smhrd.hometraining.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 체형 보정(캘리브레이션) 결과 — 관절 좌표·체형 프로필은 화면마다 구조가 달라 JSON 문자열로 저장한다. */
@Entity
@Table(name = "calibration_profiles")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CalibrationProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // 캘리브레이션 스냅샷(base64 이미지)까지 JSON에 포함되어 길이가 수십 KB를 넘어간다 —
    // @Lob만 쓰면 MySQL에서 TINYTEXT(255자)로 매핑되어 저장이 매번 조용히 실패했던 원인이라,
    // LONGTEXT로 명시한다.
    @Lob
    @Column(name = "profile_json", nullable = false, columnDefinition = "LONGTEXT")
    private String profileJson;

    @Column(name = "calibrated_at", nullable = false)
    private LocalDateTime calibratedAt;

    public static CalibrationProfile of(User user, String profileJson) {
        CalibrationProfile p = new CalibrationProfile();
        p.user = user;
        p.profileJson = profileJson;
        p.calibratedAt = LocalDateTime.now();
        return p;
    }
}
