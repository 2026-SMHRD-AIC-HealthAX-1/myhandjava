package com.smhrd.hometraining.user.repository;

import com.smhrd.hometraining.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLoginId(String loginId);
    Optional<User> findByNickname(String nickname);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByLoginId(String loginId);
    boolean existsByNickname(String nickname);

    List<User> findTop50ByRegionCityAndRegionGuAndRegionDongOrderByPointsDesc(
            String regionCity, String regionGu, String regionDong);

    /** 동 단위 지역 랭킹 — 운동기록 총점(script.js의 totalScore()) 기준. */
    @Query("select u.id as userId, u.nickname as nickname, u.level as level, " +
            "coalesce(max(r.score), 0) as totalScore " +
            "from User u left join ExerciseRecord r on r.user = u " +
            "where u.regionCity = :city and u.regionGu = :gu and u.regionDong = :dong " +
            "group by u.id, u.nickname, u.level order by totalScore desc")
    List<ScoreRankingRow> findRegionRanking(@Param("city") String city, @Param("gu") String gu, @Param("dong") String dong);

    /** 동 단위 + 종목별 랭킹. */
    @Query("select u.id as userId, u.nickname as nickname, u.level as level, " +
            "coalesce(max(r.score), 0) as totalScore " +
            "from User u left join ExerciseRecord r on r.user = u and r.exerciseType = :exerciseType " +
            "where u.regionCity = :city and u.regionGu = :gu and u.regionDong = :dong " +
            "group by u.id, u.nickname, u.level order by totalScore desc")
    List<ScoreRankingRow> findExerciseRanking(@Param("city") String city, @Param("gu") String gu,
                                               @Param("dong") String dong, @Param("exerciseType") String exerciseType);

    interface ScoreRankingRow {
        Long getUserId();
        String getNickname();
        int getLevel();
        long getTotalScore();
    }
}
