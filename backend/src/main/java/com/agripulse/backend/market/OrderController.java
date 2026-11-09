package com.agripulse.backend.market;

/**
 * Controllers. Farmers manage listings and respond to orders, and buyers
 * place and cancel orders. The user id always comes from the token.
 */

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService service;

    private static Long uid(Jwt jwt) { return Long.valueOf(jwt.getSubject()); }

    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse place(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OrderRequest r) {
        return service.place(uid(jwt), r);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('BUYER')")
    public List<OrderResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.myOrders(uid(jwt));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('BUYER')")
    public OrderResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.cancel(uid(jwt), id);
    }

    @GetMapping("/incoming")
    @PreAuthorize("hasRole('FARMER')")
    public List<OrderResponse> incoming(@AuthenticationPrincipal Jwt jwt) {
        return service.incoming(uid(jwt));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('FARMER')")
    public OrderResponse confirm(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.confirm(uid(jwt), id);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('FARMER')")
    public OrderResponse reject(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.reject(uid(jwt), id);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('FARMER')")
    public OrderResponse complete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.complete(uid(jwt), id);
    }
}
