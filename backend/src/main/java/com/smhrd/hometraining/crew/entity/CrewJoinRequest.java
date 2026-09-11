package com.smhrd.hometraining.crew.entity;

import com.smhrd.hometraining.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "crew_join_requests")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrewJoinRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crew_id", nullable = false)
    private Crew crew;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Column(length = 200)
    private String message;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    public static CrewJoinRequest of(Crew crew, User requester, String message) {
        CrewJoinRequest r = new CrewJoinRequest();
        r.crew = crew;
        r.requester = requester;
        r.message = message;
        r.requestedAt = LocalDateTime.now();
        return r;
    }
}
