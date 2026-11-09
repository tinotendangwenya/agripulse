package com.agripulse.backend.prices;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MarketPriceRepository extends JpaRepository<MarketPrice, Long> {

    List<MarketPrice> findByCommodityIgnoreCaseAndPriceDateGreaterThanEqualOrderByPriceDateAsc(
            String commodity, LocalDate from);

    // Most recent price per market for one commodity and currency
    @Query("""
        select p from MarketPrice p
        where lower(p.commodity) = lower(:commodity) and p.currency = :currency
          and p.priceDate = (select max(p2.priceDate) from MarketPrice p2
                             where lower(p2.commodity) = lower(:commodity)
                               and p2.currency = :currency and p2.market = p.market)
        order by p.price asc
        """)
    List<MarketPrice> latestPerMarket(@Param("commodity") String commodity,
                                      @Param("currency") String currency);

    @Query("select distinct p.commodity from MarketPrice p order by p.commodity")
    List<String> commodities();
}
