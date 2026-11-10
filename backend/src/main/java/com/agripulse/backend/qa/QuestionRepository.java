package com.agripulse.backend.qa;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findTop100ByFarmerIdOrderByUpdatedAtDesc(Long farmerId);
    List<Question> findTop100ByStatusOrderByUpdatedAtAsc(String status);
    List<Question> findTop100ByStatusOrderByUpdatedAtDesc(String status);
    List<Question> findTop100ByOrderByUpdatedAtDesc();
    Optional<Question> findByIdAndFarmerId(Long id, Long farmerId);
}
