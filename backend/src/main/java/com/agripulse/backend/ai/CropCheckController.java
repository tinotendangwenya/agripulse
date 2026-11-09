package com.agripulse.backend.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** Leaf check. FARMER only, and the user id comes from the token. Results are preliminary, never a diagnosis. */
@RestController
@RequestMapping("/api/ai")
@PreAuthorize("hasRole('FARMER')")
@RequiredArgsConstructor
public class CropCheckController {
    private final CropCheckService service;

    @PostMapping(value = "/crop-check", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CropCheckResponse check(@AuthenticationPrincipal Jwt jwt,
                                   @RequestParam("file") MultipartFile file,
                                   @RequestParam("crop") String crop,
                                   @RequestParam(value = "farmId", required = false) Long farmId) {
        return service.check(Long.valueOf(jwt.getSubject()), farmId, crop, file);
    }
}
