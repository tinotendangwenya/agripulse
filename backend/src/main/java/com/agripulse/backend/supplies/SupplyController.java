package com.agripulse.backend.supplies;

import com.agripulse.backend.supplies.SupplyDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/supplies")
@RequiredArgsConstructor
public class SupplyController {
    private final SupplyService service;

    private static Long uid(Jwt jwt) { return Long.valueOf(jwt.getSubject()); }

    // ----- Suppliers -----

    @GetMapping("/profile")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ProfileView profile(@AuthenticationPrincipal Jwt jwt) {
        return service.profile(uid(jwt));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ProfileView saveProfile(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProfileRequest r) {
        return service.saveProfile(uid(jwt), r);
    }

    @GetMapping("/products/mine")
    @PreAuthorize("hasRole('SUPPLIER')")
    public List<ProductView> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.mine(uid(jwt));
    }

    @PostMapping("/products")
    @PreAuthorize("hasRole('SUPPLIER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductView create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProductRequest r) {
        return service.create(uid(jwt), r);
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ProductView update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                              @Valid @RequestBody ProductRequest r) {
        return service.update(uid(jwt), id, r);
    }

    @PutMapping("/products/{id}/stock")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ProductView stock(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                             @Valid @RequestBody StockRequest r) {
        return service.setStock(uid(jwt), id, r.stockQty());
    }

    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasRole('SUPPLIER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.remove(uid(jwt), id);
    }

    // ----- Farmers (any logged-in user can browse) -----

    @GetMapping("/products")
    public List<ProductView> search(@RequestParam(required = false) String q,
                                    @RequestParam(required = false) String category,
                                    @RequestParam(required = false) String province,
                                    @RequestParam(required = false) String currency,
                                    @RequestParam(required = false) BigDecimal maxPrice,
                                    @RequestParam(defaultValue = "false") boolean deliveryOnly,
                                    @RequestParam(defaultValue = "price") String sort,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "30") int size) {
        return service.search(q, category, province, currency, maxPrice, deliveryOnly, sort, page, size);
    }
}