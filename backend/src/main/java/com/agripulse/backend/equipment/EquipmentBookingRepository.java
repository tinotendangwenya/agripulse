package com.agripulse.backend.equipment;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EquipmentBookingRepository extends JpaRepository<EquipmentBooking, Long> {
    List<EquipmentBooking> findTop100ByHirerIdOrderByIdDesc(Long hirerId);
    long countByHirerIdAndStatus(Long hirerId, String status);
    List<EquipmentBooking> findByEquipmentIdAndStatus(Long equipmentId, String status);
    boolean existsByEquipmentIdAndStatusAndEndDateGreaterThanEqual(Long equipmentId, String status, LocalDate date);

    // Reads only the id, so no booking entity is loaded before the locks are taken
    @Query("select b.equipmentId from EquipmentBooking b where b.id = :id")
    Optional<Long> equipmentIdOf(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from EquipmentBooking b where b.id = :id")
    Optional<EquipmentBooking> findForUpdate(@Param("id") Long id);

    @Query("""
        select b from EquipmentBooking b
        where b.equipmentId in :ids and b.status = 'CONFIRMED' and b.startDate <= :toDate and b.endDate >= :fromDate
        """)
    List<EquipmentBooking> confirmedOverlapping(@Param("ids") Collection<Long> ids,
                                                @Param("fromDate") LocalDate from, @Param("toDate") LocalDate to);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select b from EquipmentBooking b
        where b.equipmentId = :eid and b.status = 'CONFIRMED' and b.startDate <= :toDate and b.endDate >= :fromDate
        """)
    List<EquipmentBooking> lockConfirmedOverlapping(@Param("eid") Long equipmentId,
                                                    @Param("fromDate") LocalDate from, @Param("toDate") LocalDate to);

    @Query("select b from EquipmentBooking b where b.equipmentId in (select e.id from Equipment e where e.ownerId = :oid) order by b.id desc")
    List<EquipmentBooking> forOwner(@Param("oid") Long ownerId, Pageable pageable);
}
