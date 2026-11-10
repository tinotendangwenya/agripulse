package com.agripulse.backend.qa;

import com.agripulse.backend.qa.QaDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Farmers ask, experts answer. The user id and role come from the token, never from the request. */
@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QaController {
    private final QaService service;

    private static Long uid(Jwt jwt) { return Long.valueOf(jwt.getSubject()); }
    private static List<String> roles(Jwt jwt) {
        List<String> r = jwt.getClaimAsStringList("roles");
        return r == null ? List.of() : r;
    }
    private static boolean staff(Jwt jwt) { return roles(jwt).contains("EXPERT") || roles(jwt).contains("ADMIN"); }
    private static String roleOf(Jwt jwt) {
        return roles(jwt).contains("EXPERT") ? "EXPERT" : roles(jwt).contains("ADMIN") ? "ADMIN" : "FARMER";
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('FARMER')")
    public ThreadView ask(@AuthenticationPrincipal Jwt jwt,
                          @RequestParam String subject,
                          @RequestParam(required = false) String topic,
                          @RequestParam String title,
                          @RequestParam(required = false) String body,
                          @RequestParam(required = false) Long farmId,
                          @RequestParam(value = "file", required = false) MultipartFile file) {
        return service.ask(uid(jwt), farmId, subject, topic, title, body, file);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('FARMER')")
    public List<QuestionSummary> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.mine(uid(jwt));
    }

    @GetMapping("/queue")
    @PreAuthorize("hasAnyRole('EXPERT','ADMIN')")
    public List<QuestionSummary> queue(@RequestParam(required = false) String status) {
        return service.queue(status);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FARMER','EXPERT','ADMIN')")
    public ThreadView get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.get(uid(jwt), staff(jwt), id);
    }

    @PostMapping(value = "/{id}/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('FARMER','EXPERT','ADMIN')")
    public ThreadView reply(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                            @RequestParam(required = false) String body,
                            @RequestParam(value = "file", required = false) MultipartFile file) {
        return service.reply(uid(jwt), roleOf(jwt), id, body, file);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('FARMER')")
    public ThreadView close(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.close(uid(jwt), id);
    }

    @GetMapping("/{id}/messages/{messageId}/image")
    @PreAuthorize("hasAnyRole('FARMER','EXPERT','ADMIN')")
    public ResponseEntity<byte[]> image(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                        @PathVariable Long messageId) {
        ImageData img = service.image(uid(jwt), staff(jwt), id, messageId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(img.type()))
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600").body(img.bytes());
    }
}
