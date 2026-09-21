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
    // 구매 가능 여부는 포인트만으로 결정한다(레벨 제한 없음) — userLevel은 더 이상 잠금 계산에
    // 쓰이지 않지만, 호출부(ShopService)가 이미 레벨을 넘겨주고 있어 시그니처는 그대로 둔다.
    public static ShopItemResponse of(ShopItem item, boolean owned, boolean equipped, int userLevel) {
        return new ShopItemResponse(item.getId(), item.getName(), item.getPrice(), item.getCategory().name(),
                item.isConsumable(), item.getSlot(), item.getLevelReq(), item.getEffect(), item.getEffectDesc(),
                owned, equipped, false);
    }
}
