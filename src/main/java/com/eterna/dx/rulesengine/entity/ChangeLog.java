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

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "change_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "user_name")
    private String user;

    @Column(name = "role")
    private String role;

    @Column(name = "action", length = 50, nullable = false)
    private String action;

    @Column(name = "entity_type", length = 50, nullable = false)
    private String entityType;

    @Column(name = "entity_id")
    private String entityId;

    @Lob
    @Column(name = "before_data")
    private String beforeJson;

    @Lob
    @Column(name = "after_data")
    private String afterJson;

    // Métodos helper para manejar JSON
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Transient
    public Map<String, Object> getBefore() {
        if (beforeJson == null) return Map.of();
        try {
            return objectMapper.readValue(beforeJson, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    public void setBefore(Map<String, Object> before) {
        try {
            this.beforeJson = objectMapper.writeValueAsString(before);
        } catch (JsonProcessingException e) {
            this.beforeJson = "{}";
        }
    }

    @Transient
    public Map<String, Object> getAfter() {
        if (afterJson == null) return Map.of();
        try {
            return objectMapper.readValue(afterJson, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    public void setAfter(Map<String, Object> after) {
        try {
            this.afterJson = objectMapper.writeValueAsString(after);
        } catch (JsonProcessingException e) {
            this.afterJson = "{}";
        }
    }
}
