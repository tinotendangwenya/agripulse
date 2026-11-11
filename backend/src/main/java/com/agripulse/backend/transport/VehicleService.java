package com.agripulse.backend.transport;

import com.agripulse.backend.transport.TransportDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/** Transporters manage their own vehicles. Every lookup is by the caller's id, so others get 404. */
@Service
@RequiredArgsConstructor
public class VehicleService {
    static final Set<String> TYPES = Set.of("PICKUP", "LIGHT_TRUCK", "TRUCK", "TRUCK_TRAILER",
            "LIVESTOCK_TRUCK", "REFRIGERATED", "TRACTOR_TRAILER", "OTHER");
    static final List<String> CARGO_ORDER = List.of("PRODUCE", "LIVESTOCK", "INPUTS", "OTHER");
    static final Set<String> CURRENCIES = Set.of("USD", "ZIG", "ZAR");
    private static final int MAX_ACTIVE = 20;

    private final VehicleRepository vehicles;

    @Transactional(readOnly = true)
    public List<VehicleView> mine(Long ownerId) {
        return vehicles.findByOwnerIdAndStatusNotOrderByIdDesc(ownerId, "REMOVED").stream().map(VehicleService::view).toList();
    }

    @Transactional
    public VehicleView create(Long ownerId, VehicleRequest r) {
        String type = up(r.vehicleType()), unit = up(r.capacityUnit()), cur = up(r.currency());
        if (!TYPES.contains(type)) throw bad("Vehicle type must be one of " + TYPES);
        if (!unit.equals("KG") && !unit.equals("TONNE")) throw bad("Capacity unit must be KG or TONNE");
        if (!CURRENCIES.contains(cur)) throw bad("Currency must be one of " + CURRENCIES);
        String province = Geo.province(r.baseProvince());
        if (province == null) throw bad("Unknown province");
        Set<String> chosen = Set.copyOf(r.cargoTypes().stream().map(VehicleService::up).toList());
        if (!CARGO_ORDER.containsAll(chosen)) throw bad("Cargo types must be from " + CARGO_ORDER);
        String cargo = String.join(",", CARGO_ORDER.stream().filter(chosen::contains).toList());
        if (vehicles.countByOwnerIdAndStatus(ownerId, "ACTIVE") >= MAX_ACTIVE)
            throw bad("You already have " + MAX_ACTIVE + " vehicles. Remove some first.");

        Vehicle v = new Vehicle();
        v.setOwnerId(ownerId);
        v.setVehicleType(type);
        v.setDescription(r.description().trim());
        v.setCapacityKg(r.capacity().multiply(unit.equals("TONNE") ? BigDecimal.valueOf(1000) : BigDecimal.ONE)
                .setScale(2, RoundingMode.HALF_UP));
        v.setCargoTypes(cargo);
        v.setBaseProvince(province);
        v.setBaseDistrict(r.baseDistrict());
        v.setRatePerKm(r.ratePerKm());
        v.setMinCharge(r.minCharge() == null ? BigDecimal.ZERO : r.minCharge());
        v.setCurrency(cur);
        return view(vehicles.save(v));
    }

    @Transactional
    public void remove(Long ownerId, Long id) {
        vehicles.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found")).setStatus("REMOVED");
    }

    static VehicleView view(Vehicle v) {
        return new VehicleView(v.getId(), v.getVehicleType(), v.getDescription(), v.getCapacityKg(),
                Arrays.asList(v.getCargoTypes().split(",")), v.getBaseProvince(), v.getBaseDistrict(),
                v.getRatePerKm(), v.getMinCharge(), v.getCurrency(), v.getStatus());
    }

    private static String up(String s) { return s == null ? "" : s.trim().toUpperCase(); }
    private static ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
}
