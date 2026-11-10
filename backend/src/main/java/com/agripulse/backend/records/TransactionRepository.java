package com.agripulse.backend.records;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<FarmTransaction, Long> {
    List<FarmTransaction> findTop500ByFarmIdAndTxDateBetweenOrderByTxDateDescIdDesc(Long farmId, LocalDate from, LocalDate to);

    Optional<FarmTransaction> findByIdAndFarmId(Long id, Long farmId);

    @Query("""
        select t.entryType, t.currency, t.category, sum(t.amount) from FarmTransaction t
        where t.farmId = :farmId and t.txDate between :fromDate and :toDate
        group by t.entryType, t.currency, t.category
        """)
    List<Object[]> totals(@Param("farmId") Long farmId, @Param("fromDate") LocalDate from, @Param("toDate") LocalDate to);

    @Query("""
        select t.cropRecordId, t.entryType, t.currency, sum(t.amount) from FarmTransaction t
        where t.farmId = :farmId and t.cropRecordId is not null and t.txDate between :fromDate and :toDate
        group by t.cropRecordId, t.entryType, t.currency
        """)
    List<Object[]> cropTotals(@Param("farmId") Long farmId, @Param("fromDate") LocalDate from, @Param("toDate") LocalDate to);
}
