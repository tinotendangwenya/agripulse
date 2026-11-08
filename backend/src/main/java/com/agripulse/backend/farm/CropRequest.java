package com.agripulse.backend.farm;

/**
 * Request and response objects (what the API accepts and returns)
 */

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CropRequest(
        @NotBlank @Size(max = 80) String cropName,
        @Size(max = 80) String variety,
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal areaHectares,
        LocalDate plantingDate,
        LocalDate expectedHarvestDate,
        String status,
        @Size(max = 2000) String notes) {}
