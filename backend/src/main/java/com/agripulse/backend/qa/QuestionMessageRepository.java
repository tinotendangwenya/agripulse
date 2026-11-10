package com.agripulse.backend.qa;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface QuestionMessageRepository extends JpaRepository<QuestionMessage, Long> {
    List<QuestionMessage> findByQuestionIdOrderByIdAsc(Long questionId);
    Optional<QuestionMessage> findByIdAndQuestionId(Long id, Long questionId);
}
