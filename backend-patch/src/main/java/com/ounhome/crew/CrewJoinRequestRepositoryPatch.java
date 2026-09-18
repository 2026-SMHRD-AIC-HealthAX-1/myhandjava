package com.ounhome.crew;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrewJoinRequestRepositoryPatch extends JpaRepository<CrewJoinRequest, Long> {
    boolean existsByCrewIdAndRequesterIdAndStatus(Long crewId, Long requesterId, CrewJoinRequestStatus status);
    List<CrewJoinRequest> findAllByCrewIdAndStatusOrderByRequestedAtDesc(Long crewId, CrewJoinRequestStatus status);
}
