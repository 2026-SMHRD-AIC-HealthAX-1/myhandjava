package com.smhrd.hometraining.user;

import com.smhrd.hometraining.common.ApiResponse;
import com.smhrd.hometraining.user.dto.PublicProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// UserController는 "/api/users/me"(인증된 본인) 전용이라, 남의 프로필을 userId로 조회하는
// 이 엔드포인트는 별도 컨트롤러로 뺐다. 랭킹처럼 비회원도 볼 수 있게 permitAll이라
// SecurityConfig에도 등록돼 있다.
// [프론트 연동] ounhome-f/js/ranking.js openPublicProfile() — 랭킹 단상 클릭 시 팝업.
// [DB] UserService.getPublicProfile() → users, exercise_records 집계 → isPublic=false면 닉네임만 반환.
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class PublicProfileController {

    private final UserService userService;

    @GetMapping("/{userId}/public-profile")
    public ApiResponse<PublicProfileResponse> getPublicProfile(@PathVariable Long userId) {
        return ApiResponse.ok(userService.getPublicProfile(userId));
    }
}
