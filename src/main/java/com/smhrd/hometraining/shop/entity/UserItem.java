package com.smhrd.hometraining.shop.entity;

import com.smhrd.hometraining.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 착용형(비소모성) 아이템의 보유·장착 상태. 소모품(다시찍기 티켓 등)은 User 필드에 직접 반영되므로 여기에 남지 않는다. */
@Entity
@Table(name = "user_items", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_items_user_item", columnNames = {"user_id", "shop_item_id"})
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_item_id", nullable = false)
    private ShopItem shopItem;

    @Column(nullable = false)
    private boolean equipped = false;

    @Column(name = "purchased_at", nullable = false)
    private LocalDateTime purchasedAt;

    public static UserItem purchase(User user, ShopItem item) {
        UserItem ui = new UserItem();
        ui.user = user;
        ui.shopItem = item;
        ui.purchasedAt = LocalDateTime.now();
        return ui;
    }
}
