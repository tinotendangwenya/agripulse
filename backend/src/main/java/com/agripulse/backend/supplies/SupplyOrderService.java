package com.agripulse.backend.supplies;

import com.agripulse.backend.supplies.SupplyOrderDtos.*;
import com.agripulse.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SupplyOrderService {
    static final Set<String> DELIVERY = Set.of("PICKUP", "DELIVERY");
    static final Set<String> PAYMENT = Set.of("CASH", "MOBILE_MONEY", "BANK_TRANSFER");

    private final SupplyOrderRepository orders;
    private final SupplyProductRepository products;
    private final UserRepository users;

    // ---------- Farmer ----------

    @Transactional
    public OrderView place(Long buyerId, OrderRequest r) {
        SupplyProduct p = products.findById(r.productId()).orElseThrow(SupplyOrderService::notFound);
        if (!"ACTIVE".equals(p.getStatus())) throw notFound();

        String delivery = r.deliveryMethod().trim().toUpperCase();
        String payment = r.paymentMethod().trim().toUpperCase();
        if (!DELIVERY.contains(delivery)) throw bad("Delivery method must be one of " + DELIVERY);
        if (!PAYMENT.contains(payment)) throw bad("Payment method must be one of " + PAYMENT);

        SupplierProfile s = p.getSupplier();
        if (delivery.equals("DELIVERY")) {
            if (!s.isOffersDelivery()) throw bad("This supplier does not deliver. Choose pickup.");
            if (blank(r.deliveryAddress())) throw bad("Enter a delivery address");
        }
        if (r.quantity().compareTo(p.getStockQty()) > 0)
            throw bad("Only " + p.getStockQty().toPlainString() + " " + p.getUnit() + " in stock");

        SupplyOrder o = new SupplyOrder();
        o.setProduct(p);
        o.setSupplier(s);
        o.setBuyer(users.getReferenceById(buyerId));
        o.setProductName(p.getName());
        o.setUnit(p.getUnit());
        o.setQuantity(r.quantity());
        o.setUnitPrice(p.getPrice());
        o.setCurrency(p.getCurrency());
        o.setTotalPrice(p.getPrice().multiply(r.quantity()).setScale(2, RoundingMode.HALF_UP));
        o.setDeliveryMethod(delivery);
        o.setDeliveryAddress(delivery.equals("DELIVERY") ? r.deliveryAddress().trim() : null);
        o.setPaymentMethod(payment);
        o.setNote(blank(r.note()) ? null : r.note().trim());
        return OrderView.of(orders.save(o));
    }

    @Transactional(readOnly = true)
    public List<OrderView> mine(Long buyerId) {
        return orders.findByBuyer_IdOrderByIdDesc(buyerId).stream().map(OrderView::of).toList();
    }

    @Transactional
    public OrderView cancel(Long buyerId, Long id) {
        SupplyOrder o = orders.lockForBuyer(id, buyerId).orElseThrow(SupplyOrderService::notFound);
        require(o, "PENDING");
        o.setStatus("CANCELLED");
        return OrderView.of(o);
    }

    // ---------- Supplier ----------

    @Transactional(readOnly = true)
    public List<OrderView> incoming(Long supplierId) {
        return orders.findBySupplier_UserIdOrderByIdDesc(supplierId).stream().map(OrderView::of).toList();
    }

    // Confirming is the moment stock goes down
    @Transactional
    public OrderView confirm(Long supplierId, Long id) {
        SupplyOrder o = supplierOrder(supplierId, id);
        require(o, "PENDING");
        SupplyProduct p = products.lockById(o.getProduct().getId()).orElseThrow(SupplyOrderService::notFound);
        if (o.getQuantity().compareTo(p.getStockQty()) > 0)
            throw bad("Not enough stock to confirm. In stock: " + p.getStockQty().toPlainString() + " " + p.getUnit());
        p.setStockQty(p.getStockQty().subtract(o.getQuantity()));
        o.setStatus("CONFIRMED");
        return OrderView.of(o);
    }

    @Transactional
    public OrderView reject(Long supplierId, Long id) {
        SupplyOrder o = supplierOrder(supplierId, id);
        require(o, "PENDING");
        o.setStatus("REJECTED");
        return OrderView.of(o);
    }

    @Transactional
    public OrderView dispatch(Long supplierId, Long id) {
        SupplyOrder o = supplierOrder(supplierId, id);
        require(o, "CONFIRMED");
        if (!o.getDeliveryMethod().equals("DELIVERY"))
            throw bad("This is a pickup order, there is nothing to dispatch");
        o.setStatus("DISPATCHED");
        return OrderView.of(o);
    }

    @Transactional
    public OrderView complete(Long supplierId, Long id) {
        SupplyOrder o = supplierOrder(supplierId, id);
        require(o, "CONFIRMED", "DISPATCHED");
        if (o.getDeliveryMethod().equals("DELIVERY") && o.getStatus().equals("CONFIRMED"))
            throw bad("Mark the order as dispatched first");
        o.setStatus("COMPLETED");
        return OrderView.of(o);
    }

    @Transactional
    public OrderView markPaid(Long supplierId, Long id) {
        SupplyOrder o = supplierOrder(supplierId, id);
        require(o, "CONFIRMED", "DISPATCHED", "COMPLETED");
        o.setPaymentStatus("PAID");
        return OrderView.of(o);
    }

    // ---------- helpers ----------

    private SupplyOrder supplierOrder(Long supplierId, Long id) {
        return orders.lockForSupplier(id, supplierId).orElseThrow(SupplyOrderService::notFound);
    }

    private static void require(SupplyOrder o, String... allowed) {
        for (String a : allowed) if (a.equals(o.getStatus())) return;
        throw bad("This order is " + o.getStatus() + ", so that action is not possible");
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }
    private static ResponseStatusException notFound() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"); }
    private static ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
}