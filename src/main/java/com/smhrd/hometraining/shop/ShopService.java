package com.smhrd.hometraining.shop;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smhrd.hometraining.common.exception.BusinessException;
import com.smhrd.hometraining.shop.dto.ShopItemResponse;
import com.smhrd.hometraining.shop.entity.ShopItem;
import com.smhrd.hometraining.shop.entity.UserItem;
import com.smhrd.hometraining.shop.repository.ShopItemRepository;
import com.smhrd.hometraining.shop.repository.UserItemRepository;
import com.smhrd.hometraining.user.UserResourceHistoryService;
import com.smhrd.hometraining.user.UserService;
import com.smhrd.hometraining.user.entity.User;
import com.smhrd.hometraining.user.entity.UserResourceHistory.Reason;
import com.smhrd.hometraining.user.entity.UserResourceHistory.ResourceType;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopItemRepository shopItemRepository;
    private final UserItemRepository userItemRepository;
    private final UserService userService;
    private final UserResourceHistoryService resourceHistoryService;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<ShopItemResponse> listItems(Long userIdOrNull) {

        List<ShopItem> items = shopItemRepository.findAll();

        if (userIdOrNull == null) {
            return items.stream()
                    .map(item -> ShopItemResponse.of(item, false, false, 0))
                    .toList();
        }

        User user = userService.getUserOrThrow(userIdOrNull);

        List<UserItem> userItems =
                userItemRepository.findByUserId(userIdOrNull);

        Set<Long> ownedItemIds = userItems.stream()
                .map(userItem -> userItem.getShopItem().getId())
                .collect(Collectors.toSet());

        Set<Long> equippedItemIds = userItems.stream()
                .filter(UserItem::isEquipped)
                .map(userItem -> userItem.getShopItem().getId())
                .collect(Collectors.toSet());

        return items.stream()
                .map(item -> ShopItemResponse.of(
                        item,
                        ownedItemIds.contains(item.getId()),
                        equippedItemIds.contains(item.getId()),
                        user.getLevel()
                ))
                .toList();
    }

    @Transactional
    public ShopItemResponse purchase(Long userId, Long itemId) {

        User user = userService.getUserOrThrow(userId);
        entityManager.lock(user, LockModeType.PESSIMISTIC_WRITE);

        ShopItem item = shopItemRepository.findById(itemId)
                .orElseThrow(() ->
                        new BusinessException("아이템을 찾을 수 없습니다."));

        if (user.getLevel() < item.getLevelReq()) {
            throw new BusinessException(
                    "Lv." + item.getLevelReq() + "부터 구매할 수 있습니다."
            );
        }

        if (!item.isConsumable()
                && userItemRepository
                        .findByUserIdAndShopItemId(userId, itemId)
                        .isPresent()) {
            throw new BusinessException("이미 보유한 아이템입니다.");
        }

        int serverPrice = item.getPrice();

        if (serverPrice < 0) {
            throw new BusinessException("아이템 가격 설정이 올바르지 않습니다.");
        }

        if (user.getPoints() < serverPrice) {
            throw new BusinessException("포인트가 부족합니다.");
        }

        long pointsBefore = user.getPoints();
        user.setPoints(pointsBefore - serverPrice);

        resourceHistoryService.record(
                user,
                ResourceType.POINT,
                -serverPrice,
                pointsBefore,
                user.getPoints(),
                Reason.SHOP_PURCHASE,
                item.getId()
        );

        if (item.isConsumable()) {
            applyConsumableEffect(user, item);
            return ShopItemResponse.of(item, true, false, user.getLevel());
        }

        UserItem userItem =
                userItemRepository.save(UserItem.purchase(user, item));

        return ShopItemResponse.of(
                item,
                true,
                userItem.isEquipped(),
                user.getLevel()
        );
    }

    @Transactional
    public void setEquipped(Long userId, Long itemId, boolean equip) {

        UserItem userItem = userItemRepository
                .findByUserIdAndShopItemId(userId, itemId)
                .orElseThrow(() ->
                        new BusinessException("먼저 아이템을 구매해 주세요."));

        if (equip && userItem.getShopItem().getSlot() != null) {
            userItemRepository
                    .findByUserIdAndShopItem_Slot(
                            userId,
                            userItem.getShopItem().getSlot()
                    )
                    .ifPresent(existing -> existing.setEquipped(false));
        }

        userItem.setEquipped(equip);
    }

    private void applyConsumableEffect(User user, ShopItem item) {

        switch (item.getName()) {
            case "닉네임 변경권" -> {
                int before = user.getNicknameTickets();
                user.setNicknameTickets(before + 1);
                recordTicketChange(
                        user,
                        ResourceType.NICKNAME_TICKET,
                        1,
                        before,
                        user.getNicknameTickets(),
                        item.getId()
                );
            }
            case "운동 추가권" -> {
                int before = user.getRetakeTickets();
                user.setRetakeTickets(before + 1);
                recordTicketChange(
                        user,
                        ResourceType.RETAKE_TICKET,
                        1,
                        before,
                        user.getRetakeTickets(),
                        item.getId()
                );
            }
            default -> {
                // Unknown consumables only spend their server-side price.
            }
        }
    }

    private void recordTicketChange(
            User user,
            ResourceType type,
            long amount,
            long before,
            long after,
            Long itemId
    ) {

        resourceHistoryService.record(
                user,
                type,
                amount,
                before,
                after,
                Reason.SHOP_PURCHASE,
                itemId
        );
    }
}
