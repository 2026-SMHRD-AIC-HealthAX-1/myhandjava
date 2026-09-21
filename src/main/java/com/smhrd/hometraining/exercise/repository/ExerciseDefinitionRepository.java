package com.smhrd.hometraining.exercise.repository;

import com.smhrd.hometraining.exercise.entity.ExerciseDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

// [DB 접근 지점] exercise_definitions 테이블(운동 종목 카탈로그, 지금은 '스쿼트'만 시딩됨).
public interface ExerciseDefinitionRepository extends JpaRepository<ExerciseDefinition, String> {
}
