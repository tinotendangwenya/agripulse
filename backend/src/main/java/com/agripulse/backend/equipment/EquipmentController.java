package com.agripulse.backend.equipment;

import com.agripulse.backend.equipment.EquipmentDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Farmers hire equipment. Equipment owners (and farmers who rent out their own machines) list it and answer bookings.
 * The user id comes from the token, never from the request.
 */
@RestController
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
public class EquipmentController {
    private static final String OWNER = "hasAnyRole('EQUIPMENT_OWNER','FARMER')";
    private static final String HIRER = "hasRole('FARMER')";

    private final EquipmentService equipment;
    private final BookingService bookings;

    private static Long uid(Jwt jwt) { return Long.valueOf(jwt.getSubject()); }

    // ----- hirer -----
    @GetMapping
    @PreAuthorize(HIRER)
    public List<EquipmentOption> search(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String category, @RequestParam(required = false) String province,
            @RequestParam(required = false) String currency, @RequestParam(required = false) BigDecimal maxRate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String near) {
        return equipment.search(uid(jwt), category, province, currency, maxRate, from, to, near);
    }

    @PostMapping("/bookings")
    @PreAuthorize(HIRER)
    @ResponseStatus(HttpStatus.CREATED)
    public BookingView book(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody BookingRequest r) {
        return bookings.book(uid(jwt), r);
    }

    @GetMapping("/bookings/mine")
    @PreAuthorize(HIRER)
    public List<BookingView> myBookings(@AuthenticationPrincipal Jwt jwt) {
        return bookings.mine(uid(jwt));
    }

    @PostMapping("/bookings/{id}/cancel")
    @PreAuthorize(HIRER)
    public BookingView cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return bookings.cancel(uid(jwt), id);
    }

    // ----- owner -----
    @GetMapping("/mine")
    @PreAuthorize(OWNER)
    public List<MyEquipment> mine(@AuthenticationPrincipal Jwt jwt) {
        return equipment.mine(uid(jwt));
    }

    @PostMapping
    @PreAuthorize(OWNER)
    @ResponseStatus(HttpStatus.CREATED)
    public EquipmentView create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody EquipmentRequest r) {
        return equipment.create(uid(jwt), r);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(OWNER)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        equipment.remove(uid(jwt), id);
    }

    @PostMapping("/{id}/blocks")
    @PreAuthorize(OWNER)
    @ResponseStatus(HttpStatus.CREATED)
    public BlockView addBlock(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody BlockRequest r) {
        return equipment.addBlock(uid(jwt), id, r);
    }

    @DeleteMapping("/{id}/blocks/{blockId}")
    @PreAuthorize(OWNER)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBlock(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @PathVariable Long blockId) {
        equipment.deleteBlock(uid(jwt), id, blockId);
    }

    @GetMapping("/bookings/incoming")
    @PreAuthorize(OWNER)
    public List<BookingView> incoming(@AuthenticationPrincipal Jwt jwt) {
        return bookings.incoming(uid(jwt));
    }

    @PostMapping("/bookings/{id}/confirm")
    @PreAuthorize(OWNER)
    public BookingView confirm(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return bookings.confirm(uid(jwt), id);
    }

    @PostMapping("/bookings/{id}/reject")
    @PreAuthorize(OWNER)
    public BookingView reject(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return bookings.reject(uid(jwt), id);
    }

    @PostMapping("/bookings/{id}/owner-cancel")
    @PreAuthorize(OWNER)
    public BookingView ownerCancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return bookings.ownerCancel(uid(jwt), id);
    }

    @PostMapping("/bookings/{id}/complete")
    @PreAuthorize(OWNER)
    public BookingView complete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return bookings.complete(uid(jwt), id);
    }
}
