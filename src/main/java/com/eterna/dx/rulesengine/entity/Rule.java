package com.eterna.dx.rulesengine.entity;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Entity
@Table(name = "rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rule {

    @Id
    @Column(name = "id", length = 100)
    private String id;

    @Column(name = "version")
    @Builder.Default
    private Integer version = 1;

    @Column(name = "enabled")
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "tenant_id", length = 50)
    @Builder.Default
    private String tenantId = "default";

    @Column(name = "category")
    private String category;

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 50;

    @Column(name = "severity")
    @Builder.Default
    private Integer severity = 1;

    @Column(name = "cooldown_days")
    @Builder.Default
    private Integer cooldownDays = 0;

    @Column(name = "max_per_day")
    @Builder.Default
    private Integer maxPerDay = 0;

    @Lob
    @Column(name = "tags")
    private String tagsJson;

    @Lob
    @Column(name = "logic", nullable = false)
    private String logicJson;

    @Column(name = "locale", length = 10)
    @Builder.Default
    private String locale = "es-ES";

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<RuleMessage> messages = new ArrayList<>();

    // Métodos helper para manejar JSON
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Transient
    public List<String> getTags() {
        if (tagsJson == null) return new ArrayList<>();
        try {
            return objectMapper.readValue(tagsJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    public void setTags(List<String> tags) {
        try {
            this.tagsJson = objectMapper.writeValueAsString(tags);
        } catch (JsonProcessingException e) {
            this.tagsJson = "[]";
        }
    }

    @Transient
    public Map<String, Object> getLogic() {
        if (logicJson == null) return Map.of();
        try {
            return objectMapper.readValue(logicJson, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    public void setLogic(Map<String, Object> logic) {
        try {
            this.logicJson = objectMapper.writeValueAsString(logic);
        } catch (JsonProcessingException e) {
            this.logicJson = "{}";
        }
    }

    // Métodos helper para manejar mensajes

    public void addMessage(RuleMessage message) {
        messages.add(message);
        message.setRule(this);
    }

    public void removeMessage(RuleMessage message) {
        messages.remove(message);
        message.setRule(null);
    }

    /**
     * Devuelve los mensajes en el formato esperado por el frontend
     */
    public Map<String, Object> getMessagesForFrontend() {
        if (messages == null || messages.isEmpty()) {
            return Map.of(
                "locale", locale != null ? locale : "es-ES",
                "candidates", List.of()
            );
        }

        // Convertir RuleMessage a formato frontend
        List<Map<String, Object>> candidates = messages.stream()
            .map(msg -> {
                Map<String, Object> candidate = new HashMap<>();
                candidate.put("id", msg.getId());
                candidate.put("text", msg.getText() != null ? msg.getText() : "");
                candidate.put("weight", msg.getWeight() != null ? msg.getWeight() : 1);
                candidate.put("active", msg.getActive() != null ? msg.getActive() : true);
                return candidate;
            })
            .collect(Collectors.toList());

        // Usar el locale del primer mensaje o el locale de la regla
        String messageLocale = locale != null ? locale : "es-ES";
        if (!messages.isEmpty()) {
            String firstMessageLocale = messages.get(0).getLocale();
            if (firstMessageLocale != null) {
                messageLocale = firstMessageLocale;
            }
        }

        return Map.of(
            "locale", messageLocale,
            "candidates", candidates
        );
    }
}
