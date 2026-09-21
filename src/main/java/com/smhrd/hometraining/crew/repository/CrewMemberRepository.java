package com.smhrd.hometraining.crew.repository;

import com.smhrd.hometraining.crew.entity.CrewMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// [DB 접근 지점] crew_members 테이블(회원 1명당 최대 1개 크루 소속 — user_id UNIQUE).
public interface CrewMemberRepository extends JpaRepository<CrewMember, Long> {
    Optional<CrewMember> findByUserId(Long userId);
    List<CrewMember> findByCrewIdOrderByRoleAscJoinedAtAsc(Long crewId);
    long countByCrewId(Long crewId);
    void deleteByCrewIdAndUserId(Long crewId, Long userId);
}
