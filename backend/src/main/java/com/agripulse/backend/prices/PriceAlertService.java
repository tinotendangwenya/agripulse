package com.agripulse.backend.prices;

import com.agripulse.backend.notifications.NotificationService;
import com.agripulse.backend.prices.PriceDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Price alerts. An alert fires once, when a NEW price is recorded that is above or below the limit.
 * Same commodity, currency and unit only. Prices dated more than 7 days ago never fire alerts.
 */
@Service
@RequiredArgsConstructor
public class PriceAlertService {
    private static final ZoneId HARARE = ZoneId.of("Africa/Harare");
    private static final int MAX_ACTIVE = 20;

    private final PriceAlertRepository alerts;
    private final NotificationService notifier;

    @Transactional(readOnly = true)
    public List<AlertView> mine(Long userId) {
        return alerts.findTop100ByUserIdOrderByIdDesc(userId).stream().map(this::view).toList();
    }

    @Transactional
    public AlertView create(Long userId, AlertRequest r) {
        String cur = up(r.currency()), unit = up(r.unit()), dir = up(r.direction());
        if (!PriceService.CURRENCIES.contains(cur)) throw bad("Currency must be one of " + PriceService.CURRENCIES);
        if (!PriceService.UNITS.contains(unit)) throw bad("Unit must be one of " + PriceService.UNITS);
        if (!dir.equals("ABOVE") && !dir.equals("BELOW")) throw bad("Direction must be ABOVE or BELOW");
        if (alerts.countByUserIdAndStatus(userId, "ACTIVE") >= MAX_ACTIVE)
            throw bad("You already have " + MAX_ACTIVE + " active alerts. Delete some first.");

        PriceAlert a = new PriceAlert();
        a.setUserId(userId);
        a.setCommodity(r.commodity().trim());
        a.setMarket(r.market() == null || r.market().isBlank() ? null : r.market().trim());
        a.setCurrency(cur);
        a.setUnit(unit);
        a.setDirection(dir);
        a.setThreshold(r.threshold());
        return view(alerts.save(a));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        alerts.delete(alerts.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found")));
    }

    /** Called inside the same transaction that saves a new price, so a rolled-back price sends no alert. */
    @Transactional
    public void onNewPrice(MarketPrice p) {
        if (p.getPriceDate().isBefore(LocalDate.now(HARARE).minusDays(7))) return;
        for (PriceAlert a : alerts.findMatching(p.getCommodity(), p.getCurrency(), p.getUnit(), p.getMarket())) {
            int cmp = p.getPrice().compareTo(a.getThreshold());
            boolean hit = a.getDirection().equals("ABOVE") ? cmp > 0 : cmp < 0;
            if (!hit) continue;
            a.setStatus("TRIGGERED");
            a.setTriggeredAt(LocalDateTime.now());
            a.setTriggeredPrice(p.getPrice());
            notifier.send(a.getUserId(), "PRICE", "Price alert: " + p.getCommodity(),
                    p.getCommodity() + " at " + p.getMarket() + " is now " + p.getPrice() + " " + p.getCurrency()
                            + " per " + p.getUnit() + ", " + (a.getDirection().equals("ABOVE") ? "above" : "below")
                            + " your limit of " + a.getThreshold() + ".", "prices");
        }
    }

    private AlertView view(PriceAlert a) {
        return new AlertView(a.getId(), a.getCommodity(), a.getMarket(), a.getCurrency(), a.getUnit(), a.getDirection(),
                a.getThreshold(), a.getStatus(), a.getTriggeredAt(), a.getTriggeredPrice(), a.getCreatedAt());
    }

    private static String up(String s) { return s == null ? "" : s.trim().toUpperCase(); }
    private static ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
}
