package com.smhrd.hometraining.crew.repository;

import com.smhrd.hometraining.crew.entity.Crew;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrewRepository extends JpaRepository<Crew, Long> {
    boolean existsByName(String name);
    List<Crew> findByRegionCityAndRegionGuOrderByLevelDesc(String regionCity, String regionGu);
}
