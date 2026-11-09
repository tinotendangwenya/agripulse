package com.agripulse.backend.market;

/**
 * Request and response objects (what the API accepts and returns)
 */

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record OrderRequest(
        @NotNull Long productId,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantity,
        @Size(max = 500) String note) {}
