package com.agripulse.backend.market;

/**
 * Entities and repositories (database mapping and queries)
 */

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("""
        select p from Product p
        where p.status = 'ACTIVE' and p.quantity > 0
          and lower(p.name) like lower(concat('%', :q, '%'))
          and (:province = '' or p.province = :province)
          and (:currency = '' or p.currency = :currency)
          and p.price <= :maxPrice
        order by p.id desc
        """)
    List<Product> search(@Param("q") String q, @Param("province") String province,
                         @Param("currency") String currency, @Param("maxPrice") BigDecimal maxPrice,
                         Pageable pageable);

    List<Product> findBySellerIdAndStatusNotOrderByIdDesc(Long sellerId, String status);

    Optional<Product> findByIdAndSellerId(Long id, Long sellerId);

    // Row lock so two confirmations can't both take the last stock
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findForUpdate(@Param("id") Long id);
}
