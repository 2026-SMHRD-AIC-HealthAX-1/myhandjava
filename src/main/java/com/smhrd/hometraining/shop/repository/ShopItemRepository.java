package com.smhrd.hometraining.shop.repository;

import com.smhrd.hometraining.shop.entity.ShopItem;
import org.springframework.data.jpa.repository.JpaRepository;

// [DB 접근 지점] shop_items 테이블. DataSeeder가 서버 기동 시 이 테이블을 시딩한다.
public interface ShopItemRepository extends JpaRepository<ShopItem, Long> {
    boolean existsByName(String name);
}
