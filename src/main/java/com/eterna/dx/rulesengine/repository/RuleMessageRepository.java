package com.eterna.dx.rulesengine.repository;

import com.eterna.dx.rulesengine.entity.RuleMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RuleMessageRepository extends JpaRepository<RuleMessage, Integer> {

    List<RuleMessage> findByRule_Id(String ruleId);
    
    List<RuleMessage> findByRule_IdAndActive(String ruleId, Boolean active);
    
    @Query("SELECT rm FROM RuleMessage rm WHERE rm.rule.id = :ruleId AND rm.active = true")
    List<RuleMessage> findActiveMessagesByRuleId(@Param("ruleId") String ruleId);
    
    void deleteByRule_Id(String ruleId);
    
    long countByRule_Id(String ruleId);
    
    long countByRule_IdAndActive(String ruleId, Boolean active);
}
