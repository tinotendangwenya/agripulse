package com.agripulse.backend.records;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LivestockRepository extends JpaRepository<LivestockEvent, Long> {
    List<LivestockEvent> findTop300ByFarmIdOrderByEventDateDescIdDesc(Long farmId);

    Optional<LivestockEvent> findByIdAndFarmId(Long id, Long farmId);

    @Query("""
        select e.animalType, e.eventType, sum(e.headCount) from LivestockEvent e
        where e.farmId = :farmId and e.eventDate <= :asOf
        group by e.animalType, e.eventType
        """)
    List<Object[]> balances(@Param("farmId") Long farmId, @Param("asOf") LocalDate asOf);
}
