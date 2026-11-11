package com.agripulse.backend.prices;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PriceQuoteRepository extends JpaRepository<PriceQuote, Long> {
    List<PriceQuote> findTop100BySubmitterIdOrderByIdDesc(Long submitterId);
    List<PriceQuote> findTop100ByStatusOrderByIdAsc(String status);
    List<PriceQuote> findTop100ByOrderByIdDesc();
    long countBySubmitterIdAndStatus(Long submitterId, String status);
}
