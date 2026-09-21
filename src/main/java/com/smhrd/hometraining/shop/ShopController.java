package com.smhrd.hometraining.shop;

import com.smhrd.hometraining.common.ApiResponse;
import com.smhrd.hometraining.security.CustomUserPrincipal;
import com.smhrd.hometraining.shop.dto.ShopItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * [담당] 포인트 상점 아이템 조회/구매/착용·해제.
 * [프론트 연동] ounhome-f/js/shop.js — GET /api/shop/items, POST /items/{id}/purchase, /equip, /unequip.
 * [DB] ShopService → shop_items(카탈로그), user_items(보유/착용), users(포인트 차감) 테이블.
 * [주의] 레벨 제한(levelReq)은 완전히 제거되어 포인트만 있으면 구매 가능하다 — 구매 검증 로직에
 *        레벨 체크를 다시 넣지 말 것. DataSeeder가 서버 기동 시 shop_items를 시딩하는데,
 *        시딩 실패해도 서버는 안 죽도록 방어돼 있다(config/DataSeeder.java 참고).
 */
@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    @GetMapping("/items")
    public ApiResponse<List<ShopItemResponse>> items(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return ApiResponse.ok(shopService.listItems(principal == null ? null : principal.getUserId()));
    }

    @PostMapping("/items/{itemId}/purchase")
    public ApiResponse<ShopItemResponse> purchase(@AuthenticationPrincipal CustomUserPrincipal principal,
                                                    @PathVariable Long itemId) {
        return ApiResponse.ok(shopService.purchase(principal.getUserId(), itemId));
    }

    @PostMapping("/items/{itemId}/equip")
    public ApiResponse<Void> equip(@AuthenticationPrincipal CustomUserPrincipal principal, @PathVariable Long itemId) {
        shopService.setEquipped(principal.getUserId(), itemId, true);
        return ApiResponse.ok();
    }

    @PostMapping("/items/{itemId}/unequip")
    public ApiResponse<Void> unequip(@AuthenticationPrincipal CustomUserPrincipal principal, @PathVariable Long itemId) {
        shopService.setEquipped(principal.getUserId(), itemId, false);
        return ApiResponse.ok();
    }
}
