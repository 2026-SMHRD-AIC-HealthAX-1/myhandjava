package com.smhrd.hometraining.shop.repository;

import com.smhrd.hometraining.shop.entity.ShopItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopItemRepository extends JpaRepository<ShopItem, Long> {
    boolean existsByName(String name);
}
