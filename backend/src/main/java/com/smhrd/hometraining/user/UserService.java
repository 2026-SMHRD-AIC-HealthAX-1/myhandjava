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
import com.smhrd.hometraining.user.entity.UserGrade;
import com.smhrd.hometraining.user.entity.UserResourceHistory.Reason;
import com.smhrd.hometraining.user.entity.UserResourceHistory.ResourceType;
import com.smhrd.hometraining.user.policy.UserLevelPolicy;
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

    private final UserRepository userRepository;
    private final CalibrationProfileRepository calibrationProfileRepository;
    private final ExerciseRecordRepository exerciseRecordRepository;
    private final UserResourceHistoryService resourceHistoryService;

    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {

        User user = getUserOrThrow(userId);

        boolean calibrationCompleted =
                calibrationProfileRepository
                        .findByUserId(userId)
                        .isPresent();

        return UserResponse.from(
                user,
                calibrationCompleted
        );
    }

    @Transactional
    public UserResponse updateProfile(
            Long userId,
            UpdateProfileRequest req
    ) {

        User user = getUserOrThrow(userId);

        if (req.nickname() != null
                && !req.nickname().isBlank()
                && !req.nickname().equals(user.getNickname())) {

            if (user.getNicknameTickets() <= 0) {
                throw new BusinessException(
                        "닉네임 변경권이 없습니다. 포인트 상점에서 구매해주세요."
                );
            }

            if (userRepository.existsByNickname(req.nickname())) {
                throw new BusinessException(
                        "이미 사용 중인 닉네임입니다."
                );
            }

            int ticketsBefore = user.getNicknameTickets();

            user.setNickname(req.nickname());
            user.setNicknameTickets(
                    ticketsBefore - 1
            );

            resourceHistoryService.record(
                    user,
                    ResourceType.NICKNAME_TICKET,
                    -1,
                    ticketsBefore,
                    user.getNicknameTickets(),
                    Reason.PROFILE_UPDATE,
                    null
            );
        }

        if (req.regionCity() != null) {
            user.setRegionCity(req.regionCity());
        }

        if (req.regionGu() != null) {
            user.setRegionGu(req.regionGu());
        }

        if (req.regionDong() != null) {
            user.setRegionDong(req.regionDong());
        }

        if (req.bio() != null) {
            user.setBio(req.bio());
        }

        if (req.profilePublic() != null) {
            user.setProfilePublic(req.profilePublic());
        }

        boolean calibrationCompleted =
                calibrationProfileRepository
                        .findByUserId(userId)
                        .isPresent();

        return UserResponse.from(
                user,
                calibrationCompleted
        );
    }

    /*
     * 랭킹에서 다른 사용자의 아바타를 클릭했을 때
     * 공개 프로필 정보를 반환합니다.
     *
     * 비공개 계정이면 닉네임을 제외한
     * 운동 기록은 반환하지 않습니다.
     */
    @Transactional(readOnly = true)
    public PublicProfileResponse getPublicProfile(Long userId) {

        User user = getUserOrThrow(userId);

        if (!user.isProfilePublic()) {
            return new PublicProfileResponse(
                    false,
                    user.getNickname(),
                    0,
                    null,
                    0,
                    0,
                    0,
                    0,
                    0,
                    List.of()
            );
        }

        List<ExerciseRecord> records =
                exerciseRecordRepository
                        .findByUserIdOrderByRecordedAtDesc(userId);

        long totalScore =
                records.stream()
                        .mapToLong(ExerciseRecord::getScore)
                        .sum();

        long perfect =
                records.stream()
                        .filter(record ->
                                record.getGrade()
                                        == ExerciseRecord.Grade.PERFECT
                        )
                        .count();

        long great =
                records.stream()
                        .filter(record ->
                                record.getGrade()
                                        == ExerciseRecord.Grade.GREAT
                        )
                        .count();

        long good =
                records.stream()
                        .filter(record ->
                                record.getGrade()
                                        == ExerciseRecord.Grade.GOOD
                        )
                        .count();

        long miss =
                records.stream()
                        .filter(record ->
                                record.getGrade()
                                        == ExerciseRecord.Grade.MISS
                        )
                        .count();

        Map<String, Long> byType =
                records.stream()
                        .collect(
                                Collectors.groupingBy(
                                        ExerciseRecord::getExerciseType,
                                        Collectors.counting()
                                )
                        );

        List<PublicProfileResponse.ExerciseCount> exerciseCounts =
                byType.entrySet()
                        .stream()
                        .map(entry ->
                                new PublicProfileResponse.ExerciseCount(
                                        entry.getKey(),
                                        entry.getValue()
                                )
                        )
                        .toList();

        return new PublicProfileResponse(
                true,
                user.getNickname(),
                user.getLevel(),
                user.getBio(),
                totalScore,
                perfect,
                great,
                good,
                miss,
                exerciseCounts
        );
    }

    @Transactional(readOnly = true)
    public String getCalibrationProfileJson(Long userId) {

        return calibrationProfileRepository
                .findByUserId(userId)
                .map(CalibrationProfile::getProfileJson)
                .orElse(null);
    }

    @Transactional
    public void saveCalibration(
            Long userId,
            CalibrationRequest req
    ) {

        User user = getUserOrThrow(userId);

        CalibrationProfile profile =
                calibrationProfileRepository
                        .findByUserId(userId)
                        .orElseGet(() ->
                                CalibrationProfile.of(
                                        user,
                                        req.profileJson()
                                )
                        );

        profile.setProfileJson(req.profileJson());
        profile.setCalibratedAt(
                java.time.LocalDateTime.now()
        );

        calibrationProfileRepository.save(profile);
    }

    @Transactional
    public long claimStreakReward(Long userId) {

        User user = getUserOrThrow(userId);

        if (user.getStreak() < 10) {
            throw new BusinessException(
                    "10일 연속출석부터 보상을 받을 수 있어요."
            );
        }

        if (user.isStreakRewardClaimed()) {
            throw new BusinessException(
                    "이번 보상을 이미 받았습니다."
            );
        }

        long reward = 300L;

        long pointsBefore = user.getPoints();

        user.setPoints(
                pointsBefore + reward
        );

        resourceHistoryService.record(
                user,
                ResourceType.POINT,
                reward,
                pointsBefore,
                user.getPoints(),
                Reason.STREAK_REWARD,
                null
        );

        user.setStreakRewardClaimed(true);

        return reward;
    }

    @Transactional
    public void withdraw(Long userId) {

        User user = getUserOrThrow(userId);
        userRepository.delete(user);
    }

    /**
     * 경험치와 포인트를 지급하고 레벨업을 처리합니다.
     *
     * 한 번에 많은 경험치를 획득한 경우
     * 여러 레벨을 연속으로 올릴 수 있습니다.
     *
     * 레벨 500을 달성하면 다음 등급으로 승급하고
     * 레벨은 다시 1부터 시작합니다.
     */
    @Transactional
    public void grantRewards(
            User user,
            int expGain,
            long pointsGain
    ) {

        grantRewards(
                user,
                expGain,
                pointsGain,
                Reason.SYSTEM_REWARD,
                null
        );
    }

    @Transactional
    public void grantRewards(
            User user,
            int expGain,
            long pointsGain,
            Reason reason,
            Object sourceId
    ) {

        if (user == null) {
            throw new BusinessException(
                    "보상을 지급할 사용자가 없습니다."
            );
        }

        if (expGain < 0) {
            throw new BusinessException(
                    "지급 경험치는 0 이상이어야 합니다."
            );
        }

        if (pointsGain < 0) {
            throw new BusinessException(
                    "지급 포인트는 0 이상이어야 합니다."
            );
        }

        if (reason == null) {
            throw new BusinessException(
                    "Reward reason is required."
            );
        }

        long pointsBefore = user.getPoints();
        int expBefore = user.getExp();

        user.setPoints(
                pointsBefore + pointsGain
        );

        int currentLevel = user.getLevel();
        int currentExp = user.getExp();

        UserGrade currentGrade =
                user.getGrade() == null
                        ? UserGrade.IRON
                        : user.getGrade();

        currentExp += expGain;

        while (true) {

            int requiredExp =
                    UserLevelPolicy.getRequiredExp(currentLevel);

            if (currentExp < requiredExp) {
                break;
            }

            /*
             * 최고 등급의 최고 레벨이면
             * 더 이상 레벨이나 등급을 올리지 않습니다.
             */
            if (currentLevel >= UserLevelPolicy.MAX_LEVEL
                    && currentGrade.isHighestGrade()) {

                currentExp = Math.min(
                        currentExp,
                        requiredExp - 1
                );

                break;
            }

            currentExp -= requiredExp;

            /*
             * 아직 레벨 500 미만이면
             * 같은 등급에서 레벨만 올립니다.
             */
            if (currentLevel < UserLevelPolicy.MAX_LEVEL) {
                currentLevel++;
                continue;
            }

            /*
             * 레벨 500을 넘으면 다음 등급으로 승급하고
             * 레벨을 1로 초기화합니다.
             */
            currentGrade = currentGrade.next();
            currentLevel = 1;
        }

        user.setGrade(currentGrade);
        user.setLevel(currentLevel);
        user.setExp(currentExp);

        resourceHistoryService.record(
                user,
                ResourceType.POINT,
                pointsGain,
                pointsBefore,
                user.getPoints(),
                reason,
                sourceId
        );

        resourceHistoryService.record(
                user,
                ResourceType.EXPERIENCE,
                expGain,
                expBefore,
                user.getExp(),
                reason,
                sourceId
        );
    }

    public User getUserOrThrow(Long userId) {

        return userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new BusinessException(
                                "사용자를 찾을 수 없습니다."
                        )
                );
    }
}
