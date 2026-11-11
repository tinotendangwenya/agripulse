package com.agripulse.backend.equipment;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    List<Equipment> findByOwnerIdAndStatusNotOrderByIdDesc(Long ownerId, String status);
    long countByOwnerIdAndStatus(Long ownerId, String status);

    @Query("""
        select e from Equipment e
        where e.status = 'ACTIVE' and e.ownerId <> :caller
          and (:category = '' or e.category = :category)
          and (:province = '' or e.province = :province)
          and (:currency = '' or e.currency = :currency)
          and e.dailyRate <= :maxRate
        order by e.id desc
        """)
    List<Equipment> search(@Param("category") String category, @Param("province") String province,
                           @Param("currency") String currency, @Param("maxRate") BigDecimal maxRate,
                           @Param("caller") Long caller, Pageable pageable);

    // Equipment is always locked FIRST, so confirming, blocking and removing take turns instead of racing
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Equipment e where e.id = :id")
    Optional<Equipment> findForUpdate(@Param("id") Long id);
}
