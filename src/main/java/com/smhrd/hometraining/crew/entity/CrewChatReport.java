package com.smhrd.hometraining.crew.entity;

import com.smhrd.hometraining.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 크루채팅 메시지 신고 — 관리자 "크루채팅 신고 관리" 화면에서 처리한다. */
@Entity
@Table(name = "crew_chat_reports")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrewChatReport {

    public enum Status {
        PENDING,
        RESOLVED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crew_id", nullable = false)
    private Crew crew;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private CrewChatMessage message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", nullable = false)
    private User targetUser;

    /** 신고 시점의 채팅 내용을 그대로 남겨둔다 — 나중에 메시지가 지워져도 신고 내역은 남아야 한다. */
    @Column(name = "message_text_snapshot", length = 500)
    private String messageTextSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status = Status.PENDING;

    @Column(name = "reported_at", nullable = false)
    private LocalDateTime reportedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public static CrewChatReport of(Crew crew, CrewChatMessage message, User reporter, User targetUser) {
        CrewChatReport r = new CrewChatReport();
        r.crew = crew;
        r.message = message;
        r.reporter = reporter;
        r.targetUser = targetUser;
        r.messageTextSnapshot = message.getText();
        r.reportedAt = LocalDateTime.now();
        return r;
    }

    public void resolve() {
        this.status = Status.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
    }
}
