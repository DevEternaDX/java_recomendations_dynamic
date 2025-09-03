package com.eterna.dx.rulesengine.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class RuleUpdateRequest {

    private Boolean enabled;

    private String category;

    @Min(value = 0, message = "priority debe ser >= 0")
    @Max(value = 100, message = "priority debe ser <= 100")
    private Integer priority;

    @Min(value = 1, message = "severity debe ser >= 1")
    @Max(value = 3, message = "severity debe ser <= 3")
    private Integer severity;

    @Min(value = 0, message = "cooldown_days debe ser >= 0")
    @Max(value = 30, message = "cooldown_days debe ser <= 30")
    private Integer cooldownDays;

    @Min(value = 0, message = "max_per_day debe ser >= 0")
    @Max(value = 10, message = "max_per_day debe ser <= 10")
    private Integer maxPerDay;

    private List<String> tags;

    private Map<String, Object> logic;

    private String locale;

    private List<MessageRequest> messages;

    private String updatedBy;
}
