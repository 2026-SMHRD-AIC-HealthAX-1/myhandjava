package com.smhrd.hometraining.auth;

import com.smhrd.hometraining.auth.dto.*;
import com.smhrd.hometraining.common.exception.BusinessException;
import com.smhrd.hometraining.security.JwtTokenProvider;
import com.smhrd.hometraining.user.entity.User;
import com.smhrd.hometraining.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TEMP_PW_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private final SecureRandom random = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    private final ObjectMapper objectMapper;

    // 카카오/구글 키는 application-secrets.yml(로컬 전용, git에 커밋되지 않음)에서 채워진다 —
    // 팀 저장소가 퍼블릭이라 소스에 직접 박아두면 GitHub Push Protection이 푸시를 막는다.
    @Value("${app.oauth.kakao.rest-api-key}")
    private String kakaoRestApiKey;
    @Value("${app.oauth.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    @Value("${app.oauth.google.client-id}")
    private String googleClientId;
    @Value("${app.oauth.google.client-secret}")
    private String googleClientSecret;
    @Value("${app.oauth.google.redirect-uri}")
    private String googleRedirectUri;

    @Transactional
    public LoginResponse signup(SignupRequest req) {
        if (userRepository.existsByLoginId(req.loginId())) {
            throw new BusinessException("이미 사용 중인 아이디입니다.");
        }
        if (userRepository.existsByNickname(req.nickname())) {
            throw new BusinessException("이미 사용 중인 닉네임입니다.");
        }

        if (userRepository.existsByEmail(req.email())) {
            throw new BusinessException("이미 사용 중인 이메일입니다.");
        }

        User user = User.register(req.loginId(), passwordEncoder.encode(req.password()),
                req.email(), req.nickname(), req.genderEnum());
        user.setRegionCity(req.regionCity());
        user.setRegionGu(req.regionGu());
        user.setRegionDong(req.regionDong());
        userRepository.save(user);
        return issueToken(user);
    }

    @Transactional
    public LoginResponse login(LoginRequest req) {
        User user = userRepository.findByLoginId(req.loginId())
                .orElseThrow(() -> new BusinessException("아이디 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BusinessException("아이디 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED);
        }
        applyDailyAttendance(user);
        return issueToken(user);
    }

    /** 소셜 로그인 목업 — 실제로는 각 사 OAuth 인가 코드 교환 후 사용자 조회/생성이 필요하다. */
    @Transactional
    public LoginResponse socialLogin(String provider, String providerUserId, String email, String nickname) {
        String loginId = provider + ":" + providerUserId;
        User user = userRepository.findByLoginId(loginId)
                .or(() -> userRepository.findByEmail(email))
                .orElseGet(() -> {
                    String safeNickname = userRepository.existsByNickname(nickname) ? nickname + random.nextInt(9999)
                            : nickname;
                    User created = User.register(loginId,
                            passwordEncoder.encode(java.util.UUID.randomUUID().toString()),
                            email, safeNickname, User.Gender.MALE);
                    return userRepository.save(created);
                });
        applyDailyAttendance(user);
        return issueToken(user);
    }

    @Transactional
    public LoginResponse kakaoLogin(String code) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            // ① 인가코드 -> 카카오 액세스 토큰 교환
            String tokenBody = "grant_type=authorization_code"
                    + "&client_id=" + kakaoRestApiKey
                    + "&redirect_uri=" + URLEncoder.encode(kakaoRedirectUri, StandardCharsets.UTF_8)
                    + "&code=" + code;
            HttpRequest tokenRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://kauth.kakao.com/oauth/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(tokenBody))
                    .build();
            HttpResponse<String> tokenResponse = client.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
            JsonNode tokenJson = objectMapper.readTree(tokenResponse.body());
            if (tokenJson.has("error")) {
                throw new BusinessException("카카오 인증에 실패했습니다: " + tokenJson.path("error_description").asText());
            }
            String accessToken = tokenJson.get("access_token").asText();

            // ② 액세스 토큰으로 카카오 사용자 정보 조회
            HttpRequest userRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://kapi.kakao.com/v2/user/me"))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();
            HttpResponse<String> userResponse = client.send(userRequest, HttpResponse.BodyHandlers.ofString());
            JsonNode userJson = objectMapper.readTree(userResponse.body());

            String kakaoId = userJson.get("id").asText();
            String nickname = userJson.path("properties").path("nickname").asText("카카오유저");
            String email = "kakao_" + kakaoId + "@kakao.local"; // 이메일 동의항목을 못 받아서 임시 생성

            // ③ 기존 socialLogin() 재사용 — 우리 DB에 계정 생성/조회 + 우리 서비스 토큰 발급
            return socialLogin("kakao", kakaoId, email, nickname);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("카카오 로그인 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Transactional
    public LoginResponse googleLogin(String code) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            String tokenBody = "grant_type=authorization_code"
                    + "&client_id=" + googleClientId
                    + "&client_secret=" + googleClientSecret
                    + "&redirect_uri=" + URLEncoder.encode(googleRedirectUri, StandardCharsets.UTF_8)
                    + "&code=" + code;
            HttpRequest tokenRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(tokenBody))
                    .build();
            HttpResponse<String> tokenResponse = client.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
            JsonNode tokenJson = objectMapper.readTree(tokenResponse.body());
            if (tokenJson.has("error")) {
                throw new BusinessException("구글 인증에 실패했습니다: " + tokenJson.path("error_description").asText());
            }
            String accessToken = tokenJson.get("access_token").asText();

            HttpRequest userRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.googleapis.com/oauth2/v3/userinfo"))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();
            HttpResponse<String> userResponse = client.send(userRequest, HttpResponse.BodyHandlers.ofString());
            JsonNode userJson = objectMapper.readTree(userResponse.body());

            String googleId = userJson.get("sub").asText();
            String email = userJson.path("email").asText("google_" + googleId + "@gmail.local");
            String nickname = userJson.path("name").asText("구글유저");

            return socialLogin("google", googleId, email, nickname);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("구글 로그인 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public String findLoginIdByEmail(FindIdRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new BusinessException("가입된 계정을 찾을 수 없습니다."));
        return maskLoginId(user.getLoginId());
    }

    /** 임시 비밀번호를 발급한다. 실제 서비스에서는 이 값을 응답으로 내려주지 않고 이메일로만 발송해야 한다. */
    @Transactional
    public String issueTemporaryPassword(FindPasswordRequest req) {
        User user = userRepository.findByLoginId(req.loginId())
                .filter(u -> u.getEmail().equalsIgnoreCase(req.email()))
                .orElseThrow(() -> new BusinessException("일치하는 계정 정보를 찾을 수 없습니다."));
        String tempPassword = generateTempPassword();
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        return tempPassword;
    }

    private void applyDailyAttendance(User user) {
        var today = java.time.LocalDate.now();
        if (today.equals(user.getLastAttendanceDate()))
            return;
        boolean consecutive = user.getLastAttendanceDate() != null
                && user.getLastAttendanceDate().plusDays(1).equals(today);
        user.setStreak(consecutive ? user.getStreak() + 1 : 1);
        user.setStreakRewardClaimed(false);
        user.setLastAttendanceDate(today);
    }

    private LoginResponse issueToken(User user) {
        String token = jwtTokenProvider.createAccessToken(user.getId(), user.getLoginId());
        return new LoginResponse(token, user.getId(), user.getNickname());
    }

    private String maskLoginId(String loginId) {
        if (loginId.length() <= 2)
            return loginId;
        return loginId.substring(0, 2) + "*".repeat(loginId.length() - 2);
    }

    private String generateTempPassword() {
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++)
            sb.append(TEMP_PW_CHARS.charAt(random.nextInt(TEMP_PW_CHARS.length())));
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public boolean isLoginIdTaken(String loginId) {
        return userRepository.existsByLoginId(loginId);
    }

    @Transactional(readOnly = true)
    public boolean isNicknameTaken(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    @Transactional(readOnly = true)
    public boolean isEmailTaken(String email) {
        return userRepository.existsByEmail(email);
    }

}
