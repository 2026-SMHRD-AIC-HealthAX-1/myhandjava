package com.smhrd.hometraining.crew.policy;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

public final class CrewWeeklyMissionPolicy {

    /**
     * 크루 전체 주간 목표 운동 횟수입니다.
     */
    public static final int WEEKLY_TARGET_REPS = 300;

    /**
     * 크루원 한 명당 주간 최대 인정 횟수입니다.
     */
    public static final int MAX_MEMBER_REPS = 80;

    /**
     * 주간 미션 완료 시 지급할 크루 경험치입니다.
     */
    public static final int COMPLETION_REWARD_EXP = 500;
    /**
     * 다시찍기 티켓 운동을
     * 크루 주간 미션 진행도에 포함할지 결정합니다.
     *
     * true  : 티켓 운동 포함
     * false : 무료 운동만 포함
     */
    public static final boolean INCLUDE_RETAKE_TICKET_REPS = true;

    /**
     * 주간 시작 요일입니다.
     */
    public static final DayOfWeek WEEK_START_DAY =
            DayOfWeek.MONDAY;

    /**
     * 대한민국 시간대입니다.
     */
    public static final ZoneId KOREA_ZONE_ID =
            ZoneId.of("Asia/Seoul");

    private CrewWeeklyMissionPolicy() {
    }

    /**
     * 현재 대한민국 날짜를 반환합니다.
     */
    public static LocalDate getToday() {

        return LocalDate.now(
                KOREA_ZONE_ID
        );
    }

    /**
     * 해당 날짜가 포함된 주의 월요일을 반환합니다.
     */
    public static LocalDate getWeekStart(
            LocalDate date
    ) {

        return date.with(
                TemporalAdjusters.previousOrSame(
                        WEEK_START_DAY
                )
        );
    }

    /**
     * 현재 주의 월요일을 반환합니다.
     */
    public static LocalDate getCurrentWeekStart() {

        return getWeekStart(
                getToday()
        );
    }

    /**
     * 해당 주의 일요일을 반환합니다.
     */
    public static LocalDate getWeekEnd(
            LocalDate weekStart
    ) {

        return weekStart.plusDays(6);
    }

    /**
     * 크루원 한 명의 운동 횟수를
     * 최대 80회까지만 인정합니다.
     */
    public static int capMemberReps(
            int totalReps
    ) {

        return Math.min(
                Math.max(totalReps, 0),
                MAX_MEMBER_REPS
        );
    }
}