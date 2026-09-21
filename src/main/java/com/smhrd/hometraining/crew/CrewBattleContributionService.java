package com.smhrd.hometraining.crew;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smhrd.hometraining.common.exception.BusinessException;
import com.smhrd.hometraining.crew.battle.entity.CrewBattle;
import com.smhrd.hometraining.crew.battle.entity.CrewBattleParticipant;
import com.smhrd.hometraining.crew.battle.repository.CrewBattleParticipantRepository;
import com.smhrd.hometraining.crew.dto.CrewBattleContributionResponse;
import com.smhrd.hometraining.crew.entity.CrewBattleContribution;
import com.smhrd.hometraining.crew.entity.CrewMember;
import com.smhrd.hometraining.crew.policy.CrewBattleContributionPolicy;
import com.smhrd.hometraining.crew.repository.CrewBattleContributionRepository;
import com.smhrd.hometraining.crew.repository.CrewMemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CrewBattleContributionService {

    private final CrewBattleContributionRepository
            contributionRepository;

    private final CrewBattleParticipantRepository
            participantRepository;

    private final CrewMemberRepository
            crewMemberRepository;

    /**
     * 대전 참가자가 획득한 점수를 크루·사용자별 기여도에 누적합니다.
     */
    @Transactional
    public void recordBattleContributions(
            CrewBattle battle
    ) {

        if (battle == null
                || !battle.isResultRecorded()) {

            throw new BusinessException(
                    "대전 결과가 저장된 후에만 기여도를 누적할 수 있습니다."
            );
        }

        List<CrewBattleParticipant> participants =
                participantRepository.findByBattle_Id(
                        battle.getId()
                );

        for (CrewBattleParticipant participant : participants) {

            if (participant.isContributionRecorded()) {
                continue;
            }

            Long crewId = participant.getCrew().getId();
            Long userId = participant.getUser().getId();

            CrewBattleContribution contribution =
                    contributionRepository
                            .findByCrewIdAndUserIdForUpdate(
                                    crewId,
                                    userId
                            )
                            .orElseGet(() ->
                                    contributionRepository
                                            .saveAndFlush(
                                                    CrewBattleContribution
                                                            .create(
                                                                    participant.getCrew(),
                                                                    participant.getUser()
                                                            )
                                            )
                            );

            contribution.addScore(
                    participant.getTotalScore()
            );

            participant.markContributionRecorded();
        }
    }

    /**
     * 현재 소속 크루의 사용자별 크루대전 기여도를 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<CrewBattleContributionResponse>
            getMyCrewContributions(
                    Long userId
            ) {

        CrewMember requester =
                crewMemberRepository
                        .findByUserId(userId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "소속된 크루가 없습니다."
                                )
                        );

        Long crewId = requester.getCrew().getId();

        Set<Long> currentMemberIds =
                crewMemberRepository
                        .findByCrewIdOrderByRoleAscJoinedAtAsc(
                                crewId
                        )
                        .stream()
                        .map(member -> member.getUser().getId())
                        .collect(Collectors.toSet());

        return contributionRepository
                .findByCrewIdOrderByTotalScoreDescUpdatedAtAsc(
                        crewId
                )
                .stream()
                .map(contribution ->
                        CrewBattleContributionResponse.from(
                                contribution,
                                currentMemberIds.contains(
                                        contribution.getUser().getId()
                                )
                        )
                )
                .toList();
    }

    @Transactional
    public void handleMemberDeparture(
            Long crewId,
            Long userId
    ) {

        if (CrewBattleContributionPolicy
                .shouldResetOnLeave()) {

            contributionRepository
                    .deleteByCrewIdAndUserId(
                            crewId,
                            userId
                    );
        }
    }

    @Transactional
    public void deleteCrewContributions(Long crewId) {
        contributionRepository.deleteByCrewId(crewId);
    }
}
