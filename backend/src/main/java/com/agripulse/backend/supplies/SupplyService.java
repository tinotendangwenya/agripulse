package com.agripulse.backend.supplies;

import com.agripulse.backend.supplies.SupplyDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplyService {
    static final List<String> CATEGORIES = List.of("SEEDS", "FERTILISERS", "CHEMICALS", "IRRIGATION",
            "EQUIPMENT", "LIVESTOCK", "POULTRY", "PROTECTIVE", "GENERAL");
    static final List<String> UNITS = List.of("KG", "TONNE", "BAG", "LITRE", "PIECE", "PACK", "ROLL", "METRE", "SET");
    static final List<String> CURRENCIES = List.of("USD", "ZIG", "ZAR");
    static final List<String> PROVINCES = List.of("Harare", "Bulawayo", "Manicaland", "Mashonaland Central",
            "Mashonaland East", "Mashonaland West", "Masvingo", "Matabeleland North", "Matabeleland South", "Midlands");
    private static final BigDecimal NO_LIMIT = new BigDecimal("999999999999");

    private final SupplierProfileRepository profiles;
    private final SupplyProductRepository products;

    // ---------- Supplier profile ----------

    @Transactional(readOnly = true)
    public ProfileView profile(Long uid) {
        return profiles.findById(uid).map(ProfileView::of).orElse(null);
    }

    @Transactional
    public ProfileView saveProfile(Long uid, ProfileRequest r) {
        SupplierProfile p = profiles.findById(uid).orElseGet(() -> {
            SupplierProfile n = new SupplierProfile();
            n.setUserId(uid);
            return n;
        });
        p.setBusinessName(r.businessName().trim());
        p.setProvince(province(r.province()));
        p.setDistrict(blank(r.district()) ? null : r.district().trim());
        p.setAddress(blank(r.address()) ? null : r.address().trim());
        p.setPhone(r.phone().trim());
        p.setDescription(blank(r.description()) ? null : r.description().trim());
        p.setOffersDelivery(r.offersDelivery());
        return ProfileView.of(profiles.save(p));
    }

    // ---------- Supplier products ----------

    @Transactional(readOnly = true)
    public List<ProductView> mine(Long uid) {
        return products.findBySupplier_UserIdAndStatusNotOrderByIdDesc(uid, "REMOVED")
                .stream().map(ProductView::of).toList();
    }

    @Transactional
    public ProductView create(Long uid, ProductRequest r) {
        SupplierProfile s = profiles.findById(uid)
                .orElseThrow(() -> bad("Create your business profile first"));
        SupplyProduct p = new SupplyProduct();
        p.setSupplier(s);
        apply(p, r);
        return ProductView.of(products.save(p));
    }

    @Transactional
    public ProductView update(Long uid, Long id, ProductRequest r) {
        SupplyProduct p = owned(uid, id);
        apply(p, r);
        return ProductView.of(p);
    }

    @Transactional
    public ProductView setStock(Long uid, Long id, BigDecimal qty) {
        SupplyProduct p = owned(uid, id);
        p.setStockQty(qty);
        return ProductView.of(p);
    }

    // Soft delete, so future order history stays intact
    @Transactional
    public void remove(Long uid, Long id) {
        owned(uid, id).setStatus("REMOVED");
    }

    // ---------- Farmer search and price comparison ----------

    @Transactional(readOnly = true)
    public List<ProductView> search(String q, String category, String province, String currency,
                                    BigDecimal maxPrice, boolean deliveryOnly, String sort, int page, int size) {
        String cat = blank(category) ? "" : category.trim().toUpperCase();
        if (!cat.isEmpty() && !CATEGORIES.contains(cat)) throw bad("Category must be one of " + CATEGORIES);
        String cur = blank(currency) ? "" : currency.trim().toUpperCase();
        if (!cur.isEmpty() && !CURRENCIES.contains(cur)) throw bad("Currency must be one of " + CURRENCIES);
        String prov = blank(province) ? "" : province(province);

        Sort order = "newest".equalsIgnoreCase(sort)
                ? Sort.by(Sort.Direction.DESC, "id")
                : Sort.by(Sort.Direction.ASC, "price").and(Sort.by(Sort.Direction.DESC, "id"));
        var paging = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), order);

        return products.search(blank(q) ? "" : q.trim(), cat, prov, cur, deliveryOnly,
                        maxPrice == null ? NO_LIMIT : maxPrice, paging)
                .stream().map(ProductView::of).toList();
    }

    // ---------- helpers ----------

    private SupplyProduct owned(Long uid, Long id) {
        SupplyProduct p = products.findByIdAndSupplier_UserId(id, uid).orElseThrow(SupplyService::notFound);
        if ("REMOVED".equals(p.getStatus())) throw notFound();
        return p;
    }

    private void apply(SupplyProduct p, ProductRequest r) {
        String category = r.category().trim().toUpperCase();
        String unit = r.unit().trim().toUpperCase();
        String currency = r.currency().trim().toUpperCase();
        if (!CATEGORIES.contains(category)) throw bad("Category must be one of " + CATEGORIES);
        if (!UNITS.contains(unit)) throw bad("Unit must be one of " + UNITS);
        if (!CURRENCIES.contains(currency)) throw bad("Currency must be one of " + CURRENCIES);
        if (category.equals("CHEMICALS") && blank(r.registrationNo()))
            throw bad("Chemicals need a registration number, as printed on the product label");
        if (r.expiryDate() != null && r.expiryDate().isBefore(LocalDate.now()))
            throw bad("The expiry date is already in the past");

        p.setName(r.name().trim());
        p.setBrand(blank(r.brand()) ? null : r.brand().trim());
        p.setCategory(category);
        p.setDescription(blank(r.description()) ? null : r.description().trim());
        p.setUnit(unit);
        p.setPrice(r.price());
        p.setCurrency(currency);
        p.setStockQty(r.stockQty());
        p.setRegistrationNo(blank(r.registrationNo()) ? null : r.registrationNo().trim());
        p.setExpiryDate(r.expiryDate());
        p.setStatus("ACTIVE");
    }

    private static String province(String s) {
        return PROVINCES.stream().filter(x -> x.equalsIgnoreCase(s.trim())).findFirst()
                .orElseThrow(() -> bad("Province must be one of " + PROVINCES));
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }
    private static ResponseStatusException notFound() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"); }
    private static ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
}