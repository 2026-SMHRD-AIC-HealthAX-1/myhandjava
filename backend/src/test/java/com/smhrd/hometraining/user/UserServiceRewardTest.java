package com.smhrd.hometraining.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.smhrd.hometraining.common.exception.BusinessException;
import com.smhrd.hometraining.exercise.repository.ExerciseRecordRepository;
import com.smhrd.hometraining.user.entity.User;
import com.smhrd.hometraining.user.entity.UserGrade;
import com.smhrd.hometraining.user.repository.CalibrationProfileRepository;
import com.smhrd.hometraining.user.repository.UserRepository;

class UserServiceRewardTest {

    private UserService userService;

    @BeforeEach
    void setUp() {

        UserRepository userRepository =
                mock(UserRepository.class);

        CalibrationProfileRepository calibrationProfileRepository =
                mock(CalibrationProfileRepository.class);

        ExerciseRecordRepository exerciseRecordRepository =
                mock(ExerciseRecordRepository.class);

        UserResourceHistoryService resourceHistoryService =
                mock(UserResourceHistoryService.class);

        userService = new UserService(
                userRepository,
                calibrationProfileRepository,
                exerciseRecordRepository,
                resourceHistoryService
        );
    }

    @Test
    void normalLevelUpCarriesRemainingExp() {

        User user = createUser();

        user.setLevel(1);
        user.setExp(80);
        user.setPoints(10);
        user.setGrade(UserGrade.IRON);

        userService.grantRewards(
                user,
                50,
                20
        );

        assertEquals(
                2,
                user.getLevel()
        );

        assertEquals(
                30,
                user.getExp()
        );

        assertEquals(
                30,
                user.getPoints()
        );

        assertEquals(
                UserGrade.IRON,
                user.getGrade()
        );
    }

    @Test
    void multipleLevelsCanIncreaseAtOnce() {

        User user = createUser();

        user.setLevel(1);
        user.setExp(0);
        user.setGrade(UserGrade.IRON);

        userService.grantRewards(
                user,
                250,
                0
        );

        assertEquals(
                3,
                user.getLevel()
        );

        assertEquals(
                50,
                user.getExp()
        );

        assertEquals(
                UserGrade.IRON,
                user.getGrade()
        );
    }

    @Test
    void level500CompletionPromotesGrade() {

        User user = createUser();

        user.setLevel(500);
        user.setExp(2450);
        user.setGrade(UserGrade.IRON);

        userService.grantRewards(
                user,
                100,
                0
        );

        assertEquals(
                UserGrade.BRONZE,
                user.getGrade()
        );

        assertEquals(
                1,
                user.getLevel()
        );

        assertEquals(
                50,
                user.getExp()
        );
    }

    @Test
    void challengerLevel500StopsAtMaximumExp() {

        User user = createUser();

        user.setLevel(500);
        user.setExp(2450);
        user.setGrade(UserGrade.CHALLENGER);

        userService.grantRewards(
                user,
                100,
                0
        );

        assertEquals(
                UserGrade.CHALLENGER,
                user.getGrade()
        );

        assertEquals(
                500,
                user.getLevel()
        );

        assertEquals(
                2500,
                user.getExp()
        );
    }

    @Test
    void negativeExperienceCannotBeGranted() {

        User user = createUser();

        assertThrows(
                BusinessException.class,
                () -> userService.grantRewards(
                        user,
                        -1,
                        0
                )
        );
    }

    @Test
    void negativePointsCannotBeGranted() {

        User user = createUser();

        assertThrows(
                BusinessException.class,
                () -> userService.grantRewards(
                        user,
                        0,
                        -1
                )
        );
    }

    /**
     * 테스트에서 사용할 기본 사용자를 만듭니다.
     */
    private User createUser() {

        User user = User.register(
                "test-user",
                "encoded-password",
                "test@example.com",
                "테스트사용자",
                User.Gender.MALE
        );

        user.setLevel(1);
        user.setExp(0);
        user.setPoints(0);
        user.setExtraSets(0);
        user.setGrade(UserGrade.IRON);

        return user;
    }
}
