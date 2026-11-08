package com.agripulse.backend.market;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductResponse(Long id, Long sellerId, String sellerName, String name, String category,
                              String description, BigDecimal quantity, String unit, BigDecimal price,
                              String currency, String province, String district,
                              LocalDate availableFrom, String status) {
    static ProductResponse of(Product p) {
        return new ProductResponse(p.getId(), p.getSeller().getId(), p.getSeller().getFullName(),
                p.getName(), p.getCategory(), p.getDescription(), p.getQuantity(), p.getUnit(),
                p.getPrice(), p.getCurrency(), p.getProvince(), p.getDistrict(),
                p.getAvailableFrom(), p.getStatus());
    }
}
