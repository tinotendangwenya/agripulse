package com.agripulse.backend.qa;

import com.agripulse.backend.farm.Farm;
import com.agripulse.backend.farm.FarmRepository;
import com.agripulse.backend.qa.QaDtos.*;
import com.agripulse.backend.user.User;
import com.agripulse.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Farmers see only their own questions (others get 404). Experts and admins see all questions,
 * with the farmer's name and province but no contact details.
 */
@Service
@RequiredArgsConstructor
public class QaService {
    static final Set<String> SUBJECTS = Set.of("CROP", "LIVESTOCK", "OTHER");
    static final Set<String> STATUSES = Set.of("OPEN", "ANSWERED", "CLOSED", "ALL");

    private final QuestionRepository questions;
    private final QuestionMessageRepository messages;
    private final UserRepository users;
    private final FarmRepository farms;
    private final ImageStore images;

    @Transactional
    public ThreadView ask(Long farmerId, Long farmId, String subject, String topic, String title,
                          String body, MultipartFile image) {
        String s = subject == null ? "" : subject.trim().toUpperCase();
        if (!SUBJECTS.contains(s)) throw bad("Subject must be one of " + SUBJECTS);
        String t = title == null ? "" : title.trim();
        if (t.length() < 3 || t.length() > 150) throw bad("Give your question a short title (3 to 150 characters)");
        if (body != null && body.length() > 4000) throw bad("Your message is too long");
        if (blank(body) && !hasFile(image)) throw bad("Describe your question or add a photo");
        if (farmId != null) farms.findByIdAndOwnerId(farmId, farmerId).orElseThrow(this::notFound);

        Question q = new Question();
        q.setFarmerId(farmerId);
        q.setFarmId(farmId);
        q.setSubject(s);
        String tp = blank(topic) ? null : topic.trim();
        q.setTopic(tp == null ? null : tp.substring(0, Math.min(80, tp.length())));
        q.setTitle(t);
        q.setStatus("OPEN");
        q.setUpdatedAt(LocalDateTime.now());
        questions.save(q);
        addMessage(q, farmerId, "FARMER", body, image);
        return thread(q);
    }

    @Transactional(readOnly = true)
    public List<QuestionSummary> mine(Long farmerId) {
        return questions.findTop100ByFarmerIdOrderByUpdatedAtDesc(farmerId).stream().map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public List<QuestionSummary> queue(String status) {
        String s = blank(status) ? "OPEN" : status.trim().toUpperCase();
        if (!STATUSES.contains(s)) throw bad("Status must be one of " + STATUSES);
        List<Question> list = s.equals("ALL") ? questions.findTop100ByOrderByUpdatedAtDesc()
                : s.equals("OPEN") ? questions.findTop100ByStatusOrderByUpdatedAtAsc(s)
                : questions.findTop100ByStatusOrderByUpdatedAtDesc(s);
        return list.stream().map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public ThreadView get(Long callerId, boolean staff, Long id) {
        return thread(find(callerId, staff, id));
    }

    @Transactional
    public ThreadView reply(Long callerId, String role, Long id, String body, MultipartFile image) {
        boolean staff = !role.equals("FARMER");
        Question q = find(callerId, staff, id);
        if ("CLOSED".equals(q.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "This question is closed");
        if (body != null && body.length() > 4000) throw bad("Your message is too long");
        if (blank(body) && !hasFile(image)) throw bad("Write a reply or add a photo");
        addMessage(q, callerId, role, body, image);
        q.setStatus(staff ? "ANSWERED" : "OPEN");
        return thread(q);
    }

    @Transactional
    public ThreadView close(Long farmerId, Long id) {
        Question q = questions.findByIdAndFarmerId(id, farmerId).orElseThrow(this::notFound);
        q.setStatus("CLOSED");
        q.setUpdatedAt(LocalDateTime.now());
        return thread(q);
    }

    @Transactional(readOnly = true)
    public ImageData image(Long callerId, boolean staff, Long id, Long messageId) {
        Question q = find(callerId, staff, id);
        QuestionMessage m = messages.findByIdAndQuestionId(messageId, q.getId()).orElseThrow(this::notFound);
        if (m.getImageFile() == null) throw notFound();
        return new ImageData(images.read(m.getImageFile()), m.getImageType());
    }

    private Question find(Long callerId, boolean staff, Long id) {
        return (staff ? questions.findById(id) : questions.findByIdAndFarmerId(id, callerId)).orElseThrow(this::notFound);
    }

    private void addMessage(Question q, Long authorId, String role, String body, MultipartFile image) {
        QuestionMessage m = new QuestionMessage();
        m.setQuestionId(q.getId());
        m.setAuthorId(authorId);
        m.setAuthorRole(role);
        m.setBody(blank(body) ? null : body.trim());
        if (hasFile(image)) {
            var saved = images.save(image);
            m.setImageFile(saved.name());
            m.setImageType(saved.type());
        }
        messages.save(m);
        q.setUpdatedAt(LocalDateTime.now());
    }

    private ThreadView thread(Question q) {
        List<MessageView> list = messages.findByQuestionIdOrderByIdAsc(q.getId()).stream()
                .map(m -> new MessageView(m.getId(), nameOf(m.getAuthorId()), m.getAuthorRole(), m.getBody(),
                        m.getImageFile() != null, m.getCreatedAt())).toList();
        return new ThreadView(summary(q), list);
    }

    private QuestionSummary summary(Question q) {
        Farm f = q.getFarmId() == null ? null : farms.findById(q.getFarmId()).orElse(null);
        return new QuestionSummary(q.getId(), q.getTitle(), q.getSubject(), q.getTopic(), q.getStatus(),
                nameOf(q.getFarmerId()), f == null ? null : f.getName(), f == null ? null : f.getProvince(),
                q.getCreatedAt(), q.getUpdatedAt());
    }

    private String nameOf(Long id) {
        return users.findById(id).map(User::getFullName).orElse("User");
    }

    private static boolean hasFile(MultipartFile f) { return f != null && !f.isEmpty(); }
    private static boolean blank(String s) { return s == null || s.isBlank(); }
    private ResponseStatusException notFound() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"); }
    private static ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
}
