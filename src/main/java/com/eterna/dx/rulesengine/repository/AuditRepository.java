package com.eterna.dx.rulesengine.repository;

import com.eterna.dx.rulesengine.entity.Audit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AuditRepository extends JpaRepository<Audit, Integer> {

    List<Audit> findByUserId(String userId);
    
    List<Audit> findByUserIdAndDate(String userId, LocalDate date);
    
    List<Audit> findByUserIdAndRuleId(String userId, String ruleId);
    
    List<Audit> findByUserIdAndRuleIdAndFired(String userId, String ruleId, Boolean fired);
    
    List<Audit> findByUserIdAndDateBetween(String userId, LocalDate startDate, LocalDate endDate);
    
    List<Audit> findByRuleIdAndFiredAndDateBetween(String ruleId, Boolean fired, LocalDate startDate, LocalDate endDate);
    
    boolean existsByUserIdAndRuleIdAndFiredAndDateBetween(String userId, String ruleId, Boolean fired, LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT a FROM Audit a WHERE a.tenantId = :tenantId " +
           "AND a.date BETWEEN :startDate AND :endDate " +
           "AND a.fired = true " +
           "AND (:ruleIds IS NULL OR a.ruleId IN :ruleIds) " +
           "ORDER BY a.date DESC, a.ruleId ASC")
    List<Audit> findTriggersForAnalytics(@Param("tenantId") String tenantId,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate,
                                        @Param("ruleIds") List<String> ruleIds);
    
    @Query("SELECT COUNT(a) FROM Audit a WHERE a.ruleId = :ruleId AND a.fired = true")
    long countTriggersByRuleId(@Param("ruleId") String ruleId);
    
    @Query("SELECT a.ruleId, a.date, COUNT(a) as count FROM Audit a " +
           "WHERE a.tenantId = :tenantId AND a.fired = true " +
           "AND a.date BETWEEN :startDate AND :endDate " +
           "GROUP BY a.ruleId, a.date " +
           "ORDER BY a.date ASC, a.ruleId ASC")
    List<Object[]> getTriggerStatsByDateAndRule(@Param("tenantId") String tenantId,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);
}
