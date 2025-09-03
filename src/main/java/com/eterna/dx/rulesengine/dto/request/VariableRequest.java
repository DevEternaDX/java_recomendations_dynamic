package com.eterna.dx.rulesengine.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
public class VariableRequest {

    @NotBlank(message = "key es requerido")
    private String key;

    private String label;

    private String description;

    private String unit;

    @Pattern(regexp = "^(number|boolean|category)$", message = "type debe ser number, boolean o category")
    @Builder.Default
    private String type = "number";

    @Builder.Default
    private List<String> allowedAggregators = List.of("current", "mean_3d", "mean_7d", "mean_14d", "median_14d", "delta_pct_3v14", "zscore_28d");

    private Double validMin;

    private Double validMax;

    @Pattern(regexp = "^(skip|zero|fallback\\(.+\\))$", message = "missing_policy debe ser skip, zero o fallback(...)")
    @Builder.Default
    private String missingPolicy = "skip";

    private Integer decimals;

    private String category;

    @Builder.Default
    private String tenantId = "default";

    private Map<String, Object> examples;
}
