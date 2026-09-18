package com.smhrd.hometraining.support.repository;

import com.smhrd.hometraining.support.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByAuthorIdOrderByCreatedAtDesc(Long authorId);
    List<SupportTicket> findAllByOrderByCreatedAtDesc();
}
