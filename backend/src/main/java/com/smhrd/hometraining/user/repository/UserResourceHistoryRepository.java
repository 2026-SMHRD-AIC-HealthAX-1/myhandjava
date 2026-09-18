package com.smhrd.hometraining.user.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smhrd.hometraining.user.entity.UserResourceHistory;

public interface UserResourceHistoryRepository
        extends JpaRepository<UserResourceHistory, Long> {

    List<UserResourceHistory> findByUserIdOrderByCreatedAtDesc(
            Long userId
    );
}
