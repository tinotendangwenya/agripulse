package com.agripulse.backend.market;

/**
 * Request and response objects (what the API accepts and returns)
 */

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(Long id, Long productId, String productName, Long buyerId, String buyerName,
                            BigDecimal quantity, String unit, BigDecimal unitPrice, String currency,
                            BigDecimal totalPrice, String status, String note, LocalDateTime createdAt) {
    static OrderResponse of(Order o) {
        return new OrderResponse(o.getId(), o.getProduct().getId(), o.getProduct().getName(),
                o.getBuyer().getId(), o.getBuyer().getFullName(), o.getQuantity(),
                o.getProduct().getUnit(), o.getUnitPrice(), o.getCurrency(), o.getTotalPrice(),
                o.getStatus(), o.getNote(), o.getCreatedAt());
    }
}
