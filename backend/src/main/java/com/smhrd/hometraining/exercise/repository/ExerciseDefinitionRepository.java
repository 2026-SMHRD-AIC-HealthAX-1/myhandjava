package com.smhrd.hometraining.exercise.repository;

import com.smhrd.hometraining.exercise.entity.ExerciseDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciseDefinitionRepository extends JpaRepository<ExerciseDefinition, String> {
}
