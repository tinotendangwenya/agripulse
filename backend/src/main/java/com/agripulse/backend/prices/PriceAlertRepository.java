package com.agripulse.backend.prices;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {
    List<PriceAlert> findTop100ByUserIdOrderByIdDesc(Long userId);
    long countByUserIdAndStatus(Long userId, String status);
    Optional<PriceAlert> findByIdAndUserId(Long id, Long userId);

    @Query("""
        select a from PriceAlert a
        where a.status = 'ACTIVE' and lower(a.commodity) = lower(:c)
          and a.currency = :cur and a.unit = :u
          and (a.market is null or lower(a.market) = lower(:m))
        """)
    List<PriceAlert> findMatching(@Param("c") String commodity, @Param("cur") String currency,
                                  @Param("u") String unit, @Param("m") String market);
}
