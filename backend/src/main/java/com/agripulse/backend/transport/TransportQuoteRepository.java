package com.agripulse.backend.transport;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TransportQuoteRepository extends JpaRepository<TransportQuote, Long> {
    List<TransportQuote> findByRequestIdOrderByIdDesc(Long requestId);
    List<TransportQuote> findTop100ByTransporterIdOrderByIdDesc(Long transporterId);
    Optional<TransportQuote> findByIdAndRequestId(Long id, Long requestId);
    Optional<TransportQuote> findByIdAndTransporterId(Long id, Long transporterId);
    boolean existsByRequestIdAndTransporterIdAndStatus(Long requestId, Long transporterId, String status);
    long countByRequestIdAndStatus(Long requestId, String status);
}
