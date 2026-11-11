package com.agripulse.backend.equipment;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EquipmentBlockRepository extends JpaRepository<EquipmentBlock, Long> {
    List<EquipmentBlock> findByEquipmentIdOrderByStartDateAsc(Long equipmentId);
    Optional<EquipmentBlock> findByIdAndEquipmentId(Long id, Long equipmentId);

    @Query("select k from EquipmentBlock k where k.equipmentId in :ids and k.startDate <= :toDate and k.endDate >= :fromDate")
    List<EquipmentBlock> overlapping(@Param("ids") Collection<Long> ids,
                                     @Param("fromDate") LocalDate from, @Param("toDate") LocalDate to);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select k from EquipmentBlock k where k.equipmentId = :eid and k.startDate <= :toDate and k.endDate >= :fromDate")
    List<EquipmentBlock> lockOverlapping(@Param("eid") Long equipmentId,
                                         @Param("fromDate") LocalDate from, @Param("toDate") LocalDate to);
}
