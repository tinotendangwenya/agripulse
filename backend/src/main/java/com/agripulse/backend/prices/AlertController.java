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

/** Price alerts for farmers, buyers and suppliers. The user id comes from the token. */
@RestController
@RequestMapping("/api/prices/alerts")
@PreAuthorize("hasAnyRole('FARMER','BUYER','SUPPLIER')")
@RequiredArgsConstructor
public class AlertController {
    private final PriceAlertService service;

    private static Long uid(Jwt jwt) { return Long.valueOf(jwt.getSubject()); }

    @GetMapping
    public List<AlertView> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.mine(uid(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AlertView create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AlertRequest r) {
        return service.create(uid(jwt), r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.delete(uid(jwt), id);
    }
}
