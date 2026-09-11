package com.smhrd.hometraining.shop.repository;

import com.smhrd.hometraining.shop.entity.UserItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserItemRepository extends JpaRepository<UserItem, Long> {
    List<UserItem> findByUserId(Long userId);
    Optional<UserItem> findByUserIdAndShopItemId(Long userId, Long shopItemId);
    Optional<UserItem> findByUserIdAndShopItem_Slot(Long userId, String slot);
}
