package com.agripulse.backend.account;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ExpertApplicationRepository extends JpaRepository<ExpertApplication, Long> {
    List<ExpertApplication> findTop100ByStatusOrderByIdAsc(String status);
    List<ExpertApplication> findTop100ByOrderByIdDesc();
    Optional<ExpertApplication> findFirstByUserIdOrderByIdDesc(Long userId);
}
