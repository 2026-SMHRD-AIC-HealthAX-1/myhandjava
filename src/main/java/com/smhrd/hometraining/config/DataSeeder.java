package com.smhrd.hometraining.config;

import com.smhrd.hometraining.exercise.entity.ExerciseDefinition;
import com.smhrd.hometraining.exercise.repository.ExerciseDefinitionRepository;
import com.smhrd.hometraining.shop.entity.ShopItem;
import com.smhrd.hometraining.shop.repository.ShopItemRepository;
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

    private void seedShopItems() {
        if (shopItemRepository.count() > 0) return;
        shopItemRepository.saveAll(java.util.List.of(
                ShopItem.of("다시찍기 티켓", 80, ShopItem.Category.기타, true, null, 1,
                        "재촬영 1회 추가",
                        "세션당 무료 재촬영 2회를 모두 쓴 뒤, 추가로 다시 촬영할 때 1장씩 소모됩니다."),
                ShopItem.of("네온 트레이닝복", 300, ShopItem.Category.의상, false, "outfit", 5,
                        "판정 관대도 +3%",
                        "경계선 각도의 자세를 GOOD 이상으로 인정할 확률이 올라갑니다."),
                ShopItem.of("금빛 뱃지 프레임", 450, ShopItem.Category.의상, false, "badge", 6,
                        "미션 포인트 +10%",
                        "모든 미션 달성 보상 포인트에 10% 추가 지급됩니다."),
                ShopItem.of("챔피언 왕관", 900, ShopItem.Category.의상, false, "crown", 10,
                        "랭킹 점수 +5%",
                        "지역·종목 랭킹에 반영되는 점수가 5% 가산됩니다."),
                ShopItem.of("프로필 배경 - 새벽 러닝", 250, ShopItem.Category.배경, false, "background", 3,
                        "출석 보너스 +5P/일",
                        "연속 출석일마다 기본 출석 포인트에 5P가 추가됩니다."),
                ShopItem.of("캐릭터 - 로봇 코치", 600, ShopItem.Category.의상, false, "skin", 8,
                        "준비 카운트다운 -1초",
                        "촬영 시작 전 정렬 확인 후 나오는 카운트다운이 1초 짧아집니다."),
                ShopItem.of("닉네임 컬러 이펙트", 180, ShopItem.Category.기타, false, "nickname", 2,
                        "능력치 없음 · 외형 전용",
                        "랭킹에서 닉네임 색상만 강조되며 점수에는 영향이 없습니다."),
                ShopItem.of("닉네임 변경권", 150, ShopItem.Category.기타, true, null, 1,
                        "닉네임 변경 1회",
                        "설정에서 닉네임을 한 번 변경할 수 있습니다."),
                ShopItem.of("세트 추가권", 200, ShopItem.Category.기타, true, null, 1,
                        "일일 운동세트 +3",
                        "하루에 가능한 운동세트 한도가 3세트 늘어납니다.")
        ));
    }
}
