package com.gdou.marinebio.repository;

import com.gdou.marinebio.entity.ObservationSpecies;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ObservationSpeciesRepository extends JpaRepository<ObservationSpecies, Integer> {

    List<ObservationSpecies> findByObservationId(Integer observationId);

    void deleteByObservationId(Integer observationId);

    /** 某物种被多少条观测记录关联，删除物种前判断影响面用 */
    long countBySpeciesId(Integer speciesId);

    /** 地图上要显示每条记录关联了几个物种，一次分组查询代替逐条查询 */
    @Query("select os.observation.id, count(os.id) from ObservationSpecies os group by os.observation.id")
    List<Object[]> countGroupByObservation();
}
