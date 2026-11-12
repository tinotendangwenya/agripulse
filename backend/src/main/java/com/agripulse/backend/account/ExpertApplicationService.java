package com.agripulse.backend.account;

import com.agripulse.backend.account.AccountDtos.*;
import com.agripulse.backend.notifications.NotificationService;
import com.agripulse.backend.user.RoleRepository;
import com.agripulse.backend.user.User;
import com.agripulse.backend.user.UserRepository;
import com.agripulse.backend.weather.ProvinceLocations;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Expert applications. An application creates a DISABLED expert account. Only an admin decision enables it.
 * AgriPulse cannot verify qualifications by itself: the admin checks them outside the app before approving.
 */
@Service
@RequiredArgsConstructor
public class ExpertApplicationService {
    private static final Set<String> FILTERS = Set.of("PENDING", "APPROVED", "REJECTED", "ALL");

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final ExpertApplicationRepository applications;
    private final NotificationService notifier;
    private final AuditService audit;

    @Transactional
    public ApplyResult apply(ApplyRequest r) {
        if (!Boolean.TRUE.equals(r.acceptedPrivacy())) throw bad("Please accept the privacy notice to apply");
        String email = blankToNull(r.email());
        String phone = blankToNull(r.phone());
        if (email == null && phone == null) throw bad("Provide an email or a phone number");
        String province = ProvinceLocations.CENTRES.keySet().stream()
                .filter(p -> p.equalsIgnoreCase(r.province().trim())).findFirst()
                .orElseThrow(() -> bad("Unknown province"));
        if (email != null) {
            email = email.toLowerCase();
            if (users.existsByEmailIgnoreCase(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        if (phone != null && users.existsByPhone(phone))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone already registered");

        User u = new User();
        u.setFullName(r.fullName().trim());
        u.setEmail(email);
        u.setPhone(phone);
        u.setPasswordHash(encoder.encode(r.password()));
        u.setRole(roles.findByName("EXPERT").orElseThrow());
        u.setEnabled(false);
        u.setConsentAt(LocalDateTime.now());
        users.save(u);

        ExpertApplication a = new ExpertApplication();
        a.setUserId(u.getId());
        a.setQualification(r.qualification().trim());
        a.setOrganisation(blankToNull(r.organisation()));
        a.setRegistrationNumber(blankToNull(r.registrationNumber()));
        a.setProvince(province);
        a.setSpeciality(r.speciality().trim());
        a.setStatement(blankToNull(r.statement()));
        applications.save(a);

        for (User admin : users.findByRoleNameAndEnabledTrue("ADMIN"))
            notifier.send(admin.getId(), "APPLICATION", "New expert application",
                    u.getFullName() + " applied as an expert (" + a.getSpeciality() + ").", "applications");
        return new ApplyResult("Application received. You can log in once an admin has approved it.");
    }

    /** For the login screen: says why a correct password still cannot log in. Null when it does not apply. */
    @Transactional(readOnly = true)
    public String pendingMessage(String identifier, String password) {
        String id = identifier.trim();
        Optional<User> found = id.contains("@") ? users.findByEmailIgnoreCase(id) : users.findByPhone(id);
        User u = found.filter(x -> !x.isEnabled())
                .filter(x -> encoder.matches(password, x.getPasswordHash())).orElse(null);
        if (u == null) return null;
        return applications.findFirstByUserIdOrderByIdDesc(u.getId()).map(a -> switch (a.getStatus()) {
            case "PENDING" -> "Your expert application is still being reviewed. You can log in once an admin approves it.";
            case "REJECTED" -> "Your expert application was not approved."
                    + (a.getReviewNote() == null ? "" : " Note from the admin: " + a.getReviewNote());
            default -> null;
        }).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ApplicationView> list(String status) {
        String s = status == null || status.isBlank() ? "PENDING" : status.trim().toUpperCase();
        if (!FILTERS.contains(s)) throw bad("Status must be one of " + FILTERS);
        List<ExpertApplication> list = s.equals("ALL") ? applications.findTop100ByOrderByIdDesc()
                : applications.findTop100ByStatusOrderByIdAsc(s);
        return list.stream().map(this::view).toList();
    }

    @Transactional
    public ApplicationView approve(Long adminId, Long id) {
        ExpertApplication a = pending(id);
        User u = users.findById(a.getUserId()).orElseThrow(this::notFound);
        u.setEnabled(true);
        a.setStatus("APPROVED");
        a.setReviewedBy(adminId);
        a.setReviewedAt(LocalDateTime.now());
        audit.record(adminId, "APPROVE_EXPERT", u.getId(), a.getSpeciality());
        return view(a);
    }

    @Transactional
    public ApplicationView reject(Long adminId, Long id, String note) {
        ExpertApplication a = pending(id);
        a.setStatus("REJECTED");
        a.setReviewedBy(adminId);
        a.setReviewedAt(LocalDateTime.now());
        a.setReviewNote(blankToNull(note));
        audit.record(adminId, "REJECT_EXPERT", a.getUserId(), a.getReviewNote());
        return view(a);
    }

    private ExpertApplication pending(Long id) {
        ExpertApplication a = applications.findById(id).orElseThrow(this::notFound);
        if (!"PENDING".equals(a.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This application is already " + a.getStatus());
        return a;
    }

    private ApplicationView view(ExpertApplication a) {
        User u = users.findById(a.getUserId()).orElse(null);
        return new ApplicationView(a.getId(), a.getUserId(), u == null ? "User" : u.getFullName(),
                u == null ? null : u.getEmail(), u == null ? null : u.getPhone(), a.getQualification(),
                a.getOrganisation(), a.getRegistrationNumber(), a.getProvince(), a.getSpeciality(), a.getStatement(),
                a.getStatus(), a.getReviewNote(), a.getCreatedAt());
    }

    private static String blankToNull(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }
    private ResponseStatusException notFound() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"); }
    private static ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
}
