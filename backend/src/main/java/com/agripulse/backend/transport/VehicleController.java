package com.agripulse.backend.transport;

import com.agripulse.backend.transport.TransportDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Transporters manage their own vehicles. TRANSPORTER role only. */
@RestController
@RequestMapping("/api/transport/vehicles")
@PreAuthorize("hasRole('TRANSPORTER')")
@RequiredArgsConstructor
public class VehicleController {
    private final VehicleService service;

    private static Long uid(Jwt jwt) { return Long.valueOf(jwt.getSubject()); }

    @GetMapping
    public List<VehicleView> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.mine(uid(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleView create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody VehicleRequest r) {
        return service.create(uid(jwt), r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.remove(uid(jwt), id);
    }
}
