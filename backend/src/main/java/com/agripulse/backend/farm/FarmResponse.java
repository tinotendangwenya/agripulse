package com.agripulse.backend.farm;

/**
 * Request and response objects (what the API accepts and returns)
 */

import java.math.BigDecimal;

public record FarmResponse(Long id, String name, String province, String district,
                           BigDecimal latitude, BigDecimal longitude, BigDecimal sizeHectares) {
    static FarmResponse of(Farm f) {
        return new FarmResponse(f.getId(), f.getName(), f.getProvince(), f.getDistrict(),
                f.getLatitude(), f.getLongitude(), f.getSizeHectares());
    }
}
