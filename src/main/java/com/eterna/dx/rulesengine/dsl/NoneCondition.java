package com.eterna.dx.rulesengine.dsl;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Condición que requiere que NINGUNA de las condiciones hijas se cumpla (NOT-ANY lógico).
 * Equivalente a GroupNone en Python.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoneCondition implements Node {

    @Builder.Default
    private List<Node> none = new ArrayList<>();

    @Override
    public boolean eval(Map<String, Map<String, Object>> features, List<Map<String, Object>> trace) {
        boolean result = true;
        List<Map<String, Object>> childTraces = new ArrayList<>();

        for (Node child : none) {
            List<Map<String, Object>> childTrace = new ArrayList<>();
            boolean childResult = child.eval(features, childTrace);
            
            childTraces.addAll(childTrace);
            
            if (childResult) {
                result = false; // Si alguna condición se cumple, el resultado es false
            }
        }

        // Agregar información de debug
        trace.add(Map.of(
            "type", "none",
            "result", result,
            "children_count", none.size(),
            "children_traces", childTraces
        ));

        return result;
    }
}
