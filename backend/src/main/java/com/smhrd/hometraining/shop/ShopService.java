package com.smhrd.hometraining.shop;

import com.smhrd.hometraining.common.exception.BusinessException;
import com.smhrd.hometraining.shop.dto.ShopItemResponse;
import com.smhrd.hometraining.shop.entity.ShopItem;
import com.smhrd.hometraining.shop.entity.UserItem;
import com.smhrd.hometraining.shop.repository.ShopItemRepository;
import com.smhrd.hometraining.shop.repository.UserItemRepository;
import com.smhrd.hometraining.user.UserService;
import com.smhrd.hometraining.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopItemRepository shopItemRepository;
    private final UserItemRepository userItemRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<ShopItemResponse> listItems(Long userIdOrNull) {
        List<ShopItem> items = shopItemRepository.findAll();
        if (userIdOrNull == null) {
            return items.stream().map(i -> ShopItemResponse.of(i, false, false, 0)).toList();
        }
        User user = userService.getUserOrThrow(userIdOrNull);
        Set<Long> ownedItemIds = userItemRepository.findByUserId(userIdOrNull).stream()
                .map(ui -> ui.getShopItem().getId()).collect(java.util.stream.Collectors.toSet());
        Set<Long> equippedItemIds = userItemRepository.findByUserId(userIdOrNull).stream()
                .filter(UserItem::isEquipped).map(ui -> ui.getShopItem().getId()).collect(java.util.stream.Collectors.toSet());
        return items.stream()
                .map(i -> ShopItemResponse.of(i, ownedItemIds.contains(i.getId()), equippedItemIds.contains(i.getId()), user.getLevel()))
                .toList();
    }

    @Transactional
    public ShopItemResponse purchase(Long userId, Long itemId) {
        User user = userService.getUserOrThrow(userId);
        ShopItem item = shopItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException("아이템을 찾을 수 없습니다."));

        if (user.getLevel() < item.getLevelReq()) {
            throw new BusinessException("Lv." + item.getLevelReq() + "부터 구매할 수 있습니다.");
        }
        if (!item.isConsumable() && userItemRepository.findByUserIdAndShopItemId(userId, itemId).isPresent()) {
            throw new BusinessException("이미 보유한 아이템입니다.");
        }
        if (user.getPoints() < item.getPrice()) {
            throw new BusinessException("포인트가 부족합니다.");
        }

        user.setPoints(user.getPoints() - item.getPrice());

        if (item.isConsumable()) {
            applyConsumableEffect(user, item);
            return ShopItemResponse.of(item, true, false, user.getLevel());
        }

        UserItem userItem = userItemRepository.save(UserItem.purchase(user, item));
        return ShopItemResponse.of(item, true, userItem.isEquipped(), user.getLevel());
    }

    @Transactional
    public void setEquipped(Long userId, Long itemId, boolean equip) {
        UserItem userItem = userItemRepository.findByUserIdAndShopItemId(userId, itemId)
                .orElseThrow(() -> new BusinessException("먼저 아이템을 구매해주세요."));
        if (equip && userItem.getShopItem().getSlot() != null) {
            // 같은 슬롯에 이미 장착된 아이템이 있으면 해제한다 (슬롯당 1개만 착용 가능).
            userItemRepository.findByUserIdAndShopItem_Slot(userId, userItem.getShopItem().getSlot())
                    .ifPresent(existing -> existing.setEquipped(false));
        }
        userItem.setEquipped(equip);
    }

    private void applyConsumableEffect(User user, ShopItem item) {
        switch (item.getName()) {
            case "닉네임 변경권" -> user.setNicknameTickets(user.getNicknameTickets() + 1);
            case "세트 추가권" -> user.setExtraSets(user.getExtraSets() + 3);
            case "다시찍기 티켓" -> user.setRetakeTickets(user.getRetakeTickets() + 1);
            default -> { /* 정의되지 않은 소모품은 포인트 차감만 적용된다. */ }
        }
    }
}
