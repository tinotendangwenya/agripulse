package com.agripulse.backend.farm;

/**
 * Controller. Only the FARMER role can use these routes, and the owner id
 * comes from the token, never from the request.
 */

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/farms")
@PreAuthorize("hasRole('FARMER')")
@RequiredArgsConstructor
public class FarmController {
    private final FarmService service;

    private static Long uid(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }

    @GetMapping
    public List<FarmResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return service.list(uid(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FarmResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody FarmRequest r) {
        return service.create(uid(jwt), r);
    }

    @GetMapping("/{id}")
    public FarmResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.get(uid(jwt), id);
    }

    @PutMapping("/{id}")
    public FarmResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                               @Valid @RequestBody FarmRequest r) {
        return service.update(uid(jwt), id, r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.delete(uid(jwt), id);
    }

    @GetMapping("/{farmId}/crops")
    public List<CropResponse> crops(@AuthenticationPrincipal Jwt jwt, @PathVariable Long farmId) {
        return service.listCrops(uid(jwt), farmId);
    }

    @PostMapping("/{farmId}/crops")
    @ResponseStatus(HttpStatus.CREATED)
    public CropResponse addCrop(@AuthenticationPrincipal Jwt jwt, @PathVariable Long farmId,
                                @Valid @RequestBody CropRequest r) {
        return service.addCrop(uid(jwt), farmId, r);
    }

    @PutMapping("/{farmId}/crops/{cropId}")
    public CropResponse updateCrop(@AuthenticationPrincipal Jwt jwt, @PathVariable Long farmId,
                                   @PathVariable Long cropId, @Valid @RequestBody CropRequest r) {
        return service.updateCrop(uid(jwt), farmId, cropId, r);
    }

    @DeleteMapping("/{farmId}/crops/{cropId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCrop(@AuthenticationPrincipal Jwt jwt, @PathVariable Long farmId,
                           @PathVariable Long cropId) {
        service.deleteCrop(uid(jwt), farmId, cropId);
    }
}
