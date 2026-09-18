package com.smhrd.hometraining.shop.dto;

import com.smhrd.hometraining.shop.entity.ShopItem;

public record ShopItemResponse(
        Long id,
        String name,
        int price,
        String category,
        boolean consumable,
        String slot,
        int levelReq,
        String effect,
        String effectDesc,
        boolean owned,
        boolean equipped,
        boolean locked
) {
    public static ShopItemResponse of(ShopItem item, boolean owned, boolean equipped, int userLevel) {
        return new ShopItemResponse(item.getId(), item.getName(), item.getPrice(), item.getCategory().name(),
                item.isConsumable(), item.getSlot(), item.getLevelReq(), item.getEffect(), item.getEffectDesc(),
                owned, equipped, userLevel < item.getLevelReq());
    }
}
