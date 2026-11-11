package com.agripulse.backend.transport;

import com.agripulse.backend.transport.TransportDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Farmers, buyers and suppliers post jobs and accept quotes. Transporters quote and update the job. User id comes from the token. */
@RestController
@RequestMapping("/api/transport")
@RequiredArgsConstructor
public class TransportController {
    private static final String REQUESTER = "hasAnyRole('FARMER','BUYER','SUPPLIER')";
    private final TransportService service;

    private static Long uid(Jwt jwt) { return Long.valueOf(jwt.getSubject()); }

    // ----- requester -----
    @PostMapping("/requests")
    @PreAuthorize(REQUESTER)
    @ResponseStatus(HttpStatus.CREATED)
    public RequestView create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RequestCreate r) {
        return service.create(uid(jwt), r);
    }

    @GetMapping("/requests/mine")
    @PreAuthorize(REQUESTER)
    public List<RequestView> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.mine(uid(jwt));
    }

    @GetMapping("/requests/{id}")
    @PreAuthorize(REQUESTER)
    public RequestDetail detail(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.detail(uid(jwt), id);
    }

    @PostMapping("/requests/{id}/cancel")
    @PreAuthorize(REQUESTER)
    public RequestView cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.cancel(uid(jwt), id);
    }

    @PostMapping("/requests/{id}/quotes/{quoteId}/accept")
    @PreAuthorize(REQUESTER)
    public QuoteView accept(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @PathVariable Long quoteId) {
        return service.accept(uid(jwt), id, quoteId);
    }

    @PostMapping("/requests/{id}/quotes/{quoteId}/reject")
    @PreAuthorize(REQUESTER)
    public QuoteView reject(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @PathVariable Long quoteId) {
        return service.reject(uid(jwt), id, quoteId);
    }

    // ----- transporter -----
    @GetMapping("/jobs")
    @PreAuthorize("hasRole('TRANSPORTER')")
    public List<Job> jobs(@AuthenticationPrincipal Jwt jwt) {
        return service.jobs(uid(jwt));
    }

    @PostMapping("/requests/{id}/quotes")
    @PreAuthorize("hasRole('TRANSPORTER')")
    @ResponseStatus(HttpStatus.CREATED)
    public QuoteView quote(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody QuoteCreate c) {
        return service.quote(uid(jwt), id, c);
    }

    @GetMapping("/quotes/mine")
    @PreAuthorize("hasRole('TRANSPORTER')")
    public List<QuoteView> myQuotes(@AuthenticationPrincipal Jwt jwt) {
        return service.myQuotes(uid(jwt));
    }

    @PostMapping("/quotes/{quoteId}/withdraw")
    @PreAuthorize("hasRole('TRANSPORTER')")
    public QuoteView withdraw(@AuthenticationPrincipal Jwt jwt, @PathVariable Long quoteId) {
        return service.withdraw(uid(jwt), quoteId);
    }

    @PostMapping("/requests/{id}/status")
    @PreAuthorize("hasRole('TRANSPORTER')")
    public RequestView status(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody StatusRequest r) {
        return service.setStatus(uid(jwt), id, r.status());
    }
}
