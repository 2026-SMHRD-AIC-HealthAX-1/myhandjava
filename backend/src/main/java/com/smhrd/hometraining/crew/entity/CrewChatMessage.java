package com.smhrd.hometraining.crew.entity;

import com.smhrd.hometraining.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "crew_chat_messages")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrewChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crew_id", nullable = false)
    private Crew crew;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, length = 500)
    private String text;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    public static CrewChatMessage of(Crew crew, User sender, String text) {
        CrewChatMessage m = new CrewChatMessage();
        m.crew = crew;
        m.sender = sender;
        m.text = text;
        m.sentAt = LocalDateTime.now();
        return m;
    }
}
