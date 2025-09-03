package com.eterna.dx.rulesengine.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleRequest {

    @NotBlank(message = "id es requerido")
    @Size(max = 100, message = "id no puede exceder 100 caracteres")
    private String id;

    @Builder.Default
    private Integer version = 1;

    @Builder.Default
    private Boolean enabled = true;

    @Builder.Default
    private String tenantId = "default";

    @NotBlank(message = "category es requerida")
    private String category;

    @Min(value = 0, message = "priority debe ser >= 0")
    @Max(value = 100, message = "priority debe ser <= 100")
    @Builder.Default
    private Integer priority = 50;

    @Min(value = 1, message = "severity debe ser >= 1")
    @Max(value = 3, message = "severity debe ser <= 3")
    @Builder.Default
    private Integer severity = 1;

    @Min(value = 0, message = "cooldown_days debe ser >= 0")
    @Max(value = 30, message = "cooldown_days debe ser <= 30")
    @Builder.Default
    private Integer cooldownDays = 0;

    @Min(value = 0, message = "max_per_day debe ser >= 0")
    @Max(value = 10, message = "max_per_day debe ser <= 10")
    @Builder.Default
    private Integer maxPerDay = 0;

    @Builder.Default
    private List<String> tags = List.of();

    @NotNull(message = "logic es requerida")
    private Map<String, Object> logic;

    @Builder.Default
    private String locale = "es-ES";

    @NotEmpty(message = "messages es requerido y no puede estar vacío")
    private List<MessageRequest> messages;

    private String createdBy;
    
    private String updatedBy;
}
