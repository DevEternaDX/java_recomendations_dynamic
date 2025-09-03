package com.eterna.dx.rulesengine.repository;

import com.eterna.dx.rulesengine.entity.ChangeLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChangeLogRepository extends JpaRepository<ChangeLog, Integer> {

    List<ChangeLog> findByEntityTypeAndEntityId(String entityType, String entityId);
    
    List<ChangeLog> findByUserOrderByCreatedAtDesc(String user);
    
    List<ChangeLog> findByActionOrderByCreatedAtDesc(String action);
    
    Page<ChangeLog> findByOrderByCreatedAtDesc(Pageable pageable);
    
    @Query("SELECT cl FROM ChangeLog cl WHERE " +
           "(:startDate IS NULL OR cl.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR cl.createdAt <= :endDate) AND " +
           "(:entityType IS NULL OR cl.entityType = :entityType) AND " +
           "(:entityId IS NULL OR cl.entityId = :entityId) AND " +
           "(:user IS NULL OR cl.user = :user) AND " +
           "(:action IS NULL OR cl.action = :action) " +
           "ORDER BY cl.createdAt DESC")
    Page<ChangeLog> findLogsWithFilters(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate,
                                       @Param("entityType") String entityType,
                                       @Param("entityId") String entityId,
                                       @Param("user") String user,
                                       @Param("action") String action,
                                       Pageable pageable);
    
    List<ChangeLog> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startDate, LocalDateTime endDate);
}
