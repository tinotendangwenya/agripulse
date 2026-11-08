package com.agripulse.backend.farm;

/**
 * Entities and repositories (database mapping and queries)
 */

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FarmRepository extends JpaRepository<Farm, Long> {
    List<Farm> findByOwnerIdOrderByIdDesc(Long ownerId);
    Optional<Farm> findByIdAndOwnerId(Long id, Long ownerId);
}
