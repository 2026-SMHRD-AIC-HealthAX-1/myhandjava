package com.smhrd.hometraining.user.repository;

import com.smhrd.hometraining.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// [DB 접근 지점] users 테이블 — 이 프로젝트에서 가장 많이 참조되는 핵심 테이블.
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLoginId(String loginId);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByLoginId(String loginId);
    boolean existsByNickname(String nickname);

    List<User> findTop50ByRegionCityAndRegionGuAndRegionDongOrderByPointsDesc(
            String regionCity, String regionGu, String regionDong);

    /** 관리자 전체 사용자 관리 화면의 검색 — 닉네임 또는 이메일에 검색어가 포함된 회원을 찾는다. */
    List<User> findByNicknameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String nickname, String email);

    /**
     * 동 단위 지역 랭킹 — 운동기록 총점(script.js의 totalScore()) 기준.
     *
     * city/gu/dong은 null이면 그 단위는 필터링하지 않는다 — "전체"를 고르면 city까지 null로
     * 보내 전국 랭킹을 조회한다.
     */
    @Query("select u.id as userId, u.nickname as nickname, u.level as level, " +
            "coalesce(max(r.score), 0) as totalScore " +
            "from User u left join ExerciseRecord r on r.user = u " +
            "where (:city is null or u.regionCity = :city) " +
            "and (:gu is null or u.regionGu = :gu) " +
            "and (:dong is null or u.regionDong = :dong) " +
            "group by u.id, u.nickname, u.level order by totalScore desc")
    List<ScoreRankingRow> findRegionRanking(@Param("city") String city, @Param("gu") String gu, @Param("dong") String dong);

    /**
     * 동 단위 + 종목별 랭킹.
     *
     * city/gu/dong은 null이면 그 단위는 필터링하지 않는다 — 프론트에서 "전체"를 선택하면
     * city까지 null로 보내 전국 랭킹을 조회한다.
     */
    @Query("select u.id as userId, u.nickname as nickname, u.level as level, " +
            "coalesce(max(r.score), 0) as totalScore " +
            "from User u left join ExerciseRecord r on r.user = u and r.exerciseType = :exerciseType " +
            "where (:city is null or u.regionCity = :city) " +
            "and (:gu is null or u.regionGu = :gu) " +
            "and (:dong is null or u.regionDong = :dong) " +
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
