package com.smhrd.hometraining.crew.repository;

import com.smhrd.hometraining.crew.entity.CrewChatReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// [DB 접근 지점] crew_chat_reports 테이블(크루채팅 신고).
public interface CrewChatReportRepository extends JpaRepository<CrewChatReport, Long> {
    List<CrewChatReport> findAllByOrderByReportedAtDesc();
    List<CrewChatReport> findTop3ByOrderByReportedAtDesc();
    long countByStatus(CrewChatReport.Status status);
    void deleteByCrewId(Long crewId);
}
