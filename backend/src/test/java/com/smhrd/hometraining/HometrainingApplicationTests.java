package com.smhrd.hometraining;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smhrd.hometraining.crew.CrewBattleContributionService;
import com.smhrd.hometraining.crew.CrewService;
import com.smhrd.hometraining.crew.battle.CrewBattleService;
import com.smhrd.hometraining.crew.battle.dto.CrewBattleDto;
import com.smhrd.hometraining.crew.battle.dto.CrewBattleRepRequest;
import com.smhrd.hometraining.crew.battle.dto.CrewBattleResultResponse;
import com.smhrd.hometraining.crew.battle.entity.CrewBattle;
import com.smhrd.hometraining.crew.battle.repository.CrewBattleRepository;
import com.smhrd.hometraining.crew.dto.CrewBattleContributionResponse;
import com.smhrd.hometraining.crew.entity.Crew;
import com.smhrd.hometraining.crew.entity.CrewMember;
import com.smhrd.hometraining.crew.repository.CrewMemberRepository;
import com.smhrd.hometraining.crew.repository.CrewRepository;
import com.smhrd.hometraining.exercise.entity.ExerciseRecord;
import com.smhrd.hometraining.user.entity.User;
import com.smhrd.hometraining.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class HometrainingApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CrewBattleService crewBattleService;

    @Autowired
    private CrewBattleRepository crewBattleRepository;

    @Autowired
    private CrewBattleContributionService
            crewBattleContributionService;

    @Autowired
    private CrewService crewService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CrewRepository crewRepository;

    @Autowired
    private CrewMemberRepository crewMemberRepository;

    @Test
    void coreUserAndCrewFlowWorks() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "smoketest01",
                                  "password": "testpass123",
                                  "email": "smoketest01@test.local",
                                  "nickname": "테스터",
                                  "gender": "male",
                                  "regionCity": "광주광역시",
                                  "regionGu": "동구",
                                  "regionDong": "충장동"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "smoketest01",
                                  "password": "testpass123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode loginJson = objectMapper.readTree(loginResponse);
        String token = loginJson.path("data").path("accessToken").asText();
        String authorization = "Bearer " + token;

        mockMvc.perform(get("/api/users/me").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loginId").value("smoketest01"));

        mockMvc.perform(post("/api/exercise-records")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "exerciseType": "squat",
                                  "reps": 20,
                                  "accuracy": 90,
                                  "score": 250,
                                  "perfectCount": 10,
                                  "greatCount": 5,
                                  "goodCount": 5,
                                  "missCount": 0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pointsAwarded").value(100));

        mockMvc.perform(post("/api/crews")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Smoke Test Crew",
                                  "description": "before",
                                  "concept": "beginner"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Smoke Test Crew"));

        mockMvc.perform(patch("/api/crews/me")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "after",
                                  "concept": "steady"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description").value("after"))
                .andExpect(jsonPath("$.data.concept").value("steady"));

        mockMvc.perform(patch("/api/crews/me")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"concept\":\"123456789012345678901\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void threeVsThreeCrewBattleUsesSelectedRoster() {
        TestTeam challenger = createTeam("battle-a", "Battle Crew A");
        TestTeam opponent = createTeam("battle-b", "Battle Crew B");

        Set<Long> challengerParticipants = Set.of(
                challenger.members().get(0).getId(),
                challenger.members().get(1).getId(),
                challenger.members().get(2).getId()
        );
        Set<Long> opponentParticipants = Set.of(
                opponent.members().get(0).getId(),
                opponent.members().get(1).getId(),
                opponent.members().get(2).getId()
        );

        CrewBattleDto.Response requested = crewBattleService.request(
                challenger.leader().getId(),
                new CrewBattleDto.CreateRequest(
                        3,
                        challengerParticipants,
                        "squat"
                )
        );

        assertThat(requested.status()).isEqualTo(CrewBattle.Status.WAITING);
        assertThat(requested.teamSize()).isEqualTo(3);
        assertThat(requested.startedAt()).isNull();

        CrewBattleDto.Response active = crewBattleService.request(
                opponent.leader().getId(),
                new CrewBattleDto.CreateRequest(
                        3,
                        opponentParticipants,
                        "squat"
                )
        );

        assertThat(active.id()).isEqualTo(requested.id());
        assertThat(active.status()).isEqualTo(CrewBattle.Status.ACTIVE);
        assertThat(active.startedAt()).isNotNull();
        assertThat(active.endsAt()).isAfter(active.startedAt());

        for (int i = 0; i < 12; i++) {
            crewBattleService.registerRep(
                    challenger.members().get(1).getId(),
                    new CrewBattleRepRequest(requested.id(), ExerciseRecord.Grade.PERFECT)
            );
        }

        for (int i = 0; i < 7; i++) {
            crewBattleService.registerRep(
                    opponent.members().get(2).getId(),
                    new CrewBattleRepRequest(requested.id(), ExerciseRecord.Grade.GREAT)
            );
        }

        CrewBattleDto.Response scoreboard = crewBattleService.get(
                challenger.leader().getId(), requested.id()
        );

        assertThat(scoreboard.challengerScore()).isEqualTo(1200);
        assertThat(scoreboard.opponentScore()).isEqualTo(560);
        assertThat(scoreboard.winnerCrewId()).isNull();
        assertThat(scoreboard.remainingSeconds()).isPositive();

        CrewBattle battle = crewBattleRepository
                .findById(requested.id())
                .orElseThrow();

        ReflectionTestUtils.setField(
                battle,
                "endsAt",
                LocalDateTime.now().minusSeconds(1)
        );

        crewBattleRepository.saveAndFlush(battle);

        CrewBattleResultResponse result =
                crewBattleService.getResult(
                        challenger.leader().getId(),
                        requested.id()
                );

        assertThat(result.status())
                .isEqualTo(CrewBattle.Status.FINISHED);

        assertThat(result.winnerCrewId())
                .isEqualTo(challenger.crew().getId());

        assertThat(result.drawResult()).isFalse();
        assertThat(result.resultRecorded()).isTrue();
        assertThat(result.rewardRecorded()).isTrue();

        assertThat(result.challenger().result())
                .isEqualTo(CrewBattle.BattleResult.WIN);

        assertThat(result.challenger().totalReps()).isEqualTo(12);
        assertThat(result.challenger().totalScore()).isEqualTo(1200);
        assertThat(result.challenger().rewardExp()).isEqualTo(100);
        assertThat(result.challenger().participants()).hasSize(3);

        assertThat(result.opponent().result())
                .isEqualTo(CrewBattle.BattleResult.LOSS);

        assertThat(result.opponent().totalReps()).isEqualTo(7);
        assertThat(result.opponent().totalScore()).isEqualTo(560);
        assertThat(result.opponent().rewardExp()).isEqualTo(50);
        assertThat(result.opponent().participants()).hasSize(3);

        Long scorerUserId =
                challenger.members().get(1).getId();

        CrewBattleContributionResponse contribution =
                crewBattleContributionService
                        .getMyCrewContributions(
                                challenger.leader().getId()
                        )
                        .stream()
                        .filter(item ->
                                item.userId().equals(scorerUserId)
                        )
                        .findFirst()
                        .orElseThrow();

        assertThat(contribution.totalScore()).isEqualTo(1200);
        assertThat(contribution.currentMember()).isTrue();

        CrewBattle finishedBattle = crewBattleRepository
                .findById(requested.id())
                .orElseThrow();

        crewBattleContributionService
                .recordBattleContributions(
                        finishedBattle
                );

        CrewBattleContributionResponse afterDuplicateRequest =
                crewBattleContributionService
                        .getMyCrewContributions(
                                challenger.leader().getId()
                        )
                        .stream()
                        .filter(item ->
                                item.userId().equals(scorerUserId)
                        )
                        .findFirst()
                        .orElseThrow();

        assertThat(afterDuplicateRequest.totalScore())
                .isEqualTo(1200);

        crewService.leave(scorerUserId);

        CrewBattleContributionResponse afterLeave =
                crewBattleContributionService
                        .getMyCrewContributions(
                                challenger.leader().getId()
                        )
                        .stream()
                        .filter(item ->
                                item.userId().equals(scorerUserId)
                        )
                        .findFirst()
                        .orElseThrow();

        assertThat(afterLeave.totalScore()).isEqualTo(1200);
        assertThat(afterLeave.currentMember()).isFalse();
    }

    private TestTeam createTeam(String prefix, String crewName) {
        List<User> members = new ArrayList<>();
        for (int i = 1; i <= Crew.MAX_MEMBERS; i++) {
            User user = User.register(
                    prefix + "-user-" + i,
                    "unused-password-hash",
                    prefix + "-user-" + i + "@test.local",
                    prefix + "-nickname-" + i,
                    User.Gender.MALE
            );
            members.add(userRepository.save(user));
        }

        Crew crew = crewRepository.save(Crew.create(
                crewName, "battle test crew", "steady", "Gwangju", "Dong-gu", "Test-dong"
        ));

        for (int i = 0; i < members.size(); i++) {
            CrewMember.Role role = i == 0 ? CrewMember.Role.LEADER : CrewMember.Role.MEMBER;
            crewMemberRepository.save(CrewMember.of(crew, members.get(i), role));
        }

        return new TestTeam(crew, members);
    }

    private record TestTeam(Crew crew, List<User> members) {
        User leader() {
            return members.get(0);
        }
    }
}
