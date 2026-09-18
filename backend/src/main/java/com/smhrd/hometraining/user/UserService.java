package com.smhrd.hometraining.user;

import com.smhrd.hometraining.common.exception.BusinessException;
import com.smhrd.hometraining.exercise.entity.ExerciseRecord;
import com.smhrd.hometraining.exercise.repository.ExerciseRecordRepository;
import com.smhrd.hometraining.user.dto.CalibrationRequest;
import com.smhrd.hometraining.user.dto.PublicProfileResponse;
import com.smhrd.hometraining.user.dto.UpdateProfileRequest;
import com.smhrd.hometraining.user.dto.UserResponse;
import com.smhrd.hometraining.user.entity.CalibrationProfile;
import com.smhrd.hometraining.user.entity.User;
import com.smhrd.hometraining.user.repository.CalibrationProfileRepository;
import com.smhrd.hometraining.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    public static final int EXP_PER_LEVEL = UserResponse.EXP_PER_LEVEL;

    private final UserRepository userRepository;
    private final CalibrationProfileRepository calibrationProfileRepository;
    private final ExerciseRecordRepository exerciseRecordRepository;

    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        return UserResponse.from(getUserOrThrow(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest req) {
        User user = getUserOrThrow(userId);

        if (req.nickname() != null && !req.nickname().isBlank() && !req.nickname().equals(user.getNickname())) {
            if (user.getNicknameTickets() <= 0) {
                throw new BusinessException("닉네임 변경권이 없습니다. 포인트 상점에서 구매해주세요.");
            }
            if (userRepository.existsByNickname(req.nickname())) {
                throw new BusinessException("이미 사용 중인 닉네임입니다.");
            }
            user.setNickname(req.nickname());
            user.setNicknameTickets(user.getNicknameTickets() - 1);
        }
        if (req.regionCity() != null) user.setRegionCity(req.regionCity());
        if (req.regionGu() != null) user.setRegionGu(req.regionGu());
        if (req.regionDong() != null) user.setRegionDong(req.regionDong());
        if (req.bio() != null) user.setBio(req.bio());
        if (req.profilePublic() != null) user.setProfilePublic(req.profilePublic());

        return UserResponse.from(user);
    }

    // 랭킹 단상 아바타를 클릭했을 때 보여주는 다른 사용자의 공개 프로필. 비공개 계정이면
    // nickname만 채우고 나머지는 비워서 돌려준다 — 실제로 막는 건 이 메서드 안쪽이라, 프론트가
    // isPublic을 무시해도 비공개 사용자의 기록이 새어나가지 않는다.
    @Transactional(readOnly = true)
    public PublicProfileResponse getPublicProfile(Long userId) {
        User user = getUserOrThrow(userId);
        if (!user.isProfilePublic()) {
            return new PublicProfileResponse(false, user.getNickname(), 0, null, 0, 0, 0, 0, 0, List.of());
        }
        List<ExerciseRecord> records = exerciseRecordRepository.findByUserIdOrderByRecordedAtDesc(userId);
        long totalScore = records.stream().mapToLong(ExerciseRecord::getScore).sum();
        long perfect = records.stream().filter(r -> r.getGrade() == ExerciseRecord.Grade.PERFECT).count();
        long great = records.stream().filter(r -> r.getGrade() == ExerciseRecord.Grade.GREAT).count();
        long good = records.stream().filter(r -> r.getGrade() == ExerciseRecord.Grade.GOOD).count();
        long miss = records.stream().filter(r -> r.getGrade() == ExerciseRecord.Grade.MISS).count();
        Map<String, Long> byType = records.stream()
                .collect(Collectors.groupingBy(ExerciseRecord::getExerciseType, Collectors.counting()));
        List<PublicProfileResponse.ExerciseCount> exerciseCounts = byType.entrySet().stream()
                .map(e -> new PublicProfileResponse.ExerciseCount(e.getKey(), e.getValue()))
                .toList();
        return new PublicProfileResponse(true, user.getNickname(), user.getLevel(), user.getBio(),
                totalScore, perfect, great, good, miss, exerciseCounts);
    }

    @Transactional(readOnly = true)
    public String getCalibrationProfileJson(Long userId) {
        return calibrationProfileRepository.findByUserId(userId)
                .map(CalibrationProfile::getProfileJson)
                .orElse(null);
    }

    @Transactional
    public void saveCalibration(Long userId, CalibrationRequest req) {
        User user = getUserOrThrow(userId);
        CalibrationProfile profile = calibrationProfileRepository.findByUserId(userId)
                .orElseGet(() -> CalibrationProfile.of(user, req.profileJson()));
        profile.setProfileJson(req.profileJson());
        profile.setCalibratedAt(java.time.LocalDateTime.now());
        calibrationProfileRepository.save(profile);
    }

    @Transactional
    public long claimStreakReward(Long userId) {
        User user = getUserOrThrow(userId);
        if (user.getStreak() < 10) throw new BusinessException("10일 연속출석부터 보상을 받을 수 있어요.");
        if (user.isStreakRewardClaimed()) throw new BusinessException("이번 보상을 이미 받았습니다.");
        long reward = 300;
        user.setPoints(user.getPoints() + reward);
        user.setStreakRewardClaimed(true);
        return reward;
    }

    @Transactional
    public void withdraw(Long userId) {
        userRepository.deleteById(userId);
    }

    /** 미션·운동기록·상점 등 여러 도메인에서 공통으로 쓰는 경험치·포인트 지급 + 레벨업 처리. */
    @Transactional
    public void grantRewards(User user, int expGain, long pointsGain) {
        user.setPoints(user.getPoints() + pointsGain);
        int totalExp = user.getExp() + expGain;
        int levelUps = totalExp / EXP_PER_LEVEL;
        if (levelUps > 0) {
            user.setLevel(user.getLevel() + levelUps);
            // 5레벨마다 기본 세트 한도가 자동으로 늘어난다.
            if ((user.getLevel() / 5) > ((user.getLevel() - levelUps) / 5)) {
                user.setExtraSets(user.getExtraSets() + 3);
            }
        }
        user.setExp(totalExp % EXP_PER_LEVEL);
    }

    public User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다."));
    }
}
