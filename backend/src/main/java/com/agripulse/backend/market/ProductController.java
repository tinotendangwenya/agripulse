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

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService service;

    private static Long uid(Jwt jwt) { return Long.valueOf(jwt.getSubject()); }

    // Any logged-in user can browse
    @GetMapping
    public List<ProductResponse> search(@RequestParam(required = false) String q,
                                        @RequestParam(required = false) String province,
                                        @RequestParam(required = false) String currency,
                                        @RequestParam(required = false) BigDecimal maxPrice,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return service.search(q, province, currency, maxPrice, page, size);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('FARMER')")
    public List<ProductResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.mine(uid(jwt));
    }

    @GetMapping("/{id}")
    public ProductResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.get(uid(jwt), id);
    }

    @PostMapping
    @PreAuthorize("hasRole('FARMER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProductRequest r) {
        return service.create(uid(jwt), r);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('FARMER')")
    public ProductResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                  @Valid @RequestBody ProductRequest r) {
        return service.update(uid(jwt), id, r);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('FARMER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.remove(uid(jwt), id);
    }
}
