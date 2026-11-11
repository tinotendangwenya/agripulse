package com.agripulse.backend.prices;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class PriceDtos {
    private PriceDtos() {}

    public record BoardRow(String commodity, String market, String province, String currency, String unit,
                           BigDecimal price, LocalDate priceDate, String source,
                           BigDecimal previousPrice, LocalDate previousDate,
                           BigDecimal change, BigDecimal changePercent, long daysOld, boolean stale) {}

    public record SeriesPoint(LocalDate date, BigDecimal price) {}

    public record AlertRequest(@NotBlank @Size(max = 80) String commodity, @Size(max = 100) String market,
                               @NotBlank String currency, @NotBlank String unit, @NotBlank String direction,
                               @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal threshold) {}

    public record AlertView(Long id, String commodity, String market, String currency, String unit,
                            String direction, BigDecimal threshold, String status,
                            LocalDateTime triggeredAt, BigDecimal triggeredPrice, LocalDateTime createdAt) {}

    public record QuoteRequest(@NotBlank @Size(max = 80) String commodity, @NotBlank @Size(max = 100) String market,
                               @NotBlank String province,
                               @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
                               @NotBlank String currency, @NotBlank String unit, @NotNull LocalDate priceDate,
                               @Size(max = 300) String note) {}

    public record QuoteView(Long id, String submitterName, String submitterRole, String commodity, String market,
                            String province, BigDecimal price, String currency, String unit, LocalDate priceDate,
                            String note, String status, String reviewNote, LocalDateTime createdAt) {}

    public record NoteRequest(@Size(max = 300) String note) {}
}
