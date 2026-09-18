package com.smhrd.hometraining.crew.repository;

import com.smhrd.hometraining.crew.entity.CrewNotice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrewNoticeRepository extends JpaRepository<CrewNotice, Long> {
    List<CrewNotice> findByCrewIdOrderByCreatedAtDesc(Long crewId);
}
