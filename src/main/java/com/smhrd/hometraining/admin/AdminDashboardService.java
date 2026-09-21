package com.smhrd.hometraining.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smhrd.hometraining.admin.dto.AdminDashboardResponse;
import com.smhrd.hometraining.crew.dto.CrewChatReportResponse;
import com.smhrd.hometraining.crew.entity.CrewChatReport;
import com.smhrd.hometraining.crew.repository.CrewChatReportRepository;
import com.smhrd.hometraining.exercise.repository.ExerciseRecordRepository;
import com.smhrd.hometraining.support.dto.SupportTicketDto;
import com.smhrd.hometraining.support.entity.SupportTicket;
import com.smhrd.hometraining.support.repository.SupportTicketRepository;
import com.smhrd.hometraining.user.entity.User;
import com.smhrd.hometraining.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final ExerciseRecordRepository exerciseRecordRepository;
    private final CrewChatReportRepository crewChatReportRepository;
    private final SupportTicketRepository supportTicketRepository;

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {

        LocalDate today = LocalDate.now(User.KOREA_ZONE_ID);
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();

        return new AdminDashboardResponse(
                userRepository.count(),
                exerciseRecordRepository.countByRecordedAtBetween(todayStart, todayEnd),
                crewChatReportRepository.countByStatus(CrewChatReport.Status.PENDING),
                supportTicketRepository.countByStatusNot(SupportTicket.Status.ANSWERED),
                crewChatReportRepository.findTop3ByOrderByReportedAtDesc()
                        .stream().map(CrewChatReportResponse::from).toList(),
                supportTicketRepository.findTop3ByOrderByCreatedAtDesc()
                        .stream().map(SupportTicketDto::from).toList()
        );
    }
}
