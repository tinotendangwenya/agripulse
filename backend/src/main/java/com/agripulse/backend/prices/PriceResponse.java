package com.agripulse.backend.prices;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceResponse(Long id, String commodity, String market, String province,
                            BigDecimal price, String currency, String unit,
                            LocalDate priceDate, String source) {
    static PriceResponse of(MarketPrice p) {
        return new PriceResponse(p.getId(), p.getCommodity(), p.getMarket(), p.getProvince(),
                p.getPrice(), p.getCurrency(), p.getUnit(), p.getPriceDate(), p.getSource());
    }
}
