package com.smhrd.hometraining.support.repository;

import com.smhrd.hometraining.support.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// [DB 접근 지점] support_tickets 테이블.
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByAuthorIdOrderByCreatedAtDesc(Long authorId);
    List<SupportTicket> findAllByOrderByCreatedAtDesc();
    List<SupportTicket> findTop3ByOrderByCreatedAtDesc();
    long countByStatusNot(SupportTicket.Status status);
    void deleteByAuthorId(Long authorId);
}
