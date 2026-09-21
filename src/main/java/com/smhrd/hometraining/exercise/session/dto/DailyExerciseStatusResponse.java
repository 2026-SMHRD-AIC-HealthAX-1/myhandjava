package com.smhrd.hometraining.exercise.session.dto;

import com.smhrd.hometraining.user.entity.User;

public record DailyExerciseStatusResponse(

        int dailyFreeLimit,
        int setsUsedToday,
        int remainingFreeSets,
        int retakeTickets

) {

    public static DailyExerciseStatusResponse from(User user) {

        int dailyFreeLimit =
                user.getDailySetLimit();

        int setsUsedToday =
                Math.min(
                        user.getSetsUsedToday(),
                        dailyFreeLimit
                );

        int remainingFreeSets =
                Math.max(
                        dailyFreeLimit - setsUsedToday,
                        0
                );

        return new DailyExerciseStatusResponse(
                dailyFreeLimit,
                setsUsedToday,
                remainingFreeSets,
                user.getRetakeTickets()
        );
    }
}