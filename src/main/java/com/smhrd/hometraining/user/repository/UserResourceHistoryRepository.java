package com.smhrd.hometraining.user.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smhrd.hometraining.user.entity.UserResourceHistory;

// [DB 접근 지점] user_resource_histories 테이블(포인트/경험치/티켓 증감 이력 로그).
public interface UserResourceHistoryRepository
        extends JpaRepository<UserResourceHistory, Long> {

    List<UserResourceHistory> findByUserIdOrderByCreatedAtDesc(
            Long userId
    );

    void deleteByUserId(Long userId);
}
