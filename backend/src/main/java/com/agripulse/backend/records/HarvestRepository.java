package com.agripulse.backend.records;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HarvestRepository extends JpaRepository<Harvest, Long> {
    List<Harvest> findTop500ByFarmIdAndHarvestDateBetweenOrderByHarvestDateDescIdDesc(Long farmId, LocalDate from, LocalDate to);

    Optional<Harvest> findByIdAndFarmId(Long id, Long farmId);

    @Query("""
        select h.cropRecordId, sum(h.quantityKg) from Harvest h
        where h.farmId = :farmId and h.harvestDate between :fromDate and :toDate
        group by h.cropRecordId
        """)
    List<Object[]> totals(@Param("farmId") Long farmId, @Param("fromDate") LocalDate from, @Param("toDate") LocalDate to);
}
