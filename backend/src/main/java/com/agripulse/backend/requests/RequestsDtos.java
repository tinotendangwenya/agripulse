package com.agripulse.backend.requests;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class RequestsDtos {
    private RequestsDtos() {}

    public record RequestCreate(
            @NotBlank @Size(max = 100) String productName,
            @NotBlank String category,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantity,
            @NotBlank String unit,
            @DecimalMin(value = "0.0", inclusive = false) BigDecimal maxPrice,
            @NotBlank String currency,
            @NotBlank String province,
            @Size(max = 80) String district,
            LocalDate neededBy,
            @Size(max = 500) String note) {}

    public record RequestView(Long id, String buyerName, String productName, String category, BigDecimal quantity,
                              String unit, BigDecimal maxPrice, String currency, String province, String district,
                              LocalDate neededBy, String note, String status, LocalDateTime createdAt,
                              long pendingOffers) {}

    public record ListingMatch(Long productId, String sellerName, String name, BigDecimal quantity, String unit,
                               BigDecimal price, String currency, String province, String district,
                               Double distanceKm, boolean coversQuantity, boolean nearest, boolean cheapest) {}

    public record OfferRequest(@NotNull Long productId,
                               @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantity,
                               @DecimalMin(value = "0.0", inclusive = false) BigDecimal unitPrice,
                               @Size(max = 500) String message) {}

    public record OfferView(Long id, Long requestId, String requestProduct, String farmerName, Long productId,
                            String productName, String listingProvince, BigDecimal quantity, String unit,
                            BigDecimal unitPrice, String currency, BigDecimal total, String message,
                            String status, Long orderId, LocalDateTime createdAt) {}

    public record RequestDetail(RequestView request, List<OfferView> offers, List<ListingMatch> matchingListings) {}

    public record MyListing(Long productId, String name, BigDecimal quantity, String unit, BigDecimal price,
                            String currency, boolean coversQuantity) {}

    public record FarmerMatch(RequestView request, List<MyListing> myListings) {}
}
