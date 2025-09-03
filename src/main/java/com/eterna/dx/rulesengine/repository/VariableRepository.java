package com.eterna.dx.rulesengine.repository;

import com.eterna.dx.rulesengine.entity.Variable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VariableRepository extends JpaRepository<Variable, Integer> {

    Optional<Variable> findByKey(String key);
    
    List<Variable> findByTenantId(String tenantId);
    
    List<Variable> findByTenantIdOrderByKeyAsc(String tenantId);
    
    boolean existsByKey(String key);
    
    @Query("SELECT v FROM Variable v WHERE v.tenantId = :tenantId AND v.category = :category ORDER BY v.key ASC")
    List<Variable> findByTenantIdAndCategory(@Param("tenantId") String tenantId, @Param("category") String category);
}
