package com.agripulse.backend.requests;

import com.agripulse.backend.market.Order;
import com.agripulse.backend.market.OrderRepository;
import com.agripulse.backend.market.Product;
import com.agripulse.backend.market.ProductRepository;
import com.agripulse.backend.requests.RequestsDtos.*;
import com.agripulse.backend.user.User;
import com.agripulse.backend.user.UserRepository;
import com.agripulse.backend.weather.ProvinceLocations;
import lombok.RequiredArgsConstructor;
import com.agripulse.backend.notifications.NotificationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Buyer purchase requests and farmer offers. Matching is rule-based (name, unit, currency, price), not AI.
 * Currencies are never converted and units are never converted. Distances are approximate (province centres).
 */
@Service
@RequiredArgsConstructor
public class RequestService {
    static final Set<String> CATEGORIES = Set.of("CROP", "LIVESTOCK", "POULTRY", "FISH", "OTHER");
    static final Set<String> UNITS = Set.of("KG", "TONNE", "BAG", "LITRE", "CRATE", "HEAD", "DOZEN");
    static final Set<String> CURRENCIES = Set.of("USD", "ZIG", "ZAR");
    private static final int MAX_OPEN_PER_BUYER = 20;
    private static final BigDecimal NO_LIMIT = new BigDecimal("999999999999");
    private static final ZoneId HARARE = ZoneId.of("Africa/Harare");

    private record Cand(Product p, Double dist, boolean covers) {}

    private final PurchaseRequestRepository requests;
    private final PurchaseOfferRepository offers;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final UserRepository users;
    private final NotificationService notifier;

    // ---------- buyer ----------
    @Transactional
    public RequestView create(Long buyerId, RequestCreate r) {
        String category = up(r.category()), unit = up(r.unit()), currency = up(r.currency());
        if (!CATEGORIES.contains(category)) throw bad("Category must be one of " + CATEGORIES);
        if (!UNITS.contains(unit)) throw bad("Unit must be one of " + UNITS);
        if (!CURRENCIES.contains(currency)) throw bad("Currency must be one of " + CURRENCIES);
        String province = ProvinceLocations.CENTRES.keySet().stream()
                .filter(p -> p.equalsIgnoreCase(r.province().trim())).findFirst()
                .orElseThrow(() -> bad("Unknown province"));
        if (r.neededBy() != null && r.neededBy().isBefore(today())) throw bad("The needed-by date is in the past");
        if (requests.countByBuyerIdAndStatus(buyerId, "OPEN") >= MAX_OPEN_PER_BUYER)
            throw bad("You already have " + MAX_OPEN_PER_BUYER + " open requests. Close some first.");

        PurchaseRequest p = new PurchaseRequest();
        p.setBuyerId(buyerId);
        p.setProductName(r.productName().trim());
        p.setCategory(category);
        p.setQuantity(r.quantity());
        p.setUnit(unit);
        p.setMaxPrice(r.maxPrice());
        p.setCurrency(currency);
        p.setProvince(province);
        p.setDistrict(r.district());
        p.setNeededBy(r.neededBy());
        p.setNote(r.note());
        p.setUpdatedAt(LocalDateTime.now());
        PurchaseRequest saved = requests.save(p);
        Set<Long> sellers = new LinkedHashSet<>();
        BigDecimal cap = saved.getMaxPrice() == null ? NO_LIMIT : saved.getMaxPrice();
        for (Product pr : products.search(saved.getProductName(), "", saved.getCurrency(), cap, PageRequest.of(0, 50)))
            if (pr.getUnit().equals(saved.getUnit())) sellers.add(pr.getSeller().getId());
        for (Long sid : sellers)
            notifier.send(sid, "REQUEST", "A buyer wants " + saved.getProductName(),
                    saved.getQuantity() + " " + saved.getUnit() + " wanted in " + saved.getProvince()
                            + ". It matches one of your listings.", "requests");
        return view(saved);
    }

    @Transactional(readOnly = true)
    public List<RequestView> mine(Long buyerId) {
        return requests.findTop100ByBuyerIdOrderByIdDesc(buyerId).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public RequestDetail detail(Long buyerId, Long id) {
        PurchaseRequest r = requests.findByIdAndBuyerId(id, buyerId).orElseThrow(this::notFound);
        List<OfferView> list = offers.findByRequestIdOrderByIdDesc(id).stream().map(this::offerView).toList();
        boolean open = "OPEN".equals(r.getStatus()) && !expired(r);
        return new RequestDetail(view(r), list, open ? listingsFor(r) : List.of());
    }

    @Transactional
    public RequestView close(Long buyerId, Long id) {
        PurchaseRequest r = requests.findByIdAndBuyerId(id, buyerId).orElseThrow(this::notFound);
        if (!"OPEN".equals(r.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "This request is already " + r.getStatus());
        r.setStatus("CLOSED");
        r.setUpdatedAt(LocalDateTime.now());
        for (PurchaseOffer o : offers.findByRequestIdOrderByIdDesc(id))
            if ("PENDING".equals(o.getStatus())) o.setStatus("REJECTED");
        return view(r);
    }

    /** Accepting creates a CONFIRMED order and takes the stock under a row lock, so a listing cannot be oversold. */
    @Transactional
    public OfferView accept(Long buyerId, Long requestId, Long offerId) {
        PurchaseRequest r = requests.findByIdAndBuyerId(requestId, buyerId).orElseThrow(this::notFound);
        requireOpen(r);
        PurchaseOffer o = offers.findByIdAndRequestId(offerId, requestId).orElseThrow(this::notFound);
        if (!"PENDING".equals(o.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This offer is " + o.getStatus());
        Product p = products.findForUpdate(o.getProductId()).orElseThrow(this::notFound);
        if (!"ACTIVE".equals(p.getStatus()) || p.getQuantity().compareTo(o.getQuantity()) < 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The farmer no longer has enough stock for this offer");

        Order order = new Order();
        order.setProduct(p);
        order.setBuyer(users.getReferenceById(buyerId));
        order.setQuantity(o.getQuantity());
        order.setUnitPrice(o.getUnitPrice());
        order.setCurrency(o.getCurrency());
        order.setTotalPrice(o.getUnitPrice().multiply(o.getQuantity()).setScale(2, RoundingMode.HALF_UP));
        order.setStatus("CONFIRMED");
        order.setNote("From purchase request #" + r.getId());
        orders.save(order);

        p.setQuantity(p.getQuantity().subtract(o.getQuantity()));
        if (p.getQuantity().compareTo(BigDecimal.ZERO) == 0) p.setStatus("SOLD_OUT");

        o.setStatus("ACCEPTED");
        o.setOrderId(order.getId());
        for (PurchaseOffer other : offers.findByRequestIdOrderByIdDesc(requestId))
            if (!other.getId().equals(o.getId()) && "PENDING".equals(other.getStatus())) {
                other.setStatus("REJECTED");
                notifier.send(other.getFarmerId(), "OFFER", "Offer not chosen",
                        "The buyer chose another offer for " + r.getProductName() + ".", "requests");
            }
        r.setStatus("FULFILLED");
        r.setUpdatedAt(LocalDateTime.now());
        notifier.send(o.getFarmerId(), "OFFER", "Your offer was accepted",
                "A confirmed order was created for " + r.getProductName() + ". See Orders.", "orders");
        return offerView(o);
    }

    @Transactional
    public OfferView reject(Long buyerId, Long requestId, Long offerId) {
        requests.findByIdAndBuyerId(requestId, buyerId).orElseThrow(this::notFound);
        PurchaseOffer o = offers.findByIdAndRequestId(offerId, requestId).orElseThrow(this::notFound);
        if (!"PENDING".equals(o.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This offer is " + o.getStatus());
        o.setStatus("REJECTED");
        notifier.send(o.getFarmerId(), "OFFER", "Offer declined", "The buyer declined your offer.", "requests");
        return offerView(o);
    }

    // ---------- farmer ----------
    @Transactional(readOnly = true)
    public List<RequestView> open(String q, String province) {
        String prov = "";
        if (province != null && !province.isBlank()) {
            prov = ProvinceLocations.CENTRES.keySet().stream().filter(p -> p.equalsIgnoreCase(province.trim()))
                    .findFirst().orElseThrow(() -> bad("Unknown province"));
        }
        return requests.findOpen(q == null ? "" : q.trim(), prov, today(), PageRequest.of(0, 100))
                .stream().map(this::view).toList();
    }

    /** Open requests that fit at least one of the caller's active listings. */
    @Transactional(readOnly = true)
    public List<FarmerMatch> myMatches(Long farmerId) {
        List<Product> mine = activeListings(farmerId);
        if (mine.isEmpty()) return List.of();
        List<FarmerMatch> out = new ArrayList<>();
        for (PurchaseRequest r : requests.findOpen("", "", today(), PageRequest.of(0, 100))) {
            List<MyListing> fits = mine.stream().filter(p -> fits(p, r))
                    .map(p -> new MyListing(p.getId(), p.getName(), p.getQuantity(), p.getUnit(), p.getPrice(),
                            p.getCurrency(), p.getQuantity().compareTo(r.getQuantity()) >= 0)).toList();
            if (!fits.isEmpty()) out.add(new FarmerMatch(view(r), fits));
        }
        return out;
    }

    @Transactional
    public OfferView makeOffer(Long farmerId, Long requestId, OfferRequest o) {
        PurchaseRequest r = requests.findById(requestId).orElseThrow(this::notFound);
        if (!"OPEN".equals(r.getStatus()) || expired(r)) throw notFound();
        Product p = products.findByIdAndSellerId(o.productId(), farmerId)
                .orElseThrow(() -> bad("Choose one of your own listings"));
        if (!"ACTIVE".equals(p.getStatus())) throw bad("That listing is not active");
        if (!p.getUnit().equals(r.getUnit()))
            throw bad("Your listing is priced per " + p.getUnit() + " but the buyer wants " + r.getUnit());
        if (!p.getCurrency().equals(r.getCurrency()))
            throw bad("Your listing is in " + p.getCurrency() + " but the buyer wants " + r.getCurrency()
                    + ". Currencies are not converted.");
        if (o.quantity().compareTo(p.getQuantity()) > 0)
            throw bad("You only have " + p.getQuantity() + " " + p.getUnit() + " in that listing");
        if (o.quantity().compareTo(r.getQuantity()) > 0)
            throw bad("The buyer wants " + r.getQuantity() + " " + r.getUnit() + ". Offer no more than that.");
        BigDecimal price = o.unitPrice() == null ? p.getPrice() : o.unitPrice();
        if (r.getMaxPrice() != null && price.compareTo(r.getMaxPrice()) > 0)
            throw bad("That price is above the buyer's maximum of " + r.getMaxPrice() + " " + r.getCurrency());
        if (offers.existsByRequestIdAndFarmerIdAndStatus(requestId, farmerId, "PENDING"))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have an offer waiting on this request. Withdraw it first to change it.");

        PurchaseOffer offer = new PurchaseOffer();
        offer.setRequestId(requestId);
        offer.setFarmerId(farmerId);
        offer.setProductId(p.getId());
        offer.setQuantity(o.quantity());
        offer.setUnitPrice(price);
        offer.setCurrency(p.getCurrency());
        offer.setMessage(o.message());
        PurchaseOffer savedOffer = offers.save(offer);
        notifier.send(r.getBuyerId(), "OFFER", "New offer on your request",
                nameOf(farmerId) + " offered " + savedOffer.getQuantity() + " " + p.getUnit() + " of " + p.getName()
                        + " at " + savedOffer.getUnitPrice() + " " + savedOffer.getCurrency() + ".", "requests");
        return offerView(savedOffer);
    }

    @Transactional(readOnly = true)
    public List<OfferView> myOffers(Long farmerId) {
        return offers.findTop100ByFarmerIdOrderByIdDesc(farmerId).stream().map(this::offerView).toList();
    }

    @Transactional
    public OfferView withdraw(Long farmerId, Long offerId) {
        PurchaseOffer o = offers.findByIdAndFarmerId(offerId, farmerId).orElseThrow(this::notFound);
        if (!"PENDING".equals(o.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This offer is " + o.getStatus());
        o.setStatus("WITHDRAWN");
        requests.findById(o.getRequestId()).ifPresent(rq -> notifier.send(rq.getBuyerId(), "OFFER",
                "An offer was withdrawn", nameOf(farmerId) + " withdrew an offer on " + rq.getProductName() + ".", "requests"));
        return offerView(o);
    }

    // ---------- helpers ----------
    private List<Product> activeListings(Long farmerId) {
        return products.findBySellerIdAndStatusNotOrderByIdDesc(farmerId, "REMOVED").stream()
                .filter(p -> "ACTIVE".equals(p.getStatus()) && p.getQuantity().signum() > 0).toList();
    }

    private boolean fits(Product p, PurchaseRequest r) {
        if (!p.getUnit().equals(r.getUnit()) || !p.getCurrency().equals(r.getCurrency())) return false;
        if (r.getMaxPrice() != null && p.getPrice().compareTo(r.getMaxPrice()) > 0) return false;
        String a = p.getName().toLowerCase().trim(), b = r.getProductName().toLowerCase().trim();
        return a.contains(b) || b.contains(a);
    }

    private List<ListingMatch> listingsFor(PurchaseRequest r) {
        BigDecimal cap = r.getMaxPrice() == null ? NO_LIMIT : r.getMaxPrice();
        double[] from = ProvinceLocations.CENTRES.get(r.getProvince());
        List<Cand> cands = new ArrayList<>();
        for (Product p : products.search(r.getProductName(), "", r.getCurrency(), cap, PageRequest.of(0, 50))) {
            if (!p.getUnit().equals(r.getUnit())) continue;
            cands.add(new Cand(p, distance(from, p.getProvince()), p.getQuantity().compareTo(r.getQuantity()) >= 0));
        }
        cands.sort(Comparator.comparing((Cand c) -> !c.covers())
                .thenComparing((Cand c) -> c.dist() == null ? Double.MAX_VALUE : c.dist())
                .thenComparing((Cand c) -> c.p().getPrice()));
        Long nearestId = cands.stream().filter(c -> c.dist() != null)
                .min(Comparator.comparingDouble(Cand::dist)).map(c -> c.p().getId()).orElse(null);
        Long cheapestId = cands.stream().min(Comparator.comparing((Cand c) -> c.p().getPrice()))
                .map(c -> c.p().getId()).orElse(null);
        return cands.stream().map(c -> new ListingMatch(c.p().getId(), c.p().getSeller().getFullName(), c.p().getName(),
                c.p().getQuantity(), c.p().getUnit(), c.p().getPrice(), c.p().getCurrency(), c.p().getProvince(),
                c.p().getDistrict(), c.dist(), c.covers(), c.p().getId().equals(nearestId),
                c.p().getId().equals(cheapestId))).toList();
    }

    private static Double distance(double[] from, String province) {
        double[] to = ProvinceLocations.CENTRES.get(province);
        if (from == null || to == null) return null;
        double dLat = Math.toRadians(to[0] - from[0]), dLon = Math.toRadians(to[1] - from[1]);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(from[0]))
                * Math.cos(Math.toRadians(to[0])) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return Math.round(6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)) * 10) / 10.0;
    }

    private RequestView view(PurchaseRequest r) {
        String status = r.getStatus();
        if ("OPEN".equals(status) && expired(r)) status = "EXPIRED";
        return new RequestView(r.getId(), nameOf(r.getBuyerId()), r.getProductName(), r.getCategory(), r.getQuantity(),
                r.getUnit(), r.getMaxPrice(), r.getCurrency(), r.getProvince(), r.getDistrict(), r.getNeededBy(),
                r.getNote(), status, r.getCreatedAt(), offers.countByRequestIdAndStatus(r.getId(), "PENDING"));
    }

    private OfferView offerView(PurchaseOffer o) {
        PurchaseRequest r = requests.findById(o.getRequestId()).orElse(null);
        Product p = products.findById(o.getProductId()).orElse(null);
        return new OfferView(o.getId(), o.getRequestId(), r == null ? null : r.getProductName(), nameOf(o.getFarmerId()),
                o.getProductId(), p == null ? "Listing" : p.getName(), p == null ? null : p.getProvince(),
                o.getQuantity(), p == null ? (r == null ? "" : r.getUnit()) : p.getUnit(), o.getUnitPrice(), o.getCurrency(),
                o.getUnitPrice().multiply(o.getQuantity()).setScale(2, RoundingMode.HALF_UP), o.getMessage(),
                o.getStatus(), o.getOrderId(), o.getCreatedAt());
    }

    private void requireOpen(PurchaseRequest r) {
        if (!"OPEN".equals(r.getStatus()) || expired(r))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This request is closed or has expired");
    }

    private boolean expired(PurchaseRequest r) {
        return r.getNeededBy() != null && r.getNeededBy().isBefore(today());
    }

    private String nameOf(Long id) { return users.findById(id).map(User::getFullName).orElse("User"); }
    private static LocalDate today() { return LocalDate.now(HARARE); }
    private static String up(String s) { return s == null ? "" : s.trim().toUpperCase(); }
    private ResponseStatusException notFound() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"); }
    private static ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
}
