package com.agripulse.backend.prices;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/** Read queries for the price board and history charts. */
public interface PriceBoardRepository extends JpaRepository<MarketPrice, Long> {
    List<MarketPrice> findTop5000ByPriceDateGreaterThanEqualOrderByPriceDateDescIdDesc(LocalDate from);

    @Query("""
        select p from MarketPrice p
        where lower(p.commodity) = lower(:c) and lower(p.market) = lower(:m)
          and p.currency = :cur and p.unit = :u and p.priceDate >= :from
        order by p.priceDate asc, p.id asc
        """)
    List<MarketPrice> series(@Param("c") String commodity, @Param("m") String market, @Param("cur") String currency,
                             @Param("u") String unit, @Param("from") LocalDate from, Pageable pageable);
}
