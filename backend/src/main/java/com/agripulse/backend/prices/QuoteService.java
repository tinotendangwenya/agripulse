package com.agripulse.backend.prices;

import com.agripulse.backend.notifications.NotificationService;
import com.agripulse.backend.prices.PriceDtos.*;
import com.agripulse.backend.user.User;
import com.agripulse.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

/**
 * Price quotes from users. A quote becomes a public price ONLY when an admin approves it.
 * The public source line names the role ("a registered buyer"), never the person.
 */
@Service
@RequiredArgsConstructor
public class QuoteService {
    private static final ZoneId HARARE = ZoneId.of("Africa/Harare");
    private static final int MAX_PENDING = 10;
    private static final Set<String> FILTERS = Set.of("PENDING", "APPROVED", "REJECTED", "ALL");

    private final PriceQuoteRepository quotes;
    private final MarketPriceRepository prices;
    private final PriceAlertService alerts;
    private final UserRepository users;
    private final NotificationService notifier;

    @Transactional
    public QuoteView submit(Long userId, QuoteRequest r) {
        String cur = up(r.currency()), unit = up(r.unit());
        if (!PriceService.CURRENCIES.contains(cur)) throw bad("Currency must be one of " + PriceService.CURRENCIES);
        if (!PriceService.UNITS.contains(unit)) throw bad("Unit must be one of " + PriceService.UNITS);
        String province = PriceService.PROVINCES.stream().filter(x -> x.equalsIgnoreCase(r.province().trim()))
                .findFirst().orElseThrow(() -> bad("Unknown province"));
        LocalDate today = LocalDate.now(HARARE);
        if (r.priceDate().isAfter(today)) throw bad("The price date cannot be in the future");
        if (r.priceDate().isBefore(today.minusDays(30))) throw bad("Quotes must be for prices seen in the last 30 days");
        if (quotes.countBySubmitterIdAndStatus(userId, "PENDING") >= MAX_PENDING)
            throw bad("You already have " + MAX_PENDING + " quotes waiting for review. Please wait for those first.");

        PriceQuote q = new PriceQuote();
        q.setSubmitterId(userId);
        q.setCommodity(r.commodity().trim());
        q.setMarket(r.market().trim());
        q.setProvince(province);
        q.setPrice(r.price());
        q.setCurrency(cur);
        q.setUnit(unit);
        q.setPriceDate(r.priceDate());
        q.setNote(r.note());
        quotes.save(q);
        for (User admin : users.findByRoleNameAndEnabledTrue("ADMIN"))
            notifier.send(admin.getId(), "PRICE", "New price quote to review",
                    q.getCommodity() + " at " + q.getMarket() + ": " + q.getPrice() + " " + cur + " per " + unit + ".", "prices");
        return view(q);
    }

    @Transactional(readOnly = true)
    public List<QuoteView> mine(Long userId) {
        return quotes.findTop100BySubmitterIdOrderByIdDesc(userId).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public List<QuoteView> list(String status) {
        String s = status == null || status.isBlank() ? "PENDING" : status.trim().toUpperCase();
        if (!FILTERS.contains(s)) throw bad("Status must be one of " + FILTERS);
        List<PriceQuote> list = s.equals("ALL") ? quotes.findTop100ByOrderByIdDesc() : quotes.findTop100ByStatusOrderByIdAsc(s);
        return list.stream().map(this::view).toList();
    }

    @Transactional
    public QuoteView approve(Long adminId, Long id) {
        PriceQuote q = pending(id);
        String role = users.findById(q.getSubmitterId()).map(u -> u.getRole().getName().toLowerCase()).orElse("user");

        MarketPrice p = new MarketPrice();
        p.setCommodity(q.getCommodity());
        p.setMarket(q.getMarket());
        p.setProvince(q.getProvince());
        p.setPrice(q.getPrice());
        p.setCurrency(q.getCurrency());
        p.setUnit(q.getUnit());
        p.setPriceDate(q.getPriceDate());
        p.setSource("Quote from a registered " + role + ", approved by admin");
        p.setEnteredBy(adminId);
        MarketPrice saved = prices.save(p);

        q.setStatus("APPROVED");
        q.setReviewedBy(adminId);
        q.setReviewedAt(LocalDateTime.now());
        q.setMarketPriceId(saved.getId());
        alerts.onNewPrice(saved);
        notifier.send(q.getSubmitterId(), "PRICE", "Your price quote was approved",
                q.getCommodity() + " at " + q.getMarket() + " is now on the price board.", "prices");
        return view(q);
    }

    @Transactional
    public QuoteView reject(Long adminId, Long id, String note) {
        PriceQuote q = pending(id);
        q.setStatus("REJECTED");
        q.setReviewedBy(adminId);
        q.setReviewedAt(LocalDateTime.now());
        q.setReviewNote(note == null || note.isBlank() ? null : note.trim());
        notifier.send(q.getSubmitterId(), "PRICE", "Your price quote was not accepted",
                q.getCommodity() + " at " + q.getMarket() + (q.getReviewNote() == null ? "." : ": " + q.getReviewNote()), "prices");
        return view(q);
    }

    private PriceQuote pending(Long id) {
        PriceQuote q = quotes.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
        if (!"PENDING".equals(q.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This quote is already " + q.getStatus());
        return q;
    }

    private QuoteView view(PriceQuote q) {
        User u = users.findById(q.getSubmitterId()).orElse(null);
        return new QuoteView(q.getId(), u == null ? "User" : u.getFullName(), u == null ? "" : u.getRole().getName(),
                q.getCommodity(), q.getMarket(), q.getProvince(), q.getPrice(), q.getCurrency(), q.getUnit(),
                q.getPriceDate(), q.getNote(), q.getStatus(), q.getReviewNote(), q.getCreatedAt());
    }

    private static String up(String s) { return s == null ? "" : s.trim().toUpperCase(); }
    private static ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
}
