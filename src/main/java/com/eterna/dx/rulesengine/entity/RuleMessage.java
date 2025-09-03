package com.eterna.dx.rulesengine.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "rule_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    @JsonIgnore  // Evitar referencia circular en JSON
    private Rule rule;

    @Lob
    @Column(name = "text", nullable = false)
    private String text;

    @Column(name = "weight")
    @Builder.Default
    private Integer weight = 1;

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "locale", length = 10)
    private String locale;

    // Helper method para obtener el ID de la regla sin cargar la entidad completa
    @Transient
    public String getRuleId() {
        return rule != null ? rule.getId() : null;
    }
}
