package com.smhrd.hometraining.support;

import com.smhrd.hometraining.common.exception.BusinessException;
import com.smhrd.hometraining.support.dto.SupportTicketDto;
import com.smhrd.hometraining.support.entity.SupportTicket;
import com.smhrd.hometraining.support.repository.SupportTicketRepository;
import com.smhrd.hometraining.user.UserService;
import com.smhrd.hometraining.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupportService {

    private final SupportTicketRepository ticketRepository;
    private final UserService userService;

    @Transactional
    public SupportTicketDto create(Long userId, SupportTicketDto.Create req) {
        User author = userService.getUserOrThrow(userId);
        SupportTicket.Type type = parseType(req.type());
        SupportTicket ticket = ticketRepository.save(SupportTicket.create(author, type, req.title(), req.body()));
        return SupportTicketDto.from(ticket);
    }

    @Transactional(readOnly = true)
    public List<SupportTicketDto> myTickets(Long userId) {
        return ticketRepository.findByAuthorIdOrderByCreatedAtDesc(userId).stream().map(SupportTicketDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<SupportTicketDto> allTickets() {
        return ticketRepository.findAllByOrderByCreatedAtDesc().stream().map(SupportTicketDto::from).toList();
    }

    @Transactional
    public SupportTicketDto reply(Long ticketId, SupportTicketDto.Reply req) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessException("문의를 찾을 수 없습니다."));
        ticket.setReply(req.reply());
        ticket.setStatus(SupportTicket.Status.ANSWERED);
        ticket.setAnsweredAt(LocalDateTime.now());
        return SupportTicketDto.from(ticket);
    }

    // 접수(RECEIVED) 상태인 문의를 "처리중"으로 바꾼다. 이미 처리중이거나 답변완료인 티켓은
    // 그대로 둔다 — 답변완료를 다시 처리중으로 되돌리는 실수를 막기 위해서다.
    @Transactional
    public SupportTicketDto markInProgress(Long ticketId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessException("문의를 찾을 수 없습니다."));
        if (ticket.getStatus() == SupportTicket.Status.RECEIVED) {
            ticket.setStatus(SupportTicket.Status.IN_PROGRESS);
        }
        return SupportTicketDto.from(ticket);
    }

    private SupportTicket.Type parseType(String raw) {
        return switch (raw) {
            case "Error", "ERROR" -> SupportTicket.Type.ERROR;
            case "기능제안", "FEATURE_REQUEST" -> SupportTicket.Type.FEATURE_REQUEST;
            default -> SupportTicket.Type.ETC;
        };
    }
}
