package com.agripulse.backend.records;

import com.agripulse.backend.records.RecordsDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Farm records. FARMER only, and the owner id comes from the token. */
@RestController
@RequestMapping("/api/farms/{farmId}/records")
@PreAuthorize("hasRole('FARMER')")
@RequiredArgsConstructor
public class RecordsController {
    private final RecordsService service;

    private static Long uid(Jwt jwt) { return Long.valueOf(jwt.getSubject()); }

    @GetMapping("/transactions")
    public List<TransactionView> transactions(@AuthenticationPrincipal Jwt jwt, @PathVariable Long farmId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.transactions(uid(jwt), farmId, from, to);
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionView addTransaction(@AuthenticationPrincipal Jwt jwt, @PathVariable Long farmId,
                                          @Valid @RequestBody TransactionRequest r) {
        return service.addTransaction(uid(jwt), farmId, r);
    }

    @DeleteMapping("/transactions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTransaction(@AuthenticationPrincipal Jwt jwt, @PathVariable Long farmId, @PathVariable Long id) {
        service.deleteTransaction(uid(jwt), farmId, id);
    }

    @GetMapping("/harvests")
    public List<HarvestView> harvests(@AuthenticationPrincipal Jwt jwt, @PathVariable Long farmId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.harvests(uid(jwt), farmId, from, to);
    }

    @PostMapping("/harvests")
    @ResponseStatus(HttpStatus.CREATED)
    public HarvestView addHarvest(@AuthenticationPrincipal Jwt jwt, @PathVariable Long farmId,
                                  @Valid @RequestBody HarvestRequest r) {
        return service.addHarvest(uid(jwt), farmId, r);
    }

    @DeleteMapping("/harvests/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHarvest(@AuthenticationPrincipal Jwt jwt, @PathVariable Long farmId, @PathVariable Long id) {
        service.deleteHarvest(uid(jwt), farmId, id);
    }

    @GetMapping("/livestock")
    public List<LivestockView> livestock(@AuthenticationPrincipal Jwt jwt, @PathVariable Long farmId) {
        return service.livestock(uid(jwt), farmId);
    }

    @PostMapping("/livestock")
    @ResponseStatus(HttpStatus.CREATED)
    public LivestockView addLivestock(@AuthenticationPrincipal Jwt jwt, @PathVariable Long farmId,
                                      @Valid @RequestBody LivestockRequest r) {
        return service.addLivestock(uid(jwt), farmId, r);
    }

    @DeleteMapping("/livestock/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLivestock(@AuthenticationPrincipal Jwt jwt, @PathVariable Long farmId, @PathVariable Long id) {
        service.deleteLivestock(uid(jwt), farmId, id);
    }

    @GetMapping("/summary")
    public Summary summary(@AuthenticationPrincipal Jwt jwt, @PathVariable Long farmId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.summary(uid(jwt), farmId, from, to);
    }
}
