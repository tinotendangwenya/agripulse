package com.agripulse.backend.supplies;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SupplyOrderRepository extends JpaRepository<SupplyOrder, Long> {

    List<SupplyOrder> findByBuyer_IdOrderByIdDesc(Long buyerId);

    List<SupplyOrder> findBySupplier_UserIdOrderByIdDesc(Long supplierId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from SupplyOrder o where o.id = :id and o.buyer.id = :buyerId")
    Optional<SupplyOrder> lockForBuyer(@Param("id") Long id, @Param("buyerId") Long buyerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from SupplyOrder o where o.id = :id and o.supplier.userId = :supplierId")
    Optional<SupplyOrder> lockForSupplier(@Param("id") Long id, @Param("supplierId") Long supplierId);
}