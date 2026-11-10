package com.agripulse.backend.requests;

import com.agripulse.backend.requests.RequestsDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Buyers post requests and accept offers. Farmers browse requests and make offers. The user id comes from the token. */
@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class RequestController {
    private final RequestService service;

    private static Long uid(Jwt jwt) { return Long.valueOf(jwt.getSubject()); }

    // ----- buyer -----
    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    @ResponseStatus(HttpStatus.CREATED)
    public RequestView create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RequestCreate r) {
        return service.create(uid(jwt), r);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('BUYER')")
    public List<RequestView> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.mine(uid(jwt));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('BUYER')")
    public RequestDetail detail(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.detail(uid(jwt), id);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('BUYER')")
    public RequestView close(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.close(uid(jwt), id);
    }

    @PostMapping("/{id}/offers/{offerId}/accept")
    @PreAuthorize("hasRole('BUYER')")
    public OfferView accept(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @PathVariable Long offerId) {
        return service.accept(uid(jwt), id, offerId);
    }

    @PostMapping("/{id}/offers/{offerId}/reject")
    @PreAuthorize("hasRole('BUYER')")
    public OfferView reject(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @PathVariable Long offerId) {
        return service.reject(uid(jwt), id, offerId);
    }

    // ----- farmer -----
    @GetMapping("/open")
    @PreAuthorize("hasRole('FARMER')")
    public List<RequestView> open(@RequestParam(required = false) String q,
                                  @RequestParam(required = false) String province) {
        return service.open(q, province);
    }

    @GetMapping("/matches")
    @PreAuthorize("hasRole('FARMER')")
    public List<FarmerMatch> matches(@AuthenticationPrincipal Jwt jwt) {
        return service.myMatches(uid(jwt));
    }

    @PostMapping("/{id}/offers")
    @PreAuthorize("hasRole('FARMER')")
    @ResponseStatus(HttpStatus.CREATED)
    public OfferView offer(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody OfferRequest r) {
        return service.makeOffer(uid(jwt), id, r);
    }

    @GetMapping("/offers/mine")
    @PreAuthorize("hasRole('FARMER')")
    public List<OfferView> myOffers(@AuthenticationPrincipal Jwt jwt) {
        return service.myOffers(uid(jwt));
    }

    @PostMapping("/offers/{offerId}/withdraw")
    @PreAuthorize("hasRole('FARMER')")
    public OfferView withdraw(@AuthenticationPrincipal Jwt jwt, @PathVariable Long offerId) {
        return service.withdraw(uid(jwt), offerId);
    }
}
