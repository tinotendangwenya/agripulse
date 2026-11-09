package com.agripulse.backend.prices;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Reading prices: any logged-in user. Writing: ADMIN only (enforced by /api/admin/** in SecurityConfig). */
@RestController
@RequiredArgsConstructor
public class PriceController {
    private final PriceService service;

    @GetMapping("/api/prices/commodities")
    public List<String> commodities() {
        return service.commodities();
    }

    @GetMapping("/api/prices/compare")
    public List<PriceResponse> compare(@RequestParam String commodity, @RequestParam String currency) {
        return service.compare(commodity, currency);
    }

    @GetMapping("/api/prices/history")
    public List<PriceResponse> history(@RequestParam String commodity,
                                       @RequestParam(defaultValue = "90") int days) {
        return service.history(commodity, days);
    }

    @PostMapping("/api/admin/prices")
    @ResponseStatus(HttpStatus.CREATED)
    public PriceResponse add(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PriceRequest r) {
        return service.add(Long.valueOf(jwt.getSubject()), r);
    }

    @PutMapping("/api/admin/prices/{id}")
    public PriceResponse update(@PathVariable Long id, @Valid @RequestBody PriceRequest r) {
        return service.update(id, r);
    }

    @DeleteMapping("/api/admin/prices/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
