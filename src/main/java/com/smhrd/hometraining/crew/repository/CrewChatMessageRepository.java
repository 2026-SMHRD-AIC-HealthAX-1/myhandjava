package com.smhrd.hometraining.crew.repository;

import com.smhrd.hometraining.crew.entity.CrewChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// [DB 접근 지점] crew_chat_messages 테이블.
public interface CrewChatMessageRepository extends JpaRepository<CrewChatMessage, Long> {
    List<CrewChatMessage> findTop50ByCrewIdOrderBySentAtDesc(Long crewId);
    void deleteByCrewId(Long crewId);
    void deleteBySenderId(Long senderId);
}
