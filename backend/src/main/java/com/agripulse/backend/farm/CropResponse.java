package com.agripulse.backend.farm;

/**
 * Request and response objects (what the API accepts and returns)
 */

import java.math.BigDecimal;
import java.time.LocalDate;

public record CropResponse(Long id, Long farmId, String cropName, String variety,
                           BigDecimal areaHectares, LocalDate plantingDate,
                           LocalDate expectedHarvestDate, String status, String notes) {
    static CropResponse of(CropRecord c) {
        return new CropResponse(c.getId(), c.getFarm().getId(), c.getCropName(), c.getVariety(),
                c.getAreaHectares(), c.getPlantingDate(), c.getExpectedHarvestDate(),
                c.getStatus(), c.getNotes());
    }
}
