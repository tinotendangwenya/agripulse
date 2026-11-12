package com.agripulse.backend.supplies;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface SupplyProductRepository extends JpaRepository<SupplyProduct, Long> {

    @Query("""
            select p from SupplyProduct p join fetch p.supplier s
            where p.status = 'ACTIVE'
              and (:q = '' or lower(p.name) like lower(concat('%', :q, '%'))
                   or lower(coalesce(p.brand, '')) like lower(concat('%', :q, '%')))
              and (:category = '' or p.category = :category)
              and (:province = '' or s.province = :province)
              and (:currency = '' or p.currency = :currency)
              and (:deliveryOnly = false or s.offersDelivery = true)
              and p.price <= :maxPrice
            """)
    List<SupplyProduct> search(@Param("q") String q,
                               @Param("category") String category,
                               @Param("province") String province,
                               @Param("currency") String currency,
                               @Param("deliveryOnly") boolean deliveryOnly,
                               @Param("maxPrice") BigDecimal maxPrice,
                               Pageable pageable);

    List<SupplyProduct> findBySupplier_UserIdAndStatusNotOrderByIdDesc(Long userId, String status);

    Optional<SupplyProduct> findByIdAndSupplier_UserId(Long id, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from SupplyProduct p where p.id = :id")
    Optional<SupplyProduct> lockById(@Param("id") Long id);
}