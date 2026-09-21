package com.smhrd.hometraining.crew.repository;

import com.smhrd.hometraining.crew.entity.Crew;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// [DB 접근 지점] crews 테이블 — 여기 메서드가 실제 SQL(JPQL)을 실행한다.
public interface CrewRepository extends JpaRepository<Crew, Long> {
    boolean existsByName(String name);

    /**
     * regionCity/regionGu가 null이면 그 단위는 필터링하지 않는다 — 크루 랭킹에서 "전체"를
     * 고르면 둘 다 null로 넘어와 전국 크루를 대상으로 조회한다(RankingService 참고).
     */
    @Query("select c from Crew c " +
            "where (:regionCity is null or c.regionCity = :regionCity) " +
            "and (:regionGu is null or c.regionGu = :regionGu) " +
            "order by c.level desc")
    List<Crew> findByRegionCityAndRegionGuOrderByLevelDesc(@Param("regionCity") String regionCity,
                                                             @Param("regionGu") String regionGu);
}
