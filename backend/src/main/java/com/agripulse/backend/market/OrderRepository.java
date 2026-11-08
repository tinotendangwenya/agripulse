package com.agripulse.backend.market;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByBuyerIdOrderByIdDesc(Long buyerId);
    List<Order> findByProductSellerIdOrderByIdDesc(Long sellerId);
    Optional<Order> findByIdAndBuyerId(Long id, Long buyerId);
    Optional<Order> findByIdAndProductSellerId(Long id, Long sellerId);
}
