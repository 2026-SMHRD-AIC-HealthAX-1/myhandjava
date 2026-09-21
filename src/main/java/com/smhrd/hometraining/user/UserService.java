package com.smhrd.hometraining.user;

import com.smhrd.hometraining.common.exception.BusinessException;
import com.smhrd.hometraining.crew.CrewService;
import com.smhrd.hometraining.exercise.entity.ExerciseRecord;
import com.smhrd.hometraining.exercise.repository.ExerciseRecordRepository;
import com.smhrd.hometraining.exercise.session.repository.ExerciseSessionRepository;
import com.smhrd.hometraining.mission.repository.MissionCounterRepository;
import com.smhrd.hometraining.mission.repository.MissionRepository;
import com.smhrd.hometraining.shop.repository.UserItemRepository;
import com.smhrd.hometraining.support.repository.SupportTicketRepository;
import com.smhrd.hometraining.user.dto.AdminUserResponse;
import com.smhrd.hometraining.user.dto.CalibrationRequest;
import com.smhrd.hometraining.user.dto.PublicProfileResponse;
import com.smhrd.hometraining.user.dto.SocialOnboardingRequest;
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
import com.smhrd.hometraining.user.repository.UserResourceHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CalibrationProfileRepository calibrationProfileRepository;
    private final ExerciseRecordRepository exerciseRecordRepository;
    private final ExerciseSessionRepository exerciseSessionRepository;
    private final UserResourceHistoryRepository userResourceHistoryRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final UserItemRepository userItemRepository;
    private final MissionRepository missionRepository;
    private final MissionCounterRepository missionCounterRepository;
    private final UserResourceHistoryService resourceHistoryService;
    private final CrewService crewService;

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

    /**
     * 소셜 로그인으로 처음 가입한 사용자가 닉네임·동네·캐릭터(성별)를 최초로 확정합니다.
     *
     * updateProfile()과 달리 닉네임 변경권을 쓰지 않는다 — 소셜 로그인이 자동으로 지어준
     * 임시 닉네임을 "바꾸는" 게 아니라 처음 "정하는" 것이기 때문이다. 이미 동네를 설정해
     * 초기 설정을 마친 계정이 다시 호출하는 건 막는다(닉네임 변경권 없이 계속 닉네임을
     * 바꿔치기하는 것을 막기 위함).
     */
    @Transactional
    public UserResponse completeSocialOnboarding(
            Long userId,
            SocialOnboardingRequest req
    ) {

        User user = getUserOrThrow(userId);

        if (user.getRegionCity() != null) {
            throw new BusinessException(
                    "이미 초기 설정을 완료한 계정입니다."
            );
        }

        if (!req.nickname().equals(user.getNickname())
                && userRepository.existsByNickname(req.nickname())) {

            throw new BusinessException(
                    "이미 사용 중인 닉네임입니다."
            );
        }

        user.setNickname(req.nickname());
        user.setGender(req.genderEnum());
        user.setRegionCity(req.regionCity());
        user.setRegionGu(req.regionGu());
        user.setRegionDong(req.regionDong());

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

    /**
     * 회원탈퇴 시 이 사용자를 참조하는 모든 데이터를 정리한 뒤 계정을 삭제합니다.
     *
     * users 테이블은 크루/채팅/운동기록/상점/미션/고객센터 등 여러 테이블에서 외래키로
     * 참조되고 있어서, 바로 userRepository.delete(user)만 호출하면 외래키 제약 위반으로
     * 실패합니다. 크루 관련 정리는 CrewService에 위임하고, 나머지는 여기서 순서대로 지웁니다
     * (exercise_records가 exercise_sessions를 참조하므로 세션보다 먼저 지워야 합니다).
     */
    @Transactional
    public void withdraw(Long userId) {

        User user = getUserOrThrow(userId);

        crewService.cleanupAndLeaveForWithdrawal(userId);

        userResourceHistoryRepository.deleteByUserId(userId);
        calibrationProfileRepository.deleteByUserId(userId);
        supportTicketRepository.deleteByAuthorId(userId);
        userItemRepository.deleteByUserId(userId);
        missionRepository.deleteByUserId(userId);
        missionCounterRepository.deleteByUserId(userId);
        exerciseRecordRepository.deleteByUserId(userId);
        exerciseSessionRepository.deleteByUserId(userId);

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

        currentExp += expGain;

        while (true) {

            int requiredExp =
                    UserLevelPolicy.getRequiredExp(currentLevel);

            if (currentExp < requiredExp) {
                break;
            }

            /*
             * 최고 레벨이면 더 이상 레벨을 올리지 않습니다.
             */
            if (currentLevel >= UserLevelPolicy.MAX_LEVEL) {

                currentExp = Math.min(
                        currentExp,
                        requiredExp - 1
                );

                break;
            }

            currentExp -= requiredExp;
            currentLevel++;
        }

        /*
         * 등급은 더 이상 별도로 승급시키지 않고, 매번 현재 레벨에서 바로 계산합니다
         * (10레벨 단위로 아이언~챌린저, UserGrade.forLevel 참고).
         */
        user.setGrade(UserGrade.forLevel(currentLevel));
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

    /**
     * 관리자 "전체 사용자 관리" 화면의 회원 목록을 조회합니다.
     *
     * search가 있으면 닉네임 또는 이메일에 포함된 회원만 걸러서 돌려줍니다.
     */
    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsersForAdmin(String search) {

        List<User> users =
                (search == null || search.isBlank())
                        ? userRepository.findAll()
                        : userRepository.findByNicknameContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search);

        return users.stream()
                .sorted(Comparator.comparing(User::getCreatedAt).reversed())
                .map(user -> AdminUserResponse.of(
                        user,
                        exerciseRecordRepository.countByUserId(user.getId())
                ))
                .toList();
    }

    /** 회원을 정지시킵니다 — 정지된 계정은 다음 로그인 시도부터 거부됩니다(AuthService 참고). */
    @Transactional
    public void suspendUser(Long userId) {
        getUserOrThrow(userId).setStatus(User.Status.SUSPENDED);
    }

    /** 정지된 회원을 다시 활성 상태로 되돌립니다. */
    @Transactional
    public void activateUser(Long userId) {
        getUserOrThrow(userId).setStatus(User.Status.ACTIVE);
    }
}
