package com.smhrd.hometraining.ranking;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smhrd.hometraining.crew.entity.Crew;
import com.smhrd.hometraining.crew.repository.CrewMemberRepository;
import com.smhrd.hometraining.crew.repository.CrewRepository;
import com.smhrd.hometraining.exercise.entity.ExerciseRecord;
import com.smhrd.hometraining.exercise.repository.ExerciseRecordRepository;
import com.smhrd.hometraining.ranking.dto.CrewRankingRowResponse;
import com.smhrd.hometraining.ranking.dto.RankingRowResponse;
import com.smhrd.hometraining.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RankingService {

    private final UserRepository userRepository;
    private final CrewRepository crewRepository;
    private final CrewMemberRepository crewMemberRepository;
    private final ExerciseRecordRepository exerciseRecordRepository;

    /**
     * 지역 사용자 랭킹을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<RankingRowResponse> regionRanking(
            String city,
            String gu,
            String dong,
            Long requesterId
    ) {

        return toRows(
                userRepository.findRegionRanking(
                        city,
                        gu,
                        dong,
                        ExerciseRecord.SessionType.RANKED
                ),
                requesterId
        );
    }

    /**
     * 운동 종목별 사용자 랭킹을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<RankingRowResponse> exerciseRanking(
            String city,
            String gu,
            String dong,
            String exerciseType,
            Long requesterId
    ) {

        return toRows(
                userRepository.findExerciseRanking(
                        city,
                        gu,
                        dong,
                        exerciseType,
                        ExerciseRecord.SessionType.RANKED
                ),
                requesterId
        );
    }

    /**
     * 지역별 크루 랭킹을 조회합니다.
     *
     * 정렬 기준:
     * 1. 크루 레벨이 높은 순서
     * 2. 레벨이 같으면 경험치가 높은 순서
     * 3. 경험치까지 같으면 먼저 달성한 순서
     * 4. 달성 시간까지 같으면 크루 ID가 작은 순서
     */
    @Transactional(readOnly = true)
    public List<CrewRankingRowResponse> crewRanking(
            String city,
            String gu
    ) {

        List<Crew> crews =
                new ArrayList<>(
                        crewRepository
                                .findByRegionCityAndRegionGuOrderByLevelDesc(
                                        city,
                                        gu
                                )
                );

        crews.sort(
                Comparator
                        .comparingInt(Crew::getLevel)
                        .reversed()

                        .thenComparing(
                                Comparator
                                        .comparingInt(Crew::getExp)
                                        .reversed()
                        )

                        .thenComparing(
                                Crew::rankingAchievedAtOrCreatedAt,
                                Comparator.nullsLast(
                                        Comparator.naturalOrder()
                                )
                        )

                        .thenComparing(
                                Crew::getId
                        )
        );

        List<CrewRankingRowResponse> ranked =
                new ArrayList<>();

        for (int i = 0; i < crews.size(); i++) {

            Crew crew =
                    crews.get(i);

            List<Long> memberIds =
                    crewMemberRepository
                            .findByCrewIdOrderByRoleAscJoinedAtAsc(
                                    crew.getId()
                            )
                            .stream()
                            .map(member ->
                                    member.getUser().getId()
                            )
                            .toList();

            long totalScore =
                    memberIds.isEmpty()
                            ? 0
                            : exerciseRecordRepository
                                    .sumScoreByUserIds(
                                            memberIds,
                                            ExerciseRecord.SessionType.RANKED
                                    );

            CrewRankingRowResponse response =
                    new CrewRankingRowResponse(
                            i + 1,
                            crew.getId(),
                            crew.getName(),
                            crew.getLevel(),
                            totalScore,
                            crew.getRegionDong()
                    );

            ranked.add(response);
        }

        return ranked;
    }

    /**
     * 사용자 랭킹 조회 결과에 순위를 부여합니다.
     */
    private List<RankingRowResponse> toRows(
            List<UserRepository.ScoreRankingRow> raw,
            Long requesterId
    ) {

        List<RankingRowResponse> rows =
                new ArrayList<>();

        for (int i = 0; i < raw.size(); i++) {

            UserRepository.ScoreRankingRow row =
                    raw.get(i);

            rows.add(
                    new RankingRowResponse(
                            i + 1,
                            row.getUserId(),
                            row.getNickname(),
                            row.getLevel(),
                            row.getTotalScore(),
                            row.getUserId().equals(
                                    requesterId
                            )
                    )
            );
        }

        return rows;
    }
}