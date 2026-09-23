package com.smhrd.hometraining.exercise.session.dto;

import com.smhrd.hometraining.user.entity.User;

/**
 * 하루 운동 횟수에는 더 이상 상한이 없습니다 — 이 응답은 오늘 사용한
 * 운동 횟수와 보유 다시찍기 티켓 개수만 통계용으로 보여줍니다.
 */
public record DailyExerciseStatusResponse(

        int setsUsedToday,
        int retakeTickets

) {

    public static DailyExerciseStatusResponse from(User user) {

        return new DailyExerciseStatusResponse(
                user.getSetsUsedToday(),
                user.getRetakeTickets()
        );
    }
}