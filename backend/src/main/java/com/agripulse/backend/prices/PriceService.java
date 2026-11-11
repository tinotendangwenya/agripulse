package com.agripulse.backend.prices;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** Admin-entered prices only. Nothing is scraped or estimated. */
@Service
@RequiredArgsConstructor
public class PriceService {
    static final Set<String> CURRENCIES = Set.of("USD", "ZIG", "ZAR");
    static final Set<String> UNITS = Set.of("KG", "TONNE", "BAG", "LITRE", "CRATE", "HEAD", "DOZEN");
    static final Set<String> PROVINCES = Set.of(
            "Harare", "Bulawayo", "Manicaland", "Mashonaland Central", "Mashonaland East",
            "Mashonaland West", "Masvingo", "Matabeleland North", "Matabeleland South", "Midlands");

    private final MarketPriceRepository prices;
    private final PriceAlertService priceAlerts;

    @Transactional
    public PriceResponse add(Long adminId, PriceRequest r) {
        MarketPrice p = new MarketPrice();
        p.setEnteredBy(adminId);
        apply(p, r);
        MarketPrice saved = prices.save(p);
        priceAlerts.onNewPrice(saved);
        return PriceResponse.of(saved);
    }

    @Transactional
    public PriceResponse update(Long id, PriceRequest r) {
        MarketPrice p = prices.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
        apply(p, r);
        return PriceResponse.of(p);
    }

    @Transactional
    public void delete(Long id) {
        if (!prices.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
        prices.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<String> commodities() {
        return prices.commodities();
    }

    /** Latest price in each market, cheapest first. Currency is required so we never mix currencies. */
    @Transactional(readOnly = true)
    public List<PriceResponse> compare(String commodity, String currency) {
        String cur = currency.trim().toUpperCase();
        if (!CURRENCIES.contains(cur)) throw bad("Currency must be one of " + CURRENCIES);
        return prices.latestPerMarket(commodity.trim(), cur).stream().map(PriceResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public List<PriceResponse> history(String commodity, int days) {
        LocalDate from = LocalDate.now().minusDays(Math.min(Math.max(days, 1), 730));
        return prices.findByCommodityIgnoreCaseAndPriceDateGreaterThanEqualOrderByPriceDateAsc(
                commodity.trim(), from).stream().map(PriceResponse::of).toList();
    }

    private void apply(MarketPrice p, PriceRequest r) {
        String cur = r.currency().trim().toUpperCase();
        String unit = r.unit().trim().toUpperCase();
        if (!CURRENCIES.contains(cur)) throw bad("Currency must be one of " + CURRENCIES);
        if (!UNITS.contains(unit)) throw bad("Unit must be one of " + UNITS);
        String province = PROVINCES.stream().filter(x -> x.equalsIgnoreCase(r.province().trim()))
                .findFirst().orElseThrow(() -> bad("Province must be one of " + PROVINCES));
        p.setCommodity(r.commodity().trim());
        p.setMarket(r.market().trim());
        p.setProvince(province);
        p.setPrice(r.price());
        p.setCurrency(cur);
        p.setUnit(unit);
        p.setPriceDate(r.priceDate());
        p.setSource(r.source().trim());
    }

    private static ResponseStatusException bad(String m) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, m);
    }
}
