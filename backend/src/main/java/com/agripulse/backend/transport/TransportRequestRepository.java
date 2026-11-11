package com.agripulse.backend.transport;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransportRequestRepository extends JpaRepository<TransportRequest, Long> {
    List<TransportRequest> findTop100ByRequesterIdOrderByIdDesc(Long requesterId);
    Optional<TransportRequest> findByIdAndRequesterId(Long id, Long requesterId);
    long countByRequesterIdAndStatus(Long requesterId, String status);

    @Query("""
        select r from TransportRequest r
        where r.status = 'OPEN' and (r.neededBy is null or r.neededBy >= :today)
        order by r.id desc
        """)
    List<TransportRequest> findOpen(@Param("today") LocalDate today, Pageable pageable);

    // Row lock, so two clicks cannot both book or change the same job
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from TransportRequest r where r.id = :id")
    Optional<TransportRequest> findForUpdate(@Param("id") Long id);
}
