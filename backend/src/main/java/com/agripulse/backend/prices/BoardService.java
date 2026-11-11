package com.agripulse.backend.prices;

import com.agripulse.backend.prices.PriceDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Price board: the latest price per commodity, market, currency and unit, with the change since the previous price.
 * Only recorded prices are shown. Nothing is estimated, and currencies and units are never converted.
 */
@Service
@RequiredArgsConstructor
public class BoardService {
    private static final ZoneId HARARE = ZoneId.of("Africa/Harare");
    private static final int WINDOW_DAYS = 120;
    private static final int STALE_DAYS = 14;

    private final PriceBoardRepository repo;

    @Transactional(readOnly = true)
    public List<BoardRow> board(String commodity, String province) {
        LocalDate today = LocalDate.now(HARARE);
        String c = blank(commodity) ? "" : commodity.trim().toLowerCase();
        String prov = blank(province) ? "" : PriceService.PROVINCES.stream()
                .filter(x -> x.equalsIgnoreCase(province.trim())).findFirst()
                .orElseThrow(() -> bad("Unknown province"));

        Map<String, List<MarketPrice>> groups = new LinkedHashMap<>();
        for (MarketPrice p : repo.findTop5000ByPriceDateGreaterThanEqualOrderByPriceDateDescIdDesc(today.minusDays(WINDOW_DAYS))) {
            if (!c.isEmpty() && !p.getCommodity().toLowerCase().contains(c)) continue;
            if (!prov.isEmpty() && !p.getProvince().equals(prov)) continue;
            String key = (p.getCommodity() + "|" + p.getMarket() + "|" + p.getCurrency() + "|" + p.getUnit()).toLowerCase();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
        }

        List<BoardRow> rows = new ArrayList<>();
        for (List<MarketPrice> g : groups.values()) {
            MarketPrice latest = g.get(0);                       // newest date, then newest entry
            MarketPrice prev = null;
            for (MarketPrice p : g) {
                if (p.getPriceDate().isBefore(latest.getPriceDate())) { prev = p; break; }
            }
            BigDecimal change = null, pct = null;
            if (prev != null) {
                change = latest.getPrice().subtract(prev.getPrice()).setScale(2, RoundingMode.HALF_UP);
                if (prev.getPrice().signum() > 0)
                    pct = change.multiply(BigDecimal.valueOf(100)).divide(prev.getPrice(), 1, RoundingMode.HALF_UP);
            }
            long age = Math.max(0, ChronoUnit.DAYS.between(latest.getPriceDate(), today));
            rows.add(new BoardRow(latest.getCommodity(), latest.getMarket(), latest.getProvince(), latest.getCurrency(),
                    latest.getUnit(), latest.getPrice(), latest.getPriceDate(), latest.getSource(),
                    prev == null ? null : prev.getPrice(), prev == null ? null : prev.getPriceDate(),
                    change, pct, age, age > STALE_DAYS));
        }
        rows.sort(Comparator.comparing(BoardRow::commodity, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(BoardRow::currency).thenComparing(BoardRow::unit).thenComparing(BoardRow::price));
        return rows.stream().limit(300).toList();
    }

    @Transactional(readOnly = true)
    public List<SeriesPoint> series(String commodity, String market, String currency, String unit, int days) {
        String cur = currency == null ? "" : currency.trim().toUpperCase();
        String u = unit == null ? "" : unit.trim().toUpperCase();
        if (!PriceService.CURRENCIES.contains(cur)) throw bad("Currency must be one of " + PriceService.CURRENCIES);
        if (!PriceService.UNITS.contains(u)) throw bad("Unit must be one of " + PriceService.UNITS);
        LocalDate from = LocalDate.now(HARARE).minusDays(Math.min(Math.max(days, 1), 730));
        Map<LocalDate, BigDecimal> byDay = new TreeMap<>();       // one point per day: the last one entered
        for (MarketPrice p : repo.series(commodity.trim(), market.trim(), cur, u, from, PageRequest.of(0, 2000)))
            byDay.put(p.getPriceDate(), p.getPrice());
        return byDay.entrySet().stream().map(e -> new SeriesPoint(e.getKey(), e.getValue())).toList();
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }
    private static ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
}
