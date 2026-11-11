package com.agripulse.backend.equipment;

import com.agripulse.backend.equipment.EquipmentDtos.*;
import com.agripulse.backend.notifications.NotificationService;
import com.agripulse.backend.user.User;
import com.agripulse.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Equipment listings, search and owner-blocked dates. Search is rule-based (filters, dates, rough distance).
 * Currencies are never converted. Dates are inclusive. Owners only ever touch their own equipment (others get 404).
 */
@Service
@RequiredArgsConstructor
public class EquipmentService {
    static final Set<String> CATEGORIES = Set.of("TRACTOR", "PLOUGH", "PLANTER", "HARVESTER", "SPRAYER",
            "IRRIGATION", "TRAILER", "GENERATOR", "OTHER");
    static final Set<String> CURRENCIES = Set.of("USD", "ZIG", "ZAR");
    private static final BigDecimal NO_LIMIT = new BigDecimal("999999999999");
    private static final ZoneId HARARE = ZoneId.of("Africa/Harare");
    private static final int MAX_ACTIVE = 30;
    private static final int WINDOW_DAYS = 120;

    private record Cand(Equipment e, Double dist, Boolean avail, List<BusyRange> busy) {}

    private final EquipmentRepository equipment;
    private final EquipmentBookingRepository bookings;
    private final EquipmentBlockRepository blocks;
    private final UserRepository users;
    private final NotificationService notifier;

    // ---------- hirer: search ----------
    @Transactional(readOnly = true)
    public List<EquipmentOption> search(Long callerId, String category, String province, String currency,
                                        BigDecimal maxRate, LocalDate from, LocalDate to, String near) {
        String cat = blank(category) ? "" : up(category);
        if (!cat.isEmpty() && !CATEGORIES.contains(cat)) throw bad("Category must be one of " + CATEGORIES);
        String cur = blank(currency) ? "" : up(currency);
        if (!cur.isEmpty() && !CURRENCIES.contains(cur)) throw bad("Currency must be one of " + CURRENCIES);
        String prov = "";
        if (!blank(province)) {
            prov = Places.province(province);
            if (prov == null) throw bad("Unknown province");
        }
        String nearProv = null;
        if (!blank(near)) {
            nearProv = Places.province(near);
            if (nearProv == null) throw bad("Unknown province");
        }
        LocalDate today = today();
        if ((from == null) != (to == null)) throw bad("Give both dates, or neither");
        if (from != null) {
            if (from.isBefore(today)) throw bad("The start date is in the past");
            if (to.isBefore(from)) throw bad("The end date is before the start date");
            if (to.isAfter(today.plusDays(365))) throw bad("Choose dates within the next year");
        }

        List<Equipment> list = equipment.search(cat, prov, cur, maxRate == null ? NO_LIMIT : maxRate, callerId,
                PageRequest.of(0, 100));
        if (list.isEmpty()) return List.of();

        LocalDate windowEnd = (to != null && to.isAfter(today.plusDays(WINDOW_DAYS))) ? to : today.plusDays(WINDOW_DAYS);
        List<Long> ids = list.stream().map(Equipment::getId).toList();
        Map<Long, List<BusyRange>> busy = new HashMap<>();
        for (EquipmentBooking b : bookings.confirmedOverlapping(ids, today, windowEnd))
            busy.computeIfAbsent(b.getEquipmentId(), k -> new ArrayList<>())
                    .add(new BusyRange(b.getStartDate(), b.getEndDate(), "BOOKED"));
        for (EquipmentBlock k : blocks.overlapping(ids, today, windowEnd))
            busy.computeIfAbsent(k.getEquipmentId(), x -> new ArrayList<>())
                    .add(new BusyRange(k.getStartDate(), k.getEndDate(), "UNAVAILABLE"));
        busy.values().forEach(l -> l.sort(Comparator.comparing(BusyRange::from)));

        List<Cand> cands = new ArrayList<>();
        for (Equipment e : list) {
            List<BusyRange> b = busy.getOrDefault(e.getId(), List.of());
            Boolean avail = null;
            if (from != null) {
                final LocalDate f = from, t = to;
                avail = b.stream().noneMatch(r -> !r.from().isAfter(t) && !r.to().isBefore(f));
            }
            Double dist = nearProv == null ? null : Places.km(nearProv, e.getProvince());
            cands.add(new Cand(e, dist, avail, b));
        }
        cands.sort(Comparator.comparing((Cand c) -> Boolean.FALSE.equals(c.avail()))
                .thenComparing((Cand c) -> c.dist() == null ? Double.MAX_VALUE : c.dist())
                .thenComparing((Cand c) -> c.e().getDailyRate()));

        Long nearestId = cands.stream().filter(c -> !Boolean.FALSE.equals(c.avail()) && c.dist() != null)
                .min(Comparator.comparingDouble(Cand::dist)).map(c -> c.e().getId()).orElse(null);
        Map<String, Long> cheapestId = new HashMap<>();
        Map<String, BigDecimal> cheapestRate = new HashMap<>();
        for (Cand c : cands) {
            if (Boolean.FALSE.equals(c.avail())) continue;
            String curr = c.e().getCurrency();
            BigDecimal best = cheapestRate.get(curr);
            if (best == null || c.e().getDailyRate().compareTo(best) < 0) {
                cheapestRate.put(curr, c.e().getDailyRate());
                cheapestId.put(curr, c.e().getId());
            }
        }
        long days = from == null ? 0 : ChronoUnit.DAYS.between(from, to) + 1;
        List<EquipmentOption> out = new ArrayList<>();
        for (Cand c : cands) {
            Equipment e = c.e();
            out.add(new EquipmentOption(view(e), c.dist(), c.avail(), e.getId().equals(nearestId),
                    e.getId().equals(cheapestId.get(e.getCurrency())),
                    from == null ? null : e.getDailyRate().multiply(BigDecimal.valueOf(days)), c.busy()));
        }
        return out;
    }

    // ---------- owner ----------
    @Transactional(readOnly = true)
    public List<MyEquipment> mine(Long ownerId) {
        LocalDate today = today();
        return equipment.findByOwnerIdAndStatusNotOrderByIdDesc(ownerId, "REMOVED").stream()
                .map(e -> new MyEquipment(view(e), blocks.findByEquipmentIdOrderByStartDateAsc(e.getId()).stream()
                        .filter(b -> !b.getEndDate().isBefore(today)).map(this::blockView).toList())).toList();
    }

    @Transactional
    public EquipmentView create(Long ownerId, EquipmentRequest r) {
        String cat = up(r.category()), cur = up(r.currency());
        if (!CATEGORIES.contains(cat)) throw bad("Category must be one of " + CATEGORIES);
        if (!CURRENCIES.contains(cur)) throw bad("Currency must be one of " + CURRENCIES);
        String province = Places.province(r.province());
        if (province == null) throw bad("Unknown province");
        if (equipment.countByOwnerIdAndStatus(ownerId, "ACTIVE") >= MAX_ACTIVE)
            throw bad("You already have " + MAX_ACTIVE + " listings. Remove some first.");

        Equipment e = new Equipment();
        e.setOwnerId(ownerId);
        e.setCategory(cat);
        e.setName(r.name().trim());
        e.setDescription(r.description());
        e.setProvince(province);
        e.setDistrict(r.district());
        e.setDailyRate(r.dailyRate());
        e.setCurrency(cur);
        e.setOperatorIncluded(Boolean.TRUE.equals(r.operatorIncluded()));
        e.setFuelIncluded(Boolean.TRUE.equals(r.fuelIncluded()));
        e.setDeposit(r.deposit() == null ? BigDecimal.ZERO : r.deposit());
        return view(equipment.save(e));
    }

    @Transactional
    public void remove(Long ownerId, Long id) {
        Equipment e = owned(ownerId, id);
        if (bookings.existsByEquipmentIdAndStatusAndEndDateGreaterThanEqual(id, "CONFIRMED", today()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This equipment has confirmed bookings. Cancel or complete them first.");
        for (EquipmentBooking b : bookings.findByEquipmentIdAndStatus(id, "PENDING")) {
            b.setStatus("REJECTED");
            b.setUpdatedAt(LocalDateTime.now());
            notifier.send(b.getHirerId(), "EQUIPMENT", "Booking request declined",
                    "The owner removed " + e.getName() + " from AgriPulse.", "hire");
        }
        e.setStatus("REMOVED");
    }

    @Transactional
    public BlockView addBlock(Long ownerId, Long equipmentId, BlockRequest r) {
        owned(ownerId, equipmentId);
        LocalDate today = today();
        if (r.endDate().isBefore(r.startDate())) throw bad("The end date is before the start date");
        if (r.endDate().isBefore(today)) throw bad("Those dates are in the past");
        if (ChronoUnit.DAYS.between(r.startDate(), r.endDate()) > 365) throw bad("Block at most one year at a time");
        if (!bookings.lockConfirmedOverlapping(equipmentId, r.startDate(), r.endDate()).isEmpty())
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "There is a confirmed booking in those dates. Cancel it first.");
        EquipmentBlock k = new EquipmentBlock();
        k.setEquipmentId(equipmentId);
        k.setStartDate(r.startDate());
        k.setEndDate(r.endDate());
        k.setReason(r.reason());
        return blockView(blocks.save(k));
    }

    @Transactional
    public void deleteBlock(Long ownerId, Long equipmentId, Long blockId) {
        owned(ownerId, equipmentId);
        blocks.delete(blocks.findByIdAndEquipmentId(blockId, equipmentId).orElseThrow(this::notFound));
    }

    // ---------- helpers ----------
    private Equipment owned(Long ownerId, Long id) {
        Equipment e = equipment.findForUpdate(id).filter(x -> x.getOwnerId().equals(ownerId)).orElseThrow(this::notFound);
        if (!"ACTIVE".equals(e.getStatus())) throw notFound();
        return e;
    }

    EquipmentView view(Equipment e) {
        return new EquipmentView(e.getId(), nameOf(e.getOwnerId()), e.getCategory(), e.getName(), e.getDescription(),
                e.getProvince(), e.getDistrict(), e.getDailyRate(), e.getCurrency(), e.isOperatorIncluded(),
                e.isFuelIncluded(), e.getDeposit(), e.getStatus());
    }

    private BlockView blockView(EquipmentBlock k) {
        return new BlockView(k.getId(), k.getStartDate(), k.getEndDate(), k.getReason());
    }

    private String nameOf(Long id) { return users.findById(id).map(User::getFullName).orElse("User"); }
    private static boolean blank(String s) { return s == null || s.isBlank(); }
    private static LocalDate today() { return LocalDate.now(HARARE); }
    private static String up(String s) { return s == null ? "" : s.trim().toUpperCase(); }
    private ResponseStatusException notFound() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"); }
    private static ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
}
