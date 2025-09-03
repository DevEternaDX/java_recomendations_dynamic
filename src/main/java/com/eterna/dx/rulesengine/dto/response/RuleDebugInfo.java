package com.eterna.dx.rulesengine.dto.response;

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
public class RuleDebugInfo {

    private String ruleId;

    private Boolean fired;

    private Integer priority;

    private Integer severity;

    private List<Map<String, Object>> why;

    private Map<String, Object> values;
}
