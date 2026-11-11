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
import java.util.List;

/**
 * Equipment bookings. Several PENDING requests may overlap, but CONFIRMED bookings never can: confirming locks the
 * equipment first, re-checks overlapping confirmed bookings and blocked dates, and declines overlapping pending requests.
 * Contact details are shared only for CONFIRMED or COMPLETED bookings.
 */
@Service
@RequiredArgsConstructor
public class BookingService {
    private static final ZoneId HARARE = ZoneId.of("Africa/Harare");
    private static final int MAX_DAYS = 30;
    private static final int MAX_PENDING = 10;

    private record Locked(Equipment e, EquipmentBooking b) {}

    private final EquipmentRepository equipment;
    private final EquipmentBookingRepository bookings;
    private final EquipmentBlockRepository blocks;
    private final UserRepository users;
    private final NotificationService notifier;

    // ---------- hirer ----------
    @Transactional
    public BookingView book(Long hirerId, BookingRequest r) {
        Equipment e = equipment.findById(r.equipmentId()).orElseThrow(this::notFound);
        if (!"ACTIVE".equals(e.getStatus())) throw notFound();
        if (e.getOwnerId().equals(hirerId)) throw bad("You cannot book your own equipment");
        LocalDate today = today();
        if (r.startDate().isBefore(today)) throw bad("The start date is in the past");
        if (r.endDate().isBefore(r.startDate())) throw bad("The end date is before the start date");
        if (r.startDate().isAfter(today.plusDays(365))) throw bad("Book within the next year");
        long days = ChronoUnit.DAYS.between(r.startDate(), r.endDate()) + 1;
        if (days > MAX_DAYS) throw bad("A booking can be at most " + MAX_DAYS + " days. Make a second booking for longer.");
        if (bookings.countByHirerIdAndStatus(hirerId, "PENDING") >= MAX_PENDING)
            throw bad("You already have " + MAX_PENDING + " requests waiting. Wait for answers first.");
        if (!bookings.confirmedOverlapping(List.of(e.getId()), r.startDate(), r.endDate()).isEmpty()
                || !blocks.overlapping(List.of(e.getId()), r.startDate(), r.endDate()).isEmpty())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This equipment is not available on those dates");

        EquipmentBooking b = new EquipmentBooking();
        b.setEquipmentId(e.getId());
        b.setHirerId(hirerId);
        b.setStartDate(r.startDate());
        b.setEndDate(r.endDate());
        b.setDays((int) days);
        b.setDailyRate(e.getDailyRate());
        b.setCurrency(e.getCurrency());
        b.setTotalPrice(e.getDailyRate().multiply(BigDecimal.valueOf(days)));
        b.setDeposit(e.getDeposit());
        b.setNote(r.note());
        b.setUpdatedAt(LocalDateTime.now());
        bookings.save(b);
        notifier.send(e.getOwnerId(), "EQUIPMENT", "New booking request",
                nameOf(hirerId) + " wants " + e.getName() + " from " + b.getStartDate() + " to " + b.getEndDate() + ".", "myequipment");
        return view(b, true);
    }

    @Transactional(readOnly = true)
    public List<BookingView> mine(Long hirerId) {
        return bookings.findTop100ByHirerIdOrderByIdDesc(hirerId).stream().map(b -> view(b, true)).toList();
    }

    @Transactional
    public BookingView cancel(Long hirerId, Long id) {
        EquipmentBooking b = bookings.findForUpdate(id).filter(x -> x.getHirerId().equals(hirerId)).orElseThrow(this::notFound);
        if (!"PENDING".equals(b.getStatus()) && !"CONFIRMED".equals(b.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This booking is " + b.getStatus());
        if ("CONFIRMED".equals(b.getStatus()) && !b.getStartDate().isAfter(today()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The hire has already started. Contact the owner.");
        b.setStatus("CANCELLED");
        b.setUpdatedAt(LocalDateTime.now());
        Equipment e = equipment.findById(b.getEquipmentId()).orElse(null);
        if (e != null)
            notifier.send(e.getOwnerId(), "EQUIPMENT", "Booking cancelled",
                    nameOf(hirerId) + " cancelled " + e.getName() + " for " + b.getStartDate() + " to " + b.getEndDate() + ".", "myequipment");
        return view(b, true);
    }

    // ---------- owner ----------
    @Transactional(readOnly = true)
    public List<BookingView> incoming(Long ownerId) {
        return bookings.forOwner(ownerId, PageRequest.of(0, 100)).stream().map(b -> view(b, false)).toList();
    }

    @Transactional
    public BookingView confirm(Long ownerId, Long id) {
        Locked l = lock(ownerId, id);
        Equipment e = l.e();
        EquipmentBooking b = l.b();
        require(b, "PENDING");
        if (!"ACTIVE".equals(e.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "This equipment was removed");
        if (b.getStartDate().isBefore(today())) throw new ResponseStatusException(HttpStatus.CONFLICT, "The start date has already passed");
        if (!bookings.lockConfirmedOverlapping(e.getId(), b.getStartDate(), b.getEndDate()).isEmpty())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This equipment is already booked on those dates");
        if (!blocks.lockOverlapping(e.getId(), b.getStartDate(), b.getEndDate()).isEmpty())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You marked some of those dates as unavailable");

        b.setStatus("CONFIRMED");
        b.setUpdatedAt(LocalDateTime.now());
        for (EquipmentBooking o : bookings.findByEquipmentIdAndStatus(e.getId(), "PENDING")) {
            if (o.getId().equals(b.getId()) || !overlaps(o, b)) continue;
            o.setStatus("REJECTED");
            o.setUpdatedAt(LocalDateTime.now());
            notifier.send(o.getHirerId(), "EQUIPMENT", "Booking request declined",
                    e.getName() + " was booked by someone else for those dates.", "hire");
        }
        notifier.send(b.getHirerId(), "EQUIPMENT", "Booking confirmed",
                e.getName() + " is booked for you from " + b.getStartDate() + " to " + b.getEndDate()
                        + ". Open Hire equipment to see the owner's contact details.", "hire");
        return view(b, false);
    }

    @Transactional
    public BookingView reject(Long ownerId, Long id) {
        Locked l = lock(ownerId, id);
        require(l.b(), "PENDING");
        l.b().setStatus("REJECTED");
        l.b().setUpdatedAt(LocalDateTime.now());
        notifier.send(l.b().getHirerId(), "EQUIPMENT", "Booking request declined",
                "The owner could not accept your request for " + l.e().getName() + ".", "hire");
        return view(l.b(), false);
    }

    @Transactional
    public BookingView ownerCancel(Long ownerId, Long id) {
        Locked l = lock(ownerId, id);
        require(l.b(), "CONFIRMED");
        l.b().setStatus("CANCELLED");
        l.b().setUpdatedAt(LocalDateTime.now());
        notifier.send(l.b().getHirerId(), "EQUIPMENT", "Booking cancelled by the owner",
                "The owner cancelled " + l.e().getName() + " for " + l.b().getStartDate() + " to " + l.b().getEndDate() + ".", "hire");
        return view(l.b(), false);
    }

    @Transactional
    public BookingView complete(Long ownerId, Long id) {
        Locked l = lock(ownerId, id);
        require(l.b(), "CONFIRMED");
        if (l.b().getStartDate().isAfter(today()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You can mark it completed once the hire has started");
        l.b().setStatus("COMPLETED");
        l.b().setUpdatedAt(LocalDateTime.now());
        notifier.send(l.b().getHirerId(), "EQUIPMENT", "Hire completed",
                "Your hire of " + l.e().getName() + " was marked completed.", "hire");
        return view(l.b(), false);
    }

    // ---------- helpers ----------
    /** Locks the equipment, then the booking, and only then reads anything else. Same order everywhere. */
    private Locked lock(Long ownerId, Long bookingId) {
        Long eid = bookings.equipmentIdOf(bookingId).orElseThrow(this::notFound);
        Equipment e = equipment.findForUpdate(eid).filter(x -> x.getOwnerId().equals(ownerId)).orElseThrow(this::notFound);
        EquipmentBooking b = bookings.findForUpdate(bookingId).orElseThrow(this::notFound);
        return new Locked(e, b);
    }

    private void require(EquipmentBooking b, String status) {
        if (!status.equals(b.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This booking is " + b.getStatus() + ", expected " + status);
    }

    private static boolean overlaps(EquipmentBooking a, EquipmentBooking b) {
        return !a.getStartDate().isAfter(b.getEndDate()) && !a.getEndDate().isBefore(b.getStartDate());
    }

    private BookingView view(EquipmentBooking b, boolean forHirer) {
        Equipment e = equipment.findById(b.getEquipmentId()).orElse(null);
        Long otherId = forHirer ? (e == null ? null : e.getOwnerId()) : b.getHirerId();
        Contact contact = null;
        if (otherId != null && ("CONFIRMED".equals(b.getStatus()) || "COMPLETED".equals(b.getStatus())))
            contact = users.findById(otherId).map(u -> new Contact(u.getFullName(), u.getPhone(), u.getEmail())).orElse(null);
        return new BookingView(b.getId(), b.getEquipmentId(), e == null ? "Equipment" : e.getName(),
                e == null ? "" : e.getCategory(), otherId == null ? "User" : nameOf(otherId), b.getStartDate(),
                b.getEndDate(), b.getDays(), b.getDailyRate(), b.getCurrency(), b.getTotalPrice(), b.getDeposit(),
                b.getNote(), b.getStatus(), b.getCreatedAt(), contact);
    }

    private String nameOf(Long id) { return users.findById(id).map(User::getFullName).orElse("User"); }
    private static LocalDate today() { return LocalDate.now(HARARE); }
    private ResponseStatusException notFound() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"); }
    private static ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
}
