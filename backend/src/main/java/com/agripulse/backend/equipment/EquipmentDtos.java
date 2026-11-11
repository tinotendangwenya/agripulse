package com.agripulse.backend.equipment;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class EquipmentDtos {
    private EquipmentDtos() {}

    public record EquipmentRequest(@NotBlank String category, @NotBlank @Size(max = 150) String name,
                                   @Size(max = 500) String description, @NotBlank String province,
                                   @Size(max = 80) String district,
                                   @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal dailyRate,
                                   @NotBlank String currency, Boolean operatorIncluded, Boolean fuelIncluded,
                                   @DecimalMin("0.0") BigDecimal deposit) {}

    public record EquipmentView(Long id, String ownerName, String category, String name, String description,
                                String province, String district, BigDecimal dailyRate, String currency,
                                boolean operatorIncluded, boolean fuelIncluded, BigDecimal deposit, String status) {}

    public record BusyRange(LocalDate from, LocalDate to, String kind) {}

    public record EquipmentOption(EquipmentView equipment, Double distanceKm, Boolean available, boolean nearest,
                                  boolean cheapest, BigDecimal estimatedTotal, List<BusyRange> busy) {}

    public record BlockRequest(@NotNull LocalDate startDate, @NotNull LocalDate endDate,
                               @Size(max = 200) String reason) {}

    public record BlockView(Long id, LocalDate startDate, LocalDate endDate, String reason) {}

    public record MyEquipment(EquipmentView equipment, List<BlockView> blocks) {}

    public record BookingRequest(@NotNull Long equipmentId, @NotNull LocalDate startDate,
                                 @NotNull LocalDate endDate, @Size(max = 500) String note) {}

    public record Contact(String name, String phone, String email) {}

    public record BookingView(Long id, Long equipmentId, String equipmentName, String category, String counterpartName,
                              LocalDate startDate, LocalDate endDate, int days, BigDecimal dailyRate, String currency,
                              BigDecimal totalPrice, BigDecimal deposit, String note, String status,
                              LocalDateTime createdAt, Contact contact) {}
}
