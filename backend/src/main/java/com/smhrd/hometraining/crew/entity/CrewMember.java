package com.smhrd.hometraining.crew.entity;

import java.time.LocalDateTime;

import com.smhrd.hometraining.user.entity.User;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "crew_members",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_crew_members_user",
                        columnNames = "user_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrewMember {

    public enum Role {
        LEADER,
        MEMBER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 사용자가 소속된 크루입니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "crew_id",
            nullable = false
    )
    private Crew crew;

    /**
     * 크루에 가입한 사용자입니다.
     *
     * 사용자 한 명은 하나의 크루에만
     * 가입할 수 있습니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    /**
     * 크루 내 역할입니다.
     *
     * LEADER: 크루장
     * MEMBER: 일반 크루원
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;

    /**
     * 크루에 가입한 시간입니다.
     */
    @Column(
            name = "joined_at",
            nullable = false
    )
    private LocalDateTime joinedAt;

    /**
     * 새로운 크루 가입 관계를 생성합니다.
     */
    public static CrewMember of(
            Crew crew,
            User user,
            Role role
    ) {

        if (crew == null) {
            throw new IllegalArgumentException(
                    "크루 정보가 필요합니다."
            );
        }

        if (user == null) {
            throw new IllegalArgumentException(
                    "사용자 정보가 필요합니다."
            );
        }

        if (role == null) {
            throw new IllegalArgumentException(
                    "크루 역할이 필요합니다."
            );
        }

        CrewMember member =
                new CrewMember();

        member.crew = crew;
        member.user = user;
        member.role = role;
        member.joinedAt = LocalDateTime.now();

        return member;
    }

    /**
     * 현재 크루원을 크루장으로 변경합니다.
     */
    public void promoteToLeader() {
        this.role = Role.LEADER;
    }

    /**
     * 현재 크루장을 일반 크루원으로 변경합니다.
     */
    public void demoteToMember() {
        this.role = Role.MEMBER;
    }

    /**
     * 현재 사용자가 크루장인지 확인합니다.
     */
    public boolean isLeader() {
        return this.role == Role.LEADER;
    }
}