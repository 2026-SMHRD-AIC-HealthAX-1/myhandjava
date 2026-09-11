package com.smhrd.hometraining.mission;

import com.smhrd.hometraining.common.exception.BusinessException;
import com.smhrd.hometraining.mission.dto.MissionResponse;
import com.smhrd.hometraining.mission.entity.Mission;
import com.smhrd.hometraining.mission.entity.MissionCounter;
import com.smhrd.hometraining.mission.entity.MissionMetric;
import com.smhrd.hometraining.mission.repository.MissionCounterRepository;
import com.smhrd.hometraining.mission.repository.MissionRepository;
import com.smhrd.hometraining.user.UserService;
import com.smhrd.hometraining.user.entity.User;
import com.smhrd.hometraining.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MissionService {

    private static final int DAILY_MISSION_COUNT = 3;
    private final SecureRandom random = new SecureRandom();

    private final MissionRepository missionRepository;
    private final MissionCounterRepository counterRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Transactional
    public List<MissionResponse> getTodayMissions(Long userId) {
        LocalDate today = LocalDate.now();
        List<Mission> missions = missionRepository.findByUserIdAndAssignedDate(userId, today);
        if (missions.isEmpty()) {
            User user = userService.getUserOrThrow(userId);
            MissionMetric[] metrics = MissionMetric.values();
            for (int i = 0; i < DAILY_MISSION_COUNT; i++) {
                MissionMetric metric = metrics[random.nextInt(metrics.length)];
                int target = metric.rollTarget(random);
                missions.add(missionRepository.save(Mission.generate(user, metric, target, today)));
            }
        }
        MissionCounter counter = getOrCreateCounter(userId, today);
        return missions.stream().map(m -> MissionResponse.of(m, counter.valueOf(m.getMetric()))).toList();
    }

    @Transactional
    public long claim(Long userId, Long missionId) {
        Mission mission = missionRepository.findById(missionId)
                .filter(m -> m.getUser().getId().equals(userId))
                .orElseThrow(() -> new BusinessException("미션을 찾을 수 없습니다."));
        if (mission.isClaimed()) throw new BusinessException("이미 보상을 받은 미션입니다.");
        MissionCounter counter = getOrCreateCounter(userId, mission.getAssignedDate());
        if (counter.valueOf(mission.getMetric()) < mission.getTarget()) {
            throw new BusinessException("아직 목표를 달성하지 못했습니다.");
        }
        mission.setClaimed(true);
        User user = userService.getUserOrThrow(userId);
        userService.grantRewards(user, 0, mission.getReward());
        return mission.getReward();
    }

    /** saveExerciseResult()에서 스쿼트 세션이 저장될 때마다 호출되어 오늘의 카운터를 갱신한다. */
    @Transactional
    public void recordSquatSession(Long userId, int validReps, int perfectCount, int missCount, int accuracy) {
        LocalDate today = LocalDate.now();
        MissionCounter counter = getOrCreateCounter(userId, today);
        counter.setReps(counter.getReps() + validReps);
        counter.setPerfect(counter.getPerfect() + perfectCount);
        counter.setSessions(counter.getSessions() + 1);
        if (missCount == 0) counter.setMissFreeSession(counter.getMissFreeSession() + 1);
        if (accuracy >= 90) counter.setAccSession(counter.getAccSession() + 1);
    }

    private MissionCounter getOrCreateCounter(Long userId, LocalDate date) {
        return counterRepository.findByUserIdAndCounterDate(userId, date)
                .orElseGet(() -> counterRepository.save(MissionCounter.startFor(userRepository.getReferenceById(userId), date)));
    }
}
