package com.agripulse.backend.market;

import com.agripulse.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductService {
    static final Set<String> CURRENCIES = Set.of("USD", "ZIG", "ZAR");
    static final Set<String> UNITS = Set.of("KG", "TONNE", "BAG", "LITRE", "CRATE", "HEAD", "DOZEN");
    static final Set<String> CATEGORIES = Set.of("CROP", "LIVESTOCK", "POULTRY", "FISH", "OTHER");
    private static final BigDecimal NO_LIMIT = new BigDecimal("999999999999");

    private final ProductRepository products;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public List<ProductResponse> search(String q, String province, String currency,
                                        BigDecimal maxPrice, int page, int size) {
        String cur = blank(currency) ? "" : currency.trim().toUpperCase();
        if (!cur.isEmpty() && !CURRENCIES.contains(cur)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency must be one of " + CURRENCIES);
        }
        var paging = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        return products.search(blank(q) ? "" : q.trim(), blank(province) ? "" : province.trim(),
                        cur, maxPrice == null ? NO_LIMIT : maxPrice, paging)
                .stream().map(ProductResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long callerId, Long id) {
        Product p = products.findById(id).orElseThrow(this::notFound);
        boolean mine = p.getSeller().getId().equals(callerId);
        if (!mine && !"ACTIVE".equals(p.getStatus())) throw notFound();
        return ProductResponse.of(p);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> mine(Long sellerId) {
        return products.findBySellerIdAndStatusNotOrderByIdDesc(sellerId, "REMOVED")
                .stream().map(ProductResponse::of).toList();
    }

    @Transactional
    public ProductResponse create(Long sellerId, ProductRequest r) {
        Product p = new Product();
        p.setSeller(users.getReferenceById(sellerId));
        apply(p, r);
        return ProductResponse.of(products.save(p));
    }

    @Transactional
    public ProductResponse update(Long sellerId, Long id, ProductRequest r) {
        Product p = products.findByIdAndSellerId(id, sellerId).orElseThrow(this::notFound);
        if ("REMOVED".equals(p.getStatus())) throw notFound();
        apply(p, r);
        return ProductResponse.of(p);
    }

    // Soft delete: keeps order history intact
    @Transactional
    public void remove(Long sellerId, Long id) {
        Product p = products.findByIdAndSellerId(id, sellerId).orElseThrow(this::notFound);
        p.setStatus("REMOVED");
    }

    private void apply(Product p, ProductRequest r) {
        String category = r.category().trim().toUpperCase();
        String unit = r.unit().trim().toUpperCase();
        String currency = r.currency().trim().toUpperCase();
        if (!CATEGORIES.contains(category))
            throw bad("Category must be one of " + CATEGORIES);
        if (!UNITS.contains(unit))
            throw bad("Unit must be one of " + UNITS);
        if (!CURRENCIES.contains(currency))
            throw bad("Currency must be one of " + CURRENCIES);
        String province = FarmProvinces.match(r.province())
                .orElseThrow(() -> bad("Province must be one of " + FarmProvinces.ALL));
        p.setName(r.name().trim());
        p.setCategory(category);
        p.setDescription(r.description());
        p.setQuantity(r.quantity());
        p.setUnit(unit);
        p.setPrice(r.price());
        p.setCurrency(currency);
        p.setProvince(province);
        p.setDistrict(r.district());
        p.setAvailableFrom(r.availableFrom());
        p.setStatus("ACTIVE");
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }
    private ResponseStatusException notFound() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"); }
    private static ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
}
