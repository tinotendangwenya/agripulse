package com.agripulse.backend.transport;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByOwnerIdAndStatusNotOrderByIdDesc(Long ownerId, String status);
    Optional<Vehicle> findByIdAndOwnerId(Long id, Long ownerId);
    long countByOwnerIdAndStatus(Long ownerId, String status);

    @Query("""
        select v from Vehicle v
        where v.status = 'ACTIVE' and v.capacityKg >= :w and v.cargoTypes like concat('%', :t, '%')
        order by v.id desc
        """)
    List<Vehicle> findFitting(@Param("w") BigDecimal weightKg, @Param("t") String cargoType, Pageable pageable);
}
