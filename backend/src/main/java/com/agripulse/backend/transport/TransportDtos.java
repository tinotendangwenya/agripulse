package com.agripulse.backend.transport;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class TransportDtos {
    private TransportDtos() {}

    public record VehicleRequest(@NotBlank String vehicleType, @NotBlank @Size(max = 150) String description,
                                 @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal capacity,
                                 @NotBlank String capacityUnit, @NotEmpty List<String> cargoTypes,
                                 @NotBlank String baseProvince, @Size(max = 80) String baseDistrict,
                                 @NotNull @DecimalMin("0.0") BigDecimal ratePerKm,
                                 @DecimalMin("0.0") BigDecimal minCharge, @NotBlank String currency) {}

    public record VehicleView(Long id, String vehicleType, String description, BigDecimal capacityKg,
                              List<String> cargoTypes, String baseProvince, String baseDistrict,
                              BigDecimal ratePerKm, BigDecimal minCharge, String currency, String status) {}

    public record RequestCreate(@NotBlank String cargoType, @NotBlank @Size(max = 200) String description,
                                @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal weight,
                                @NotBlank String weightUnit, @NotBlank String pickupProvince,
                                @Size(max = 80) String pickupDistrict, @NotBlank String deliveryProvince,
                                @Size(max = 80) String deliveryDistrict, LocalDate neededBy,
                                @Size(max = 500) String note) {}

    public record RequestView(Long id, String requesterName, String cargoType, String description,
                              BigDecimal weightKg, String pickupProvince, String pickupDistrict,
                              String deliveryProvince, String deliveryDistrict, LocalDate neededBy, String note,
                              String status, LocalDateTime createdAt, long pendingQuotes, Double distanceKm) {}

    public record Contact(String name, String phone, String email) {}

    public record QuoteCreate(@NotNull Long vehicleId,
                              @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
                              @Size(max = 500) String message) {}

    public record QuoteView(Long id, Long requestId, String requestSummary, String transporterName, Long vehicleId,
                            String vehicleDescription, BigDecimal price, String currency, String message,
                            String status, String requestStatus, LocalDateTime createdAt, Contact contact) {}

    public record VehicleOption(Long vehicleId, String transporterName, String description, String vehicleType,
                                BigDecimal capacityKg, String baseProvince, Double distanceToPickupKm,
                                BigDecimal estimate, String currency, boolean nearest, boolean cheapest) {}

    public record RequestDetail(RequestView request, List<QuoteView> quotes, List<VehicleOption> vehicles,
                                Contact transporter) {}

    public record VehicleFit(Long vehicleId, String description, BigDecimal estimate, String currency) {}

    public record Job(RequestView request, List<VehicleFit> myVehicles, boolean alreadyQuoted) {}

    public record StatusRequest(@NotBlank String status) {}
}
