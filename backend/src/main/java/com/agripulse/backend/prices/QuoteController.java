package com.agripulse.backend.prices;

import com.agripulse.backend.prices.PriceDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Users submit quotes. Only an ADMIN can approve or reject them. */
@RestController
@RequiredArgsConstructor
public class QuoteController {
    private final QuoteService service;

    private static Long uid(Jwt jwt) { return Long.valueOf(jwt.getSubject()); }

    @PostMapping("/api/prices/quotes")
    @PreAuthorize("hasAnyRole('FARMER','BUYER','SUPPLIER')")
    @ResponseStatus(HttpStatus.CREATED)
    public QuoteView submit(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody QuoteRequest r) {
        return service.submit(uid(jwt), r);
    }

    @GetMapping("/api/prices/quotes/mine")
    @PreAuthorize("hasAnyRole('FARMER','BUYER','SUPPLIER')")
    public List<QuoteView> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.mine(uid(jwt));
    }

    @GetMapping("/api/admin/quotes")
    @PreAuthorize("hasRole('ADMIN')")
    public List<QuoteView> list(@RequestParam(required = false) String status) {
        return service.list(status);
    }

    @PostMapping("/api/admin/quotes/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public QuoteView approve(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.approve(uid(jwt), id);
    }

    @PostMapping("/api/admin/quotes/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public QuoteView reject(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                            @Valid @RequestBody(required = false) NoteRequest r) {
        return service.reject(uid(jwt), id, r == null ? null : r.note());
    }
}
