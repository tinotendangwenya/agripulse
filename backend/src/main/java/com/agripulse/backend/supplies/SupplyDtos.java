package com.agripulse.backend.supplies;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SupplyDtos {

    public record ProfileRequest(
            @NotBlank @Size(max = 150) String businessName,
            @NotBlank @Size(max = 60) String province,
            @Size(max = 80) String district,
            @Size(max = 255) String address,
            @NotBlank @Size(max = 20) String phone,
            @Size(max = 2000) String description,
            boolean offersDelivery) {}

    public record ProfileView(String businessName, String province, String district, String address,
                              String phone, String description, boolean offersDelivery, boolean verified) {
        static ProfileView of(SupplierProfile p) {
            return new ProfileView(p.getBusinessName(), p.getProvince(), p.getDistrict(), p.getAddress(),
                    p.getPhone(), p.getDescription(), p.isOffersDelivery(), p.isVerified());
        }
    }

    public record ProductRequest(
            @NotBlank @Size(max = 150) String name,
            @Size(max = 100) String brand,
            @NotBlank String category,
            @Size(max = 2000) String description,
            @NotBlank String unit,
            @NotNull @DecimalMin("0.01") BigDecimal price,
            @NotBlank String currency,
            @NotNull @DecimalMin("0") BigDecimal stockQty,
            @Size(max = 60) String registrationNo,
            LocalDate expiryDate) {}

    public record StockRequest(@NotNull @DecimalMin("0") BigDecimal stockQty) {}

    public record SupplierInfo(Long id, String businessName, String province, String district,
                               boolean verified, boolean offersDelivery) {}

    public record ProductView(Long id, String name, String brand, String category, String description,
                              String unit, BigDecimal price, String currency, BigDecimal stockQty,
                              boolean inStock, String registrationNo, LocalDate expiryDate,
                              String status, SupplierInfo supplier) {
        static ProductView of(SupplyProduct p) {
            SupplierProfile s = p.getSupplier();
            return new ProductView(p.getId(), p.getName(), p.getBrand(), p.getCategory(), p.getDescription(),
                    p.getUnit(), p.getPrice(), p.getCurrency(), p.getStockQty(),
                    p.getStockQty().signum() > 0, p.getRegistrationNo(), p.getExpiryDate(), p.getStatus(),
                    new SupplierInfo(s.getUserId(), s.getBusinessName(), s.getProvince(), s.getDistrict(),
                            s.isVerified(), s.isOffersDelivery()));
        }
    }
}