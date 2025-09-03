package com.eterna.dx.rulesengine.repository;

import com.eterna.dx.rulesengine.entity.Rule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RuleRepository extends JpaRepository<Rule, String> {

    List<Rule> findByTenantId(String tenantId);
    
    List<Rule> findByTenantIdAndEnabled(String tenantId, Boolean enabled);
    
    List<Rule> findByTenantIdAndEnabledOrderByPriorityDescSeverityDesc(String tenantId, Boolean enabled);
    
    List<Rule> findByTenantIdAndCategory(String tenantId, String category);
    
    List<Rule> findByTenantIdAndEnabledAndCategory(String tenantId, Boolean enabled, String category);
    
    @Query("SELECT r FROM Rule r WHERE r.tenantId = :tenantId " +
           "AND (:enabled IS NULL OR r.enabled = :enabled) " +
           "AND (:category IS NULL OR r.category = :category) " +
           "ORDER BY r.priority DESC, r.severity DESC")
    List<Rule> findRulesWithFilters(@Param("tenantId") String tenantId, 
                                   @Param("enabled") Boolean enabled, 
                                   @Param("category") String category);
    
    long countByTenantId(String tenantId);
    
    boolean existsById(String id);
}
