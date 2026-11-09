package com.agripulse.backend.prices;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceRequest(
        @NotBlank @Size(max = 80) String commodity,
        @NotBlank @Size(max = 100) String market,
        @NotBlank String province,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
        @NotBlank String currency,
        @NotBlank String unit,
        @NotNull @PastOrPresent LocalDate priceDate,
        @NotBlank @Size(max = 200) String source) {}
