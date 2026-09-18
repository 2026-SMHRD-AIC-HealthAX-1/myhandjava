package com.smhrd.hometraining.exercise.session.entity;

public enum ExerciseSessionStatus {

    /**
     * 세션 ID만 생성된 상태입니다.
     *
     * 아직 카메라와 자세 인식이 정상적으로
     * 시작되지 않았기 때문에 운동 횟수를 차감하지 않습니다.
     */
    CREATED("생성됨"),

    /**
     * 카메라와 자세 인식이 정상적으로 시작된 상태입니다.
     *
     * 이 상태로 변경되는 시점에
     * 운동 사용 횟수 또는 티켓을 차감합니다.
     */
    STARTED("운동 중"),

    /**
     * 운동 결과가 정상적으로 저장된 상태입니다.
     */
    COMPLETED("완료"),

    /**
     * 정상 시작 후 사용자가 중간에 나간 상태입니다.
     *
     * 이미 운동이 시작됐으므로
     * 사용 횟수는 복구하지 않습니다.
     */
    ABORTED("중도 종료"),

    /**
     * 카메라 권한 거부, 연결 실패 또는
     * 자세 인식 시작 실패 상태입니다.
     *
     * 운동 시작 전에 실패했으므로
     * 사용 횟수나 티켓을 차감하지 않습니다.
     */
    FAILED("시작 실패");

    private final String koreanName;

    ExerciseSessionStatus(String koreanName) {
        this.koreanName = koreanName;
    }

    public String getKoreanName() {
        return koreanName;
    }

    /**
     * 현재 상태에서 운동 시작 처리가 가능한지 확인합니다.
     */
    public boolean canStart() {
        return this == CREATED;
    }

    /**
     * 현재 상태에서 운동 결과 저장이 가능한지 확인합니다.
     */
    public boolean canComplete() {
        return this == STARTED;
    }

    /**
     * 실제 운동이 한 번이라도 시작됐는지 확인합니다.
     */
    public boolean wasStarted() {
        return this == STARTED
                || this == COMPLETED
                || this == ABORTED;
    }

    /**
     * 더 이상 진행되지 않는 종료 상태인지 확인합니다.
     */
    public boolean isFinished() {
        return this == COMPLETED
                || this == ABORTED
                || this == FAILED;
    }
}