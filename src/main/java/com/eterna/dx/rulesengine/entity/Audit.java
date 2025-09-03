package com.eterna.dx.rulesengine.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "audits")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "tenant_id", length = 50)
    @Builder.Default
    private String tenantId = "default";

    @Column(name = "user_id", length = 100, nullable = false)
    private String userId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "rule_id")
    private String ruleId;

    @Column(name = "fired")
    @Builder.Default
    private Boolean fired = false;

    @Column(name = "discarded_reason")
    private String discardedReason;

    @Lob
    @Column(name = "why")
    private String whyJson;

    @Lob
    @Column(name = "audit_values")
    private String valuesJson;

    @Column(name = "message_id")
    private Integer messageId;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Métodos helper para manejar JSON
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Transient
    public Map<String, Object> getWhy() {
        if (whyJson == null) return Map.of();
        try {
            return objectMapper.readValue(whyJson, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    public void setWhy(Map<String, Object> why) {
        try {
            this.whyJson = objectMapper.writeValueAsString(why);
        } catch (JsonProcessingException e) {
            this.whyJson = "{}";
        }
    }

    @Transient
    public Map<String, Object> getValues() {
        if (valuesJson == null) return Map.of();
        try {
            return objectMapper.readValue(valuesJson, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    public void setValues(Map<String, Object> values) {
        try {
            this.valuesJson = objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            this.valuesJson = "{}";
        }
    }
}
