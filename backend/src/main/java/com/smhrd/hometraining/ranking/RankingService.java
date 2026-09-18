package com.smhrd.hometraining.ranking;

import com.smhrd.hometraining.crew.entity.Crew;
import com.smhrd.hometraining.crew.repository.CrewMemberRepository;
import com.smhrd.hometraining.crew.repository.CrewRepository;
import com.smhrd.hometraining.exercise.repository.ExerciseRecordRepository;
import com.smhrd.hometraining.ranking.dto.CrewRankingRowResponse;
import com.smhrd.hometraining.ranking.dto.RankingRowResponse;
import com.smhrd.hometraining.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RankingService {

    private final UserRepository userRepository;
    private final CrewRepository crewRepository;
    private final CrewMemberRepository crewMemberRepository;
    private final ExerciseRecordRepository exerciseRecordRepository;

    @Transactional(readOnly = true)
    public List<RankingRowResponse> regionRanking(String city, String gu, String dong, Long requesterId) {
        return toRows(userRepository.findRegionRanking(city, gu, dong), requesterId);
    }

    @Transactional(readOnly = true)
    public List<RankingRowResponse> exerciseRanking(String city, String gu, String dong, String exerciseType, Long requesterId) {
        return toRows(userRepository.findExerciseRanking(city, gu, dong, exerciseType), requesterId);
    }

    @Transactional(readOnly = true)
    public List<CrewRankingRowResponse> crewRanking(String city, String gu) {
        List<Crew> crews = crewRepository.findByRegionCityAndRegionGuOrderByLevelDesc(city, gu);
        List<CrewRankingRowResponse> rows = new ArrayList<>();
        for (Crew crew : crews) {
            List<Long> memberIds = crewMemberRepository.findByCrewIdOrderByRoleAscJoinedAtAsc(crew.getId())
                    .stream().map(m -> m.getUser().getId()).toList();
            long totalScore = memberIds.isEmpty() ? 0 : exerciseRecordRepository.sumScoreByUserIds(memberIds);
            rows.add(new CrewRankingRowResponse(0, crew.getId(), crew.getName(), crew.getLevel(), totalScore, crew.getRegionDong()));
        }
        rows.sort((a, b) -> Long.compare(b.totalScore(), a.totalScore()));
        List<CrewRankingRowResponse> ranked = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            CrewRankingRowResponse r = rows.get(i);
            ranked.add(new CrewRankingRowResponse(i + 1, r.crewId(), r.crewName(), r.level(), r.totalScore(), r.region()));
        }
        return ranked;
    }

    private List<RankingRowResponse> toRows(List<UserRepository.ScoreRankingRow> raw, Long requesterId) {
        List<RankingRowResponse> rows = new ArrayList<>();
        for (int i = 0; i < raw.size(); i++) {
            UserRepository.ScoreRankingRow row = raw.get(i);
            rows.add(new RankingRowResponse(i + 1, row.getUserId(), row.getNickname(), row.getLevel(),
                    row.getTotalScore(), row.getUserId().equals(requesterId)));
        }
        return rows;
    }
}
