package com.agripulse.backend.supplies;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SupplyOrderDtos {

    public record OrderRequest(
            @NotNull Long productId,
            @NotNull @DecimalMin("0.01") BigDecimal quantity,
            @NotBlank String deliveryMethod,
            @Size(max = 255) String deliveryAddress,
            @NotBlank String paymentMethod,
            @Size(max = 500) String note) {}

    public record OrderView(Long id, Long productId, String productName, String unit, BigDecimal quantity,
                            BigDecimal unitPrice, String currency, BigDecimal totalPrice, String status,
                            String deliveryMethod, String deliveryAddress, String paymentMethod,
                            String paymentStatus, String note, LocalDateTime createdAt,
                            String supplierName, String supplierPhone, String buyerName, String buyerPhone) {
        static OrderView of(SupplyOrder o) {
            return new OrderView(o.getId(), o.getProduct().getId(), o.getProductName(), o.getUnit(),
                    o.getQuantity(), o.getUnitPrice(), o.getCurrency(), o.getTotalPrice(), o.getStatus(),
                    o.getDeliveryMethod(), o.getDeliveryAddress(), o.getPaymentMethod(), o.getPaymentStatus(),
                    o.getNote(), o.getCreatedAt(),
                    o.getSupplier().getBusinessName(), o.getSupplier().getPhone(),
                    o.getBuyer().getFullName(), o.getBuyer().getPhone());
        }
    }
}