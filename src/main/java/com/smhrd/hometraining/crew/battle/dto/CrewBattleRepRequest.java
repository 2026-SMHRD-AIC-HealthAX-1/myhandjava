package com.smhrd.hometraining.crew.battle.dto;

import com.smhrd.hometraining.exercise.entity.ExerciseRecord;

public record CrewBattleRepRequest(
        Long battleId,
        ExerciseRecord.Grade grade
) {
}