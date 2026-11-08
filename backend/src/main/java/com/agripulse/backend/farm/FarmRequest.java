package com.agripulse.backend.farm;

/**
 * Request and response objects (what the API accepts and returns)
 */

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record FarmRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank String province,
        @Size(max = 80) String district,
        @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal sizeHectares) {}
