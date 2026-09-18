package com.smhrd.hometraining.auth.dto;

import com.smhrd.hometraining.user.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Size(min = 4, max = 20) String loginId,
        @NotBlank @Size(min = 8, max = 64, message = "8자 이상 64자 이하로 입력해주세요") String password,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 2, max = 12) String nickname,
        @NotBlank String gender,
        String regionCity,
        String regionGu,
        String regionDong
) {
    public User.Gender genderEnum() {
        return "female".equalsIgnoreCase(gender) ? User.Gender.FEMALE : User.Gender.MALE;
    }
}
