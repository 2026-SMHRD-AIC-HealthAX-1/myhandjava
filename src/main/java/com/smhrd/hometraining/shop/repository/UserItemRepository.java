package com.smhrd.hometraining.shop.repository;

import com.smhrd.hometraining.shop.entity.UserItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// [DB 접근 지점] user_items 테이블(회원이 보유/착용 중인 상점 아이템).
public interface UserItemRepository extends JpaRepository<UserItem, Long> {
    List<UserItem> findByUserId(Long userId);
    Optional<UserItem> findByUserIdAndShopItemId(Long userId, Long shopItemId);
    Optional<UserItem> findByUserIdAndShopItem_Slot(Long userId, String slot);
    void deleteByUserId(Long userId);
}
