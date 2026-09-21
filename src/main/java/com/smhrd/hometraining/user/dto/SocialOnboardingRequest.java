package com.smhrd.hometraining.user.dto;

import java.util.Locale;

import com.smhrd.hometraining.user.entity.User;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 소셜 로그인으로 처음 가입한 사용자가 닉네임·동네·캐릭터(성별)를 한 번에 설정하는 요청.
 *
 * updateProfile()의 닉네임 변경은 "이미 정한 닉네임을 바꾸는 것"이라 닉네임 변경권을 요구하지만,
 * 여긴 소셜 로그인이 자동으로 지어준 임시 닉네임을 처음으로 확정하는 것이라 그 값을 요구하지
 * 않는다(UserService.completeSocialOnboarding 참고).
 */
public record SocialOnboardingRequest(

        @NotBlank
        @Size(min = 2, max = 12)
        String nickname,

        @NotBlank
        @Pattern(
                regexp = "^(?i)(male|female)$",
                message = "성별은 male 또는 female만 가능합니다"
        )
        String gender,

        @NotBlank
        String regionCity,

        @NotBlank
        String regionGu,

        @NotBlank
        String regionDong

) {

    public User.Gender genderEnum() {
        return User.Gender.valueOf(
                gender.toUpperCase(Locale.ROOT)
        );
    }
}
