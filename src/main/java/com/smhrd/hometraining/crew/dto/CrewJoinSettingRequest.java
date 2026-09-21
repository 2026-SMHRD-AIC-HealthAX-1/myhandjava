package com.smhrd.hometraining.crew.dto;

import jakarta.validation.constraints.NotNull;

public record CrewJoinSettingRequest(

        @NotNull(message = "가입 신청 상태가 필요합니다.")
        Boolean joinEnabled

) {
}