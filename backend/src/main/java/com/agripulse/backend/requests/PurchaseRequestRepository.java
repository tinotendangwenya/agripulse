package com.agripulse.backend.requests;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {
    List<PurchaseRequest> findTop100ByBuyerIdOrderByIdDesc(Long buyerId);
    Optional<PurchaseRequest> findByIdAndBuyerId(Long id, Long buyerId);
    long countByBuyerIdAndStatus(Long buyerId, String status);

    @Query("""
        select r from PurchaseRequest r
        where r.status = 'OPEN' and (r.neededBy is null or r.neededBy >= :today)
          and (:q = '' or lower(r.productName) like lower(concat('%', :q, '%')))
          and (:province = '' or r.province = :province)
        order by r.id desc
        """)
    List<PurchaseRequest> findOpen(@Param("q") String q, @Param("province") String province,
                                   @Param("today") LocalDate today, Pageable pageable);
}
