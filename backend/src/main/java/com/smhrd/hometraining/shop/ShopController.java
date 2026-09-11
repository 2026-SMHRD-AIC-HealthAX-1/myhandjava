package com.smhrd.hometraining.shop;

import com.smhrd.hometraining.common.ApiResponse;
import com.smhrd.hometraining.security.CustomUserPrincipal;
import com.smhrd.hometraining.shop.dto.ShopItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
