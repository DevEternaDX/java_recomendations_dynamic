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
public class SimulationResult {

    private List<RecommendationEvent> events;

    private Map<String, Object> debug;
}
