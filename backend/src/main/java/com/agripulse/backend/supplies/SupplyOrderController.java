package com.agripulse.backend.supplies;

import com.agripulse.backend.supplies.SupplyOrderDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/supplies/orders")
@RequiredArgsConstructor
public class SupplyOrderController {
    private final SupplyOrderService service;

    private static Long uid(Jwt jwt) { return Long.valueOf(jwt.getSubject()); }

    // ----- Farmers -----

    @PostMapping
    @PreAuthorize("hasRole('FARMER')")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderView place(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OrderRequest r) {
        return service.place(uid(jwt), r);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('FARMER')")
    public List<OrderView> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.mine(uid(jwt));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('FARMER')")
    public OrderView cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.cancel(uid(jwt), id);
    }

    // ----- Suppliers -----

    @GetMapping("/incoming")
    @PreAuthorize("hasRole('SUPPLIER')")
    public List<OrderView> incoming(@AuthenticationPrincipal Jwt jwt) {
        return service.incoming(uid(jwt));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('SUPPLIER')")
    public OrderView confirm(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.confirm(uid(jwt), id);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('SUPPLIER')")
    public OrderView reject(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.reject(uid(jwt), id);
    }

    @PostMapping("/{id}/dispatch")
    @PreAuthorize("hasRole('SUPPLIER')")
    public OrderView dispatch(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.dispatch(uid(jwt), id);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('SUPPLIER')")
    public OrderView complete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.complete(uid(jwt), id);
    }

    @PostMapping("/{id}/paid")
    @PreAuthorize("hasRole('SUPPLIER')")
    public OrderView paid(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.markPaid(uid(jwt), id);
    }
}