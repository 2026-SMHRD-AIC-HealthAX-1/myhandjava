package com.smhrd.hometraining.config;

import com.smhrd.hometraining.exercise.entity.ExerciseDefinition;
import com.smhrd.hometraining.exercise.repository.ExerciseDefinitionRepository;
import com.smhrd.hometraining.shop.entity.ShopItem;
import com.smhrd.hometraining.shop.repository.ShopItemRepository;
import com.smhrd.hometraining.shop.repository.UserItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

/**
 * script.js의 EXS·shopItems 목업 데이터를 최초 기동 시 DB에 그대로 반영한다(이미 있으면 건너뜀).
 *
 * [담당] 서버 기동 시 초기 데이터 시딩 — exercise_definitions, shop_items 테이블.
 * [주의] ⚠️ 이 프로젝트는 application.yml의 ddl-auto: none이라 테이블/컬럼은 자동 생성되지
 *        않는다 — 새 컬럼/테이블이 필요하면 공유 DB에 직접 ALTER TABLE/CREATE TABLE을 실행해야
 *        한다(엔티티만 고치면 실제 DB와 안 맞아 런타임 SQL 에러가 난다).
 *        seedShopItems()는 실패해도 서버 전체가 죽지 않도록 try/catch + rollback-only 처리돼
 *        있다(공유 DB에 옛날 스키마가 남아있는 환경 대비) — 이 방어 로직을 지우지 말 것.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final ExerciseDefinitionRepository exerciseDefinitionRepository;
    private final ShopItemRepository shopItemRepository;
    private final UserItemRepository userItemRepository;

    @Override
    @Transactional
    public void run(String... args) {
        seedExercises();
        try {
            seedShopItems();
        } catch (RuntimeException e) {
            // 공용 DB처럼 shop_items 스키마가 아직 옛날 카테고리 값(의상 등)에 묶여있는 환경에서는
            // 시딩이 실패할 수 있다 — 그런다고 서버 전체가 못 뜨면 안 되므로 경고만 남기고 넘어간다.
            // (여기서 잡아도 이미 실패한 트랜잭션은 커밋하면 안 되므로 rollback-only로 표시한다.)
            log.warn("상점 아이템 시딩 실패 — 기존 데이터 유지, 서버는 계속 기동합니다: {}", e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
    }

    private void seedExercises() {
        if (exerciseDefinitionRepository.count() > 0) return;
        exerciseDefinitionRepository.save(ExerciseDefinition.of("squat", "스쿼트", "하체 · 둔근", "초급"));
    }

    /**
     * 프론트(avatar-items.js/state.js)에 있는 실제 구매 가능 카탈로그로 맞춘다. 오렌지 헤드밴드·
     * 민트 티셔츠·딥그린 숏팬츠·오렌지 손목밴드는 지금 판매 대상이 아니라(shop.js의
     * SHOP_ENABLED_ITEM_NAMES 참고) 여기엔 넣지 않는다 — 구매 가능한 아이템만 시딩.
     * "네이비 스포츠 캡"을 마이그레이션 완료 표시로 써서, 예전(script.js 시절) 카탈로그가 이미
     * 심어져 있던 DB에서도 한 번만 정리하고 다시 심는다.
     */
    private void seedShopItems() {
        if (shopItemRepository.existsByName("네이비 스포츠 캡")) return;
        // 예전 카탈로그에는 지금 enum에 없는 Category 값(의상)이 남아있어, 엔티티를 읽어들이는
        // deleteAll() 대신 조회 없이 바로 지우는 배치 삭제를 써야 enum 역직렬화 오류를 피한다.
        userItemRepository.deleteAllInBatch();
        shopItemRepository.deleteAllInBatch();
        shopItemRepository.saveAll(java.util.List.of(
                ShopItem.of("닉네임 변경권", 150, ShopItem.Category.기타, true, null,
                        "닉네임 변경 1회", "닉네임을 한 번 변경할 수 있습니다."),
                ShopItem.of("순위 도전 티켓", 100, ShopItem.Category.기타, true, null,
                        "순위 도전 1회", "순위 도전에 1회 참여할 수 있는 티켓입니다."),
                // 보유/착용 개념 없이 구매 즉시 색을 골라 적용하는 소모 아이템(shop.js buyItem 참고).
                ShopItem.of("닉네임 컬러 이펙트", 180, ShopItem.Category.기타, true, "nickname",
                        "닉네임 컬러 변경 1회", "구매하면 바로 원하는 닉네임 색상을 골라 적용할 수 있습니다. 보유 아이템으로 쌓이지 않고, 다시 구매하면 색상을 또 바꿀 수 있어요."),

                ShopItem.of("네이비 스포츠 캡", 220, ShopItem.Category.헤어, false, "head",
                        "능력치 없음 · 외형 전용", "차분한 네이비 컬러의 스포츠 캡입니다."),

                ShopItem.of("오렌지 트랙 재킷", 320, ShopItem.Category.상의, false, "top",
                        "능력치 없음 · 외형 전용", "활기찬 오렌지 컬러의 집업 트랙 재킷입니다."),
                ShopItem.of("라벤더 후디", 340, ShopItem.Category.상의, false, "top",
                        "능력치 없음 · 외형 전용", "부드러운 라벤더색 후드 운동복입니다."),

                ShopItem.of("차콜 트랙 팬츠", 280, ShopItem.Category.하의, false, "bottom",
                        "능력치 없음 · 외형 전용", "오렌지 라인이 들어간 차콜 트레이닝 팬츠입니다."),
                ShopItem.of("라벤더 조거 팬츠", 300, ShopItem.Category.하의, false, "bottom",
                        "능력치 없음 · 외형 전용", "편안한 핏의 라벤더 조거 팬츠입니다."),

                ShopItem.of("민트 운동화", 260, ShopItem.Category.신발, false, "shoes",
                        "능력치 없음 · 외형 전용", "민트 포인트가 들어간 산뜻한 운동화입니다."),
                ShopItem.of("라벤더 하이탑", 310, ShopItem.Category.신발, false, "shoes",
                        "능력치 없음 · 외형 전용", "발목까지 올라오는 라벤더 하이탑 운동화입니다."),

                ShopItem.of("네이비 스마트워치", 240, ShopItem.Category.기타, false, "accessory",
                        "능력치 없음 · 외형 전용", "운동 기록을 확인하는 네이비 스마트워치입니다."),
                ShopItem.of("골드 메달", 380, ShopItem.Category.기타, false, "accessory",
                        "능력치 없음 · 외형 전용", "꾸준한 운동을 기념하는 골드 메달입니다."),

                ShopItem.of("배경 - 맑은 강변 산책로", 250, ShopItem.Category.배경, false, "background",
                        "능력치 없음 · 외형 전용", "푸른 하늘과 강변 러닝 코스가 펼쳐진 밝은 낮 배경입니다."),
                ShopItem.of("배경 - 노을빛 강변", 300, ShopItem.Category.배경, false, "background",
                        "능력치 없음 · 외형 전용", "주황빛 노을이 물든 강변 러닝 코스 배경입니다."),
                ShopItem.of("배경 - 가을 호수 공원", 350, ShopItem.Category.배경, false, "background",
                        "능력치 없음 · 외형 전용", "단풍과 호수가 어우러진 따뜻한 가을 공원 배경입니다."),
                ShopItem.of("배경 - 비 오는 가로수길", 320, ShopItem.Category.배경, false, "background",
                        "능력치 없음 · 외형 전용", "가로등 불빛이 비치는 차분한 빗속 공원 배경입니다.")
        ));
    }
}
