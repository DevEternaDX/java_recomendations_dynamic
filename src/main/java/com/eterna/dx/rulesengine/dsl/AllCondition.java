package com.eterna.dx.rulesengine.dsl;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Condición que requiere que TODAS las condiciones hijas se cumplan (AND lógico).
 * Equivalente a GroupAll en Python.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllCondition implements Node {

    @Builder.Default
    private List<Node> all = new ArrayList<>();

    @Override
    public boolean eval(Map<String, Map<String, Object>> features, List<Map<String, Object>> trace) {
        boolean result = true;
        List<Map<String, Object>> childTraces = new ArrayList<>();

        for (Node child : all) {
            List<Map<String, Object>> childTrace = new ArrayList<>();
            boolean childResult = child.eval(features, childTrace);
            
            childTraces.addAll(childTrace);
            
            if (!childResult) {
                result = false;
                break; // En AND, si uno falla, todos fallan
            }
        }

        // Agregar información de debug
        trace.add(Map.of(
            "type", "all",
            "result", result,
            "children_count", all.size(),
            "children_traces", childTraces
        ));

        return result;
    }
}
