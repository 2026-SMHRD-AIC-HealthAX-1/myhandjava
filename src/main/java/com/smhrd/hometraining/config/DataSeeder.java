package com.smhrd.hometraining.config;

import com.smhrd.hometraining.exercise.entity.ExerciseDefinition;
import com.smhrd.hometraining.exercise.repository.ExerciseDefinitionRepository;
import com.smhrd.hometraining.shop.entity.ShopItem;
import com.smhrd.hometraining.shop.repository.ShopItemRepository;
import com.smhrd.hometraining.shop.repository.UserItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** script.js의 EXS·shopItems 목업 데이터를 최초 기동 시 DB에 그대로 반영한다(이미 있으면 건너뜀). */
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
        seedShopItems();
    }

    private void seedExercises() {
        if (exerciseDefinitionRepository.count() > 0) return;
        exerciseDefinitionRepository.save(ExerciseDefinition.of("squat", "스쿼트", "하체 · 둔근", "초급"));
    }

    /**
     * 프론트(avatar-items.js/state.js)에 있는 실제 구매 가능 카탈로그로 맞춘다. 기본 지급 아이템
     * (오렌지 헤드밴드·민트 티셔츠·딥그린 숏팬츠·오렌지 손목밴드·닉네임 컬러 이펙트)은 모든
     * 유저가 항상 보유한 것으로 프론트가 처리하므로 여기엔 넣지 않는다 — 구매 대상만 시딩.
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
                ShopItem.of("운동 추가권", 80, ShopItem.Category.기타, true, null, 1,
                        "운동 1회 추가", "운동 기회를 1회 추가할 수 있는 이용권입니다."),
                ShopItem.of("닉네임 변경권", 150, ShopItem.Category.기타, true, null, 1,
                        "닉네임 변경 1회", "닉네임을 한 번 변경할 수 있습니다."),

                ShopItem.of("네이비 스포츠 캡", 220, ShopItem.Category.헤어, false, "head", 2,
                        "능력치 없음 · 외형 전용", "차분한 네이비 컬러의 스포츠 캡입니다."),

                ShopItem.of("오렌지 트랙 재킷", 320, ShopItem.Category.상의, false, "top", 3,
                        "능력치 없음 · 외형 전용", "활기찬 오렌지 컬러의 집업 트랙 재킷입니다."),
                ShopItem.of("라벤더 후디", 340, ShopItem.Category.상의, false, "top", 3,
                        "능력치 없음 · 외형 전용", "부드러운 라벤더색 후드 운동복입니다."),

                ShopItem.of("차콜 트랙 팬츠", 280, ShopItem.Category.하의, false, "bottom", 2,
                        "능력치 없음 · 외형 전용", "오렌지 라인이 들어간 차콜 트레이닝 팬츠입니다."),
                ShopItem.of("라벤더 조거 팬츠", 300, ShopItem.Category.하의, false, "bottom", 3,
                        "능력치 없음 · 외형 전용", "편안한 핏의 라벤더 조거 팬츠입니다."),

                ShopItem.of("민트 운동화", 260, ShopItem.Category.신발, false, "shoes", 2,
                        "능력치 없음 · 외형 전용", "민트 포인트가 들어간 산뜻한 운동화입니다."),
                ShopItem.of("라벤더 하이탑", 310, ShopItem.Category.신발, false, "shoes", 3,
                        "능력치 없음 · 외형 전용", "발목까지 올라오는 라벤더 하이탑 운동화입니다."),

                ShopItem.of("네이비 스마트워치", 240, ShopItem.Category.기타, false, "accessory", 2,
                        "능력치 없음 · 외형 전용", "운동 기록을 확인하는 네이비 스마트워치입니다."),
                ShopItem.of("골드 메달", 380, ShopItem.Category.기타, false, "accessory", 4,
                        "능력치 없음 · 외형 전용", "꾸준한 운동을 기념하는 골드 메달입니다."),

                ShopItem.of("배경 - 맑은 강변 산책로", 250, ShopItem.Category.배경, false, "background", 1,
                        "능력치 없음 · 외형 전용", "푸른 하늘과 강변 러닝 코스가 펼쳐진 밝은 낮 배경입니다."),
                ShopItem.of("배경 - 노을빛 강변", 300, ShopItem.Category.배경, false, "background", 2,
                        "능력치 없음 · 외형 전용", "주황빛 노을이 물든 강변 러닝 코스 배경입니다."),
                ShopItem.of("배경 - 가을 호수 공원", 350, ShopItem.Category.배경, false, "background", 3,
                        "능력치 없음 · 외형 전용", "단풍과 호수가 어우러진 따뜻한 가을 공원 배경입니다."),
                ShopItem.of("배경 - 비 오는 가로수길", 320, ShopItem.Category.배경, false, "background", 3,
                        "능력치 없음 · 외형 전용", "가로등 불빛이 비치는 차분한 빗속 공원 배경입니다.")
        ));
    }
}
