package com.agripulse.backend.weather;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** Weather for a farm. FARMER role only, and the owner id comes from the token. */
@RestController
@RequestMapping("/api/farms/{farmId}/weather")
@PreAuthorize("hasRole('FARMER')")
@RequiredArgsConstructor
public class WeatherController {
    private final WeatherService service;

    @GetMapping
    public WeatherResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long farmId) {
        return service.forFarm(Long.valueOf(jwt.getSubject()), farmId);
    }
}
