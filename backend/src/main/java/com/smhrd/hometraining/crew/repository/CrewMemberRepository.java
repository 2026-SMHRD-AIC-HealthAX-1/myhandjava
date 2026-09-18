package com.smhrd.hometraining.crew.repository;

import com.smhrd.hometraining.crew.entity.CrewMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CrewMemberRepository extends JpaRepository<CrewMember, Long> {
    Optional<CrewMember> findByUserId(Long userId);
    List<CrewMember> findByCrewIdOrderByRoleAscJoinedAtAsc(Long crewId);
    long countByCrewId(Long crewId);
    void deleteByCrewIdAndUserId(Long crewId, Long userId);
}
