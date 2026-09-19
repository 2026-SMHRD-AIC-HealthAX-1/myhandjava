package com.smhrd.hometraining.shop.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "shop_items")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShopItem {

    public enum Category { 헤어, 상의, 하의, 신발, 배경, 기타 }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String name;

    @Column(nullable = false)
    private int price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Category category;

    @Column(nullable = false)
    private boolean consumable;

    /** 착용 슬롯: outfit·badge·crown·background·skin·nickname. 소모품은 null. */
    @Column(length = 20)
    private String slot;

    @Column(name = "level_req", nullable = false)
    private int levelReq;

    @Column(length = 60)
    private String effect;

    @Column(name = "effect_desc", length = 300)
    private String effectDesc;

    public static ShopItem of(String name, int price, Category category, boolean consumable,
                               String slot, int levelReq, String effect, String effectDesc) {
        ShopItem item = new ShopItem();
        item.name = name;
        item.price = price;
        item.category = category;
        item.consumable = consumable;
        item.slot = slot;
        item.levelReq = levelReq;
        item.effect = effect;
        item.effectDesc = effectDesc;
        return item;
    }
}
