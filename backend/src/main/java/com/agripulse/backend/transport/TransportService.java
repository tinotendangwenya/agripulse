package com.agripulse.backend.transport;

import com.agripulse.backend.notifications.NotificationService;
import com.agripulse.backend.transport.TransportDtos.*;
import com.agripulse.backend.user.User;
import com.agripulse.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
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
 * Transport jobs and quotes. Matching is rule-based: capacity and cargo type. Estimates are rough
 * (rate x straight-line distance x 1.3, never below the minimum charge). Currencies are never converted.
 * Contact details are shown only to the two people in an accepted booking.
 */
@Service
@RequiredArgsConstructor
public class TransportService {
    private static final ZoneId HARARE = ZoneId.of("Africa/Harare");
    private static final int MAX_OPEN = 20;
    private static final Set<String> BOOKED_STATES = Set.of("BOOKED", "IN_TRANSIT", "DELIVERED");

    private final TransportRequestRepository requests;
    private final TransportQuoteRepository quotes;
    private final VehicleRepository vehicles;
    private final UserRepository users;
    private final NotificationService notifier;

    // ---------- requester ----------
    @Transactional
    public RequestView create(Long requesterId, RequestCreate r) {
        String cargo = up(r.cargoType()), unit = up(r.weightUnit());
        if (!VehicleService.CARGO_ORDER.contains(cargo)) throw bad("Cargo type must be one of " + VehicleService.CARGO_ORDER);
        if (!unit.equals("KG") && !unit.equals("TONNE")) throw bad("Weight unit must be KG or TONNE");
        String pickup = Geo.province(r.pickupProvince()), delivery = Geo.province(r.deliveryProvince());
        if (pickup == null || delivery == null) throw bad("Unknown province");
        if (r.neededBy() != null && r.neededBy().isBefore(today())) throw bad("The needed-by date is in the past");
        if (requests.countByRequesterIdAndStatus(requesterId, "OPEN") >= MAX_OPEN)
            throw bad("You already have " + MAX_OPEN + " open transport requests. Cancel some first.");

        TransportRequest t = new TransportRequest();
        t.setRequesterId(requesterId);
        t.setCargoType(cargo);
        t.setDescription(r.description().trim());
        t.setWeightKg(r.weight().multiply(unit.equals("TONNE") ? BigDecimal.valueOf(1000) : BigDecimal.ONE)
                .setScale(2, RoundingMode.HALF_UP));
        t.setPickupProvince(pickup);
        t.setPickupDistrict(r.pickupDistrict());
        t.setDeliveryProvince(delivery);
        t.setDeliveryDistrict(r.deliveryDistrict());
        t.setNeededBy(r.neededBy());
        t.setNote(r.note());
        t.setUpdatedAt(LocalDateTime.now());
        requests.save(t);

        Set<Long> owners = new LinkedHashSet<>();
        for (Vehicle v : vehicles.findFitting(t.getWeightKg(), cargo, PageRequest.of(0, 300)))
            if (carries(v, cargo)) owners.add(v.getOwnerId());
        for (Long id : owners)
            notifier.send(id, "TRANSPORT", "New transport job", cargo.toLowerCase() + ", " + tKg(t.getWeightKg()) + ": "
                    + pickup + " to " + delivery + ". One of your vehicles fits.", "jobs");
        return view(t);
    }

    @Transactional(readOnly = true)
    public List<RequestView> mine(Long requesterId) {
        return requests.findTop100ByRequesterIdOrderByIdDesc(requesterId).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public RequestDetail detail(Long requesterId, Long id) {
        TransportRequest r = requests.findByIdAndRequesterId(id, requesterId).orElseThrow(this::notFound);
        List<QuoteView> list = quotes.findByRequestIdOrderByIdDesc(id).stream().map(q -> quoteView(q, r, true)).toList();
        boolean open = "OPEN".equals(r.getStatus()) && !expired(r);
        Contact transporter = null;
        if (BOOKED_STATES.contains(r.getStatus()) && r.getAcceptedQuoteId() != null)
            transporter = quotes.findById(r.getAcceptedQuoteId()).map(q -> contactOf(q.getTransporterId())).orElse(null);
        return new RequestDetail(view(r), list, open ? options(r) : List.of(), transporter);
    }

    @Transactional
    public QuoteView accept(Long requesterId, Long requestId, Long quoteId) {
        TransportRequest r = requests.findForUpdate(requestId).filter(x -> x.getRequesterId().equals(requesterId))
                .orElseThrow(this::notFound);
        if (!"OPEN".equals(r.getStatus()) || expired(r))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This request is no longer open");
        TransportQuote q = quotes.findByIdAndRequestId(quoteId, requestId).orElseThrow(this::notFound);
        if (!"PENDING".equals(q.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This quote is " + q.getStatus());
        Vehicle v = vehicles.findById(q.getVehicleId()).orElse(null);
        if (v == null || !"ACTIVE".equals(v.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That vehicle is no longer available");

        q.setStatus("ACCEPTED");
        r.setStatus("BOOKED");
        r.setAcceptedQuoteId(q.getId());
        r.setUpdatedAt(LocalDateTime.now());
        for (TransportQuote other : quotes.findByRequestIdOrderByIdDesc(requestId)) {
            if (!other.getId().equals(q.getId()) && "PENDING".equals(other.getStatus())) {
                other.setStatus("REJECTED");
                notifier.send(other.getTransporterId(), "TRANSPORT", "Quote not chosen",
                        "The requester chose another transporter for " + route(r) + ".", "jobs");
            }
        }
        notifier.send(q.getTransporterId(), "TRANSPORT", "Your quote was accepted",
                "You are booked for " + route(r) + ". Open Jobs to see the contact details.", "jobs");
        return quoteView(q, r, true);
    }

    @Transactional
    public QuoteView reject(Long requesterId, Long requestId, Long quoteId) {
        TransportRequest r = requests.findByIdAndRequesterId(requestId, requesterId).orElseThrow(this::notFound);
        TransportQuote q = quotes.findByIdAndRequestId(quoteId, requestId).orElseThrow(this::notFound);
        if (!"PENDING".equals(q.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This quote is " + q.getStatus());
        q.setStatus("REJECTED");
        notifier.send(q.getTransporterId(), "TRANSPORT", "Quote declined",
                "The requester declined your quote for " + route(r) + ".", "jobs");
        return quoteView(q, r, true);
    }

    @Transactional
    public RequestView cancel(Long requesterId, Long id) {
        TransportRequest r = requests.findForUpdate(id).filter(x -> x.getRequesterId().equals(requesterId))
                .orElseThrow(this::notFound);
        if ("IN_TRANSIT".equals(r.getStatus()) || "DELIVERED".equals(r.getStatus()) || "CANCELLED".equals(r.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This job is " + r.getStatus() + " and cannot be cancelled here. Contact the transporter.");
        for (TransportQuote q : quotes.findByRequestIdOrderByIdDesc(id)) {
            if ("PENDING".equals(q.getStatus())) {
                q.setStatus("REJECTED");
                notifier.send(q.getTransporterId(), "TRANSPORT", "Transport job cancelled",
                        "The requester cancelled " + route(r) + ".", "jobs");
            } else if ("ACCEPTED".equals(q.getStatus())) {
                q.setStatus("CANCELLED");
                notifier.send(q.getTransporterId(), "TRANSPORT", "Booking cancelled",
                        "The requester cancelled the booking for " + route(r) + ".", "jobs");
            }
        }
        r.setStatus("CANCELLED");
        r.setUpdatedAt(LocalDateTime.now());
        return view(r);
    }

    // ---------- transporter ----------
    @Transactional(readOnly = true)
    public List<Job> jobs(Long transporterId) {
        List<Vehicle> mine = vehicles.findByOwnerIdAndStatusNotOrderByIdDesc(transporterId, "REMOVED").stream()
                .filter(v -> "ACTIVE".equals(v.getStatus())).toList();
        List<Job> out = new ArrayList<>();
        for (TransportRequest r : requests.findOpen(today(), PageRequest.of(0, 100))) {
            Double haul = Geo.km(r.getPickupProvince(), r.getDeliveryProvince());
            List<VehicleFit> fits = mine.stream()
                    .filter(v -> v.getCapacityKg().compareTo(r.getWeightKg()) >= 0 && carries(v, r.getCargoType()))
                    .map(v -> new VehicleFit(v.getId(), v.getDescription(), estimate(v, haul), v.getCurrency())).toList();
            boolean quoted = quotes.existsByRequestIdAndTransporterIdAndStatus(r.getId(), transporterId, "PENDING");
            out.add(new Job(view(r), fits, quoted));
        }
        out.sort(Comparator.comparing((Job j) -> j.myVehicles().isEmpty()));
        return out;
    }

    @Transactional
    public QuoteView quote(Long transporterId, Long requestId, QuoteCreate c) {
        TransportRequest r = requests.findById(requestId).orElseThrow(this::notFound);
        if (!"OPEN".equals(r.getStatus()) || expired(r)) throw notFound();
        Vehicle v = vehicles.findByIdAndOwnerId(c.vehicleId(), transporterId)
                .orElseThrow(() -> bad("Choose one of your own vehicles"));
        if (!"ACTIVE".equals(v.getStatus())) throw bad("That vehicle is not active");
        if (v.getCapacityKg().compareTo(r.getWeightKg()) < 0)
            throw bad("That vehicle carries up to " + tKg(v.getCapacityKg()) + " but the load is " + tKg(r.getWeightKg()));
        if (!carries(v, r.getCargoType())) throw bad("That vehicle is not set up to carry " + r.getCargoType().toLowerCase());
        if (quotes.existsByRequestIdAndTransporterIdAndStatus(requestId, transporterId, "PENDING"))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have a quote waiting on this job. Withdraw it first to change it.");

        TransportQuote q = new TransportQuote();
        q.setRequestId(requestId);
        q.setTransporterId(transporterId);
        q.setVehicleId(v.getId());
        q.setPrice(c.price().setScale(2, RoundingMode.HALF_UP));
        q.setCurrency(v.getCurrency());
        q.setMessage(c.message());
        quotes.save(q);
        notifier.send(r.getRequesterId(), "TRANSPORT", "New transport quote",
                nameOf(transporterId) + " quoted " + q.getPrice() + " " + q.getCurrency() + " for " + route(r) + ".", "transport");
        return quoteView(q, r, false);
    }

    @Transactional(readOnly = true)
    public List<QuoteView> myQuotes(Long transporterId) {
        List<QuoteView> out = new ArrayList<>();
        for (TransportQuote q : quotes.findTop100ByTransporterIdOrderByIdDesc(transporterId)) {
            requests.findById(q.getRequestId()).ifPresent(r -> out.add(quoteView(q, r, false)));
        }
        return out;
    }

    @Transactional
    public QuoteView withdraw(Long transporterId, Long quoteId) {
        TransportQuote q = quotes.findByIdAndTransporterId(quoteId, transporterId).orElseThrow(this::notFound);
        if (!"PENDING".equals(q.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This quote is " + q.getStatus());
        q.setStatus("WITHDRAWN");
        TransportRequest r = requests.findById(q.getRequestId()).orElseThrow(this::notFound);
        notifier.send(r.getRequesterId(), "TRANSPORT", "A quote was withdrawn",
                nameOf(transporterId) + " withdrew a quote for " + route(r) + ".", "transport");
        return quoteView(q, r, false);
    }

    @Transactional
    public RequestView setStatus(Long transporterId, Long requestId, String status) {
        String s = up(status);
        if (!s.equals("IN_TRANSIT") && !s.equals("DELIVERED")) throw bad("Status must be IN_TRANSIT or DELIVERED");
        TransportRequest r = requests.findForUpdate(requestId).orElseThrow(this::notFound);
        TransportQuote q = r.getAcceptedQuoteId() == null ? null : quotes.findById(r.getAcceptedQuoteId()).orElse(null);
        if (q == null || !q.getTransporterId().equals(transporterId)) throw notFound();
        String need = s.equals("IN_TRANSIT") ? "BOOKED" : "IN_TRANSIT";
        if (!need.equals(r.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This job is " + r.getStatus() + ", expected " + need);
        r.setStatus(s);
        r.setUpdatedAt(LocalDateTime.now());
        notifier.send(r.getRequesterId(), "TRANSPORT", s.equals("IN_TRANSIT") ? "Your load is on the way" : "Your load was delivered",
                route(r) + ".", "transport");
        return view(r);
    }

    // ---------- helpers ----------
    private List<VehicleOption> options(TransportRequest r) {
        record Cand(Vehicle v, Double toPickup, BigDecimal est) {}
        Double haul = Geo.km(r.getPickupProvince(), r.getDeliveryProvince());
        List<Cand> cands = new ArrayList<>();
        for (Vehicle v : vehicles.findFitting(r.getWeightKg(), r.getCargoType(), PageRequest.of(0, 50))) {
            if (!carries(v, r.getCargoType())) continue;
            cands.add(new Cand(v, Geo.km(v.getBaseProvince(), r.getPickupProvince()), estimate(v, haul)));
        }
        cands.sort(Comparator.comparing((Cand c) -> c.toPickup() == null ? Double.MAX_VALUE : c.toPickup())
                .thenComparing((Cand c) -> c.est() == null ? new BigDecimal("999999999") : c.est()));
        Long nearestId = cands.stream().filter(c -> c.toPickup() != null)
                .min(Comparator.comparingDouble(Cand::toPickup)).map(c -> c.v().getId()).orElse(null);
        Map<String, Long> cheapestId = new HashMap<>();
        Map<String, BigDecimal> cheapestEst = new HashMap<>();
        for (Cand c : cands) {
            if (c.est() == null) continue;
            String cur = c.v().getCurrency();
            BigDecimal best = cheapestEst.get(cur);
            if (best == null || c.est().compareTo(best) < 0) { cheapestEst.put(cur, c.est()); cheapestId.put(cur, c.v().getId()); }
        }
        return cands.stream().map(c -> new VehicleOption(c.v().getId(), nameOf(c.v().getOwnerId()), c.v().getDescription(),
                c.v().getVehicleType(), c.v().getCapacityKg(), c.v().getBaseProvince(), c.toPickup(), c.est(),
                c.v().getCurrency(), c.v().getId().equals(nearestId),
                c.v().getId().equals(cheapestId.get(c.v().getCurrency())))).toList();
    }

    private static BigDecimal estimate(Vehicle v, Double km) {
        if (km == null) return null;
        BigDecimal byKm = v.getRatePerKm().multiply(BigDecimal.valueOf(km * Geo.ROAD_FACTOR));
        BigDecimal min = v.getMinCharge() == null ? BigDecimal.ZERO : v.getMinCharge();
        return byKm.max(min).setScale(2, RoundingMode.HALF_UP);
    }

    private static boolean carries(Vehicle v, String cargo) {
        return Arrays.asList(v.getCargoTypes().split(",")).contains(cargo);
    }

    private RequestView view(TransportRequest r) {
        String status = r.getStatus();
        if ("OPEN".equals(status) && expired(r)) status = "EXPIRED";
        return new RequestView(r.getId(), nameOf(r.getRequesterId()), r.getCargoType(), r.getDescription(), r.getWeightKg(),
                r.getPickupProvince(), r.getPickupDistrict(), r.getDeliveryProvince(), r.getDeliveryDistrict(),
                r.getNeededBy(), r.getNote(), status, r.getCreatedAt(),
                quotes.countByRequestIdAndStatus(r.getId(), "PENDING"), Geo.km(r.getPickupProvince(), r.getDeliveryProvince()));
    }

    private QuoteView quoteView(TransportQuote q, TransportRequest r, boolean forRequester) {
        Vehicle v = vehicles.findById(q.getVehicleId()).orElse(null);
        Contact contact = null;
        if ("ACCEPTED".equals(q.getStatus()) && BOOKED_STATES.contains(r.getStatus()))
            contact = contactOf(forRequester ? q.getTransporterId() : r.getRequesterId());
        String summary = r.getCargoType().toLowerCase() + ", " + tKg(r.getWeightKg()) + ": " + route(r);
        return new QuoteView(q.getId(), r.getId(), summary, nameOf(q.getTransporterId()), q.getVehicleId(),
                v == null ? "Vehicle" : v.getDescription(), q.getPrice(), q.getCurrency(), q.getMessage(),
                q.getStatus(), r.getStatus(), q.getCreatedAt(), contact);
    }

    private Contact contactOf(Long userId) {
        return users.findById(userId).map(u -> new Contact(u.getFullName(), u.getPhone(), u.getEmail())).orElse(null);
    }

    private static String route(TransportRequest r) { return r.getPickupProvince() + " to " + r.getDeliveryProvince(); }
    private static String tKg(BigDecimal kg) {
        return kg.compareTo(BigDecimal.valueOf(1000)) >= 0
                ? kg.divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + " t"
                : kg.setScale(0, RoundingMode.HALF_UP).toPlainString() + " kg";
    }
    private boolean expired(TransportRequest r) { return r.getNeededBy() != null && r.getNeededBy().isBefore(today()); }
    private String nameOf(Long id) { return users.findById(id).map(User::getFullName).orElse("User"); }
    private static LocalDate today() { return LocalDate.now(HARARE); }
    private static String up(String s) { return s == null ? "" : s.trim().toUpperCase(); }
    private ResponseStatusException notFound() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"); }
    private static ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
}
