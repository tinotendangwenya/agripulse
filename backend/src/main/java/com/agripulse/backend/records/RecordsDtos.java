package com.agripulse.backend.records;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class RecordsDtos {
    private RecordsDtos() {}

    public record TransactionRequest(@NotBlank String type, @NotBlank String category,
                                     @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount,
                                     @NotBlank String currency, @NotNull LocalDate txDate,
                                     @Size(max = 300) String description, Long cropRecordId) {}

    public record TransactionView(Long id, String type, String category, BigDecimal amount, String currency,
                                  LocalDate txDate, String description, Long cropRecordId) {}

    public record HarvestRequest(@NotNull Long cropRecordId, @NotNull LocalDate harvestDate,
                                 @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantity,
                                 @NotBlank String unit, @Size(max = 300) String note) {}

    public record HarvestView(Long id, Long cropRecordId, String cropName, LocalDate harvestDate,
                              BigDecimal quantityKg, String note) {}

    public record LivestockRequest(@NotBlank String animalType, @NotBlank String eventType,
                                   @NotNull LocalDate eventDate, @NotNull @Min(1) Integer headCount,
                                   @Size(max = 300) String note) {}

    public record LivestockView(Long id, String animalType, String eventType, LocalDate eventDate,
                                Integer headCount, String note) {}

    public record CategoryTotal(String category, BigDecimal amount) {}

    public record CurrencyTotals(String currency, BigDecimal income, BigDecimal expenses, BigDecimal profit,
                                 List<CategoryTotal> expensesByCategory) {}

    public record CurrencyAmount(String currency, BigDecimal income, BigDecimal expenses) {}

    public record CropSummary(Long cropRecordId, String cropName, BigDecimal areaHectares, BigDecimal harvestedKg,
                              BigDecimal yieldTonnesPerHa, List<CurrencyAmount> linkedMoney) {}

    public record HerdCount(String animalType, long head) {}

    public record Period(LocalDate from, LocalDate to) {}

    public record Summary(Period period, List<CurrencyTotals> money, List<CropSummary> crops, List<HerdCount> herd,
                          Period previousPeriod, List<CurrencyTotals> previousMoney, List<String> notes) {}
}
