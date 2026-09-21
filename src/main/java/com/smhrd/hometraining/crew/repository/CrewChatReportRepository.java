package com.smhrd.hometraining.crew.repository;

import com.smhrd.hometraining.crew.entity.CrewChatReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrewChatReportRepository extends JpaRepository<CrewChatReport, Long> {
    List<CrewChatReport> findAllByOrderByReportedAtDesc();
    List<CrewChatReport> findTop3ByOrderByReportedAtDesc();
    long countByStatus(CrewChatReport.Status status);
}
