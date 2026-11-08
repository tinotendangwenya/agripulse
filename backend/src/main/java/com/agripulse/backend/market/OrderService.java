package com.agripulse.backend.market;

import com.agripulse.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orders;
    private final ProductRepository products;
    private final UserRepository users;

    @Transactional
    public OrderResponse place(Long buyerId, OrderRequest r) {
        Product p = products.findById(r.productId()).orElseThrow(this::notFound);
        if (!"ACTIVE".equals(p.getStatus())) throw notFound();
        if (r.quantity().compareTo(p.getQuantity()) > 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only " + p.getQuantity() + " " + p.getUnit() + " available");

        Order o = new Order();
        o.setProduct(p);
        o.setBuyer(users.getReferenceById(buyerId));
        o.setQuantity(r.quantity());
        o.setUnitPrice(p.getPrice());                 // price snapshot
        o.setCurrency(p.getCurrency());               // currency snapshot
        o.setTotalPrice(p.getPrice().multiply(r.quantity()).setScale(2, RoundingMode.HALF_UP));
        o.setNote(r.note());
        return OrderResponse.of(orders.save(o));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> myOrders(Long buyerId) {
        return orders.findByBuyerIdOrderByIdDesc(buyerId).stream().map(OrderResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> incoming(Long sellerId) {
        return orders.findByProductSellerIdOrderByIdDesc(sellerId).stream().map(OrderResponse::of).toList();
    }

    // Buyer withdraws a request that the farmer hasn't confirmed yet
    @Transactional
    public OrderResponse cancel(Long buyerId, Long id) {
        Order o = orders.findByIdAndBuyerId(id, buyerId).orElseThrow(this::notFound);
        require(o, "PENDING");
        o.setStatus("CANCELLED");
        return OrderResponse.of(o);
    }

    // Stock is reduced here, under a row lock, so a listing can't be oversold
    @Transactional
    public OrderResponse confirm(Long sellerId, Long id) {
        Order o = orders.findByIdAndProductSellerId(id, sellerId).orElseThrow(this::notFound);
        require(o, "PENDING");
        Product p = products.findForUpdate(o.getProduct().getId()).orElseThrow(this::notFound);
        if (p.getQuantity().compareTo(o.getQuantity()) < 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough stock left");
        p.setQuantity(p.getQuantity().subtract(o.getQuantity()));
        if (p.getQuantity().compareTo(BigDecimal.ZERO) == 0) p.setStatus("SOLD_OUT");
        o.setStatus("CONFIRMED");
        return OrderResponse.of(o);
    }

    @Transactional
    public OrderResponse reject(Long sellerId, Long id) {
        Order o = orders.findByIdAndProductSellerId(id, sellerId).orElseThrow(this::notFound);
        require(o, "PENDING");
        o.setStatus("REJECTED");
        return OrderResponse.of(o);
    }

    @Transactional
    public OrderResponse complete(Long sellerId, Long id) {
        Order o = orders.findByIdAndProductSellerId(id, sellerId).orElseThrow(this::notFound);
        require(o, "CONFIRMED");
        o.setStatus("COMPLETED");
        return OrderResponse.of(o);
    }

    private void require(Order o, String status) {
        if (!status.equals(o.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Order is " + o.getStatus() + ", expected " + status);
    }

    private ResponseStatusException notFound() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"); }
}
