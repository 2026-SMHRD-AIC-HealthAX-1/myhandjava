package com.smhrd.hometraining.crew.dto;

import jakarta.validation.constraints.NotNull;

public record CrewJoinSettingRequest(

        @NotNull(message = "자동가입승인 상태가 필요합니다.")
        Boolean autoApprove

) {
}