package com.agripulse.backend.market;

/**
 * Request and response objects (what the API accepts and returns)
 */

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank String category,
        @Size(max = 2000) String description,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantity,
        @NotBlank String unit,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
        @NotBlank String currency,
        @NotBlank String province,
        @Size(max = 80) String district,
        LocalDate availableFrom) {}
