package com.smhrd.hometraining.support.entity;

import com.smhrd.hometraining.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "support_tickets")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupportTicket {

    public enum Type { ERROR, FEATURE_REQUEST, ETC }
    public enum Status { RECEIVED, IN_PROGRESS, ANSWERED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(nullable = false, length = 1000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.RECEIVED;

    @Column(length = 1000)
    private String reply;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    public static SupportTicket create(User author, Type type, String title, String body) {
        SupportTicket t = new SupportTicket();
        t.author = author;
        t.type = type;
        t.title = title;
        t.body = body;
        t.createdAt = LocalDateTime.now();
        return t;
    }
}
