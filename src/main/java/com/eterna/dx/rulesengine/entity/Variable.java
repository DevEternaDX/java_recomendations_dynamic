package com.eterna.dx.rulesengine.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Entity
@Table(name = "variables")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Variable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "variable_key", length = 100, unique = true, nullable = false)
    private String key;

    @Column(name = "label")
    private String label;

    @Column(name = "description")
    private String description;

    @Column(name = "unit")
    private String unit;

    @Column(name = "type", length = 20, nullable = false)
    private String type = "number";

    @Lob
    @Column(name = "allowed_aggregators")
    private String allowedAggregatorsJson;

    @Column(name = "valid_min")
    private Double validMin;

    @Column(name = "valid_max")
    private Double validMax;

    @Column(name = "missing_policy", length = 50)
    private String missingPolicy = "skip";

    @Column(name = "decimals")
    private Integer decimals;

    @Column(name = "category")
    private String category;

    @Column(name = "tenant_id", length = 50)
    private String tenantId = "default";

    @Lob
    @Column(name = "examples")
    private String examplesJson;

    // Métodos helper para manejar JSON
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Transient
    public List<String> getAllowedAggregators() {
        if (allowedAggregatorsJson == null) return List.of();
        try {
            return objectMapper.readValue(allowedAggregatorsJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    public void setAllowedAggregators(List<String> allowedAggregators) {
        try {
            this.allowedAggregatorsJson = objectMapper.writeValueAsString(allowedAggregators);
        } catch (JsonProcessingException e) {
            this.allowedAggregatorsJson = "[]";
        }
    }

    @Transient
    public Map<String, Object> getExamples() {
        if (examplesJson == null) return Map.of();
        try {
            return objectMapper.readValue(examplesJson, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    public void setExamples(Map<String, Object> examples) {
        try {
            this.examplesJson = objectMapper.writeValueAsString(examples);
        } catch (JsonProcessingException e) {
            this.examplesJson = "{}";
        }
    }
}
