package com.agripulse.backend.requests;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PurchaseOfferRepository extends JpaRepository<PurchaseOffer, Long> {
    List<PurchaseOffer> findByRequestIdOrderByIdDesc(Long requestId);
    List<PurchaseOffer> findTop100ByFarmerIdOrderByIdDesc(Long farmerId);
    Optional<PurchaseOffer> findByIdAndRequestId(Long id, Long requestId);
    Optional<PurchaseOffer> findByIdAndFarmerId(Long id, Long farmerId);
    boolean existsByRequestIdAndFarmerIdAndStatus(Long requestId, Long farmerId, String status);
    long countByRequestIdAndStatus(Long requestId, String status);
}
