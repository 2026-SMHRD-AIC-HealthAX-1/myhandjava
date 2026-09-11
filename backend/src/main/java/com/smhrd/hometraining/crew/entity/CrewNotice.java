package com.smhrd.hometraining.crew.entity;

import com.smhrd.hometraining.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "crew_notices")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrewNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crew_id", nullable = false)
    private Crew crew;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 60)
    private String title;

    @Column(nullable = false, length = 500)
    private String body;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static CrewNotice of(Crew crew, User author, String title, String body) {
        CrewNotice n = new CrewNotice();
        n.crew = crew;
        n.author = author;
        n.title = title;
        n.body = body;
        n.createdAt = LocalDateTime.now();
        return n;
    }
}
