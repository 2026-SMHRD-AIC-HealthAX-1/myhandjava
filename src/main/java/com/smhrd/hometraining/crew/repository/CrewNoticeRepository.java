package com.smhrd.hometraining.crew.repository;

import com.smhrd.hometraining.crew.entity.CrewNotice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// [DB 접근 지점] crew_notices 테이블.
public interface CrewNoticeRepository extends JpaRepository<CrewNotice, Long> {
    List<CrewNotice> findByCrewIdOrderByCreatedAtDesc(Long crewId);
    void deleteByCrewId(Long crewId);
    void deleteByAuthorId(Long authorId);
}
