package com.smhrd.hometraining.crew.repository;

import com.smhrd.hometraining.crew.entity.CrewJoinRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// [DB 접근 지점] crew_join_requests 테이블.
public interface CrewJoinRequestRepository extends JpaRepository<CrewJoinRequest, Long> {
    List<CrewJoinRequest> findByCrewIdOrderByRequestedAtAsc(Long crewId);
    boolean existsByCrewIdAndRequesterId(Long crewId, Long requesterId);
    List<CrewJoinRequest> findByRequesterId(Long requesterId);
    void deleteByCrewId(Long crewId);
    void deleteByRequesterId(Long requesterId);
}
