package com.eterna.dx.rulesengine.dsl;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Condición que requiere que AL MENOS UNA de las condiciones hijas se cumpla (OR lógico).
 * Equivalente a GroupAny en Python.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnyCondition implements Node {

    @Builder.Default
    private List<Node> any = new ArrayList<>();

    @Override
    public boolean eval(Map<String, Map<String, Object>> features, List<Map<String, Object>> trace) {
        boolean result = false;
        List<Map<String, Object>> childTraces = new ArrayList<>();

        for (Node child : any) {
            List<Map<String, Object>> childTrace = new ArrayList<>();
            boolean childResult = child.eval(features, childTrace);
            
            childTraces.addAll(childTrace);
            
            if (childResult) {
                result = true;
                // En OR, seguimos evaluando para obtener todas las trazas de debug
            }
        }

        // Agregar información de debug
        trace.add(Map.of(
            "type", "any",
            "result", result,
            "children_count", any.size(),
            "children_traces", childTraces
        ));

        return result;
    }
}
