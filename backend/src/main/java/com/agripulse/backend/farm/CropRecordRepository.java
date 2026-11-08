package com.agripulse.backend.farm;

/**
 * Entities and repositories (database mapping and queries)
 */

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CropRecordRepository extends JpaRepository<CropRecord, Long> {
    List<CropRecord> findByFarmIdOrderByIdDesc(Long farmId);
    Optional<CropRecord> findByIdAndFarmId(Long id, Long farmId);
}
