package com.eterna.dx.rulesengine.dsl;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * Condición numérica que compara el valor de una variable con un umbral.
 * Equivalente a NumericLeaf en Python.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NumericCondition implements Node {

    private String var;
    
    @Builder.Default
    private String agg = "current";
    
    private String op;
    
    private Object value;
    
    @Builder.Default
    private boolean required = false;

    @Override
    public boolean eval(Map<String, Map<String, Object>> features, List<Map<String, Object>> trace) {
        // Obtener el valor observado de las features
        Object observedValue = null;
        if (features.containsKey(var) && features.get(var).containsKey(agg)) {
            observedValue = features.get(var).get(agg);
        }

        boolean result = false;
        
        try {
            if (observedValue == null) {
                if (required) {
                    result = false;
                } else {
                    result = false; // Por defecto, valor nulo es false
                }
            } else {
                result = compare(op, observedValue, value);
            }
        } catch (Exception e) {
            result = false;
        }

        // Agregar información de debug
        trace.add(Map.of(
            "type", "numeric",
            "var", var,
            "agg", agg,
            "op", op,
            "threshold", value,
            "observed", observedValue,
            "result", result,
            "required", required
        ));

        return result;
    }

    /**
     * Compara dos valores según el operador especificado.
     */
    private boolean compare(String op, Object left, Object right) {
        if (left == null || right == null) {
            return false;
        }

        try {
            switch (op) {
                case "<":
                    return toDouble(left) < toDouble(right);
                case "<=":
                    return toDouble(left) <= toDouble(right);
                case ">":
                    return toDouble(left) > toDouble(right);
                case ">=":
                    return toDouble(left) >= toDouble(right);
                case "==":
                    return toDouble(left).equals(toDouble(right));
                case "between":
                    if (right instanceof List) {
                        List<?> range = (List<?>) right;
                        if (range.size() == 2) {
                            double val = toDouble(left);
                            double min = toDouble(range.get(0));
                            double max = toDouble(range.get(1));
                            return val >= min && val <= max;
                        }
                    }
                    return false;
                case "in":
                    if (right instanceof List) {
                        List<?> values = (List<?>) right;
                        return values.contains(left) || values.contains(toDouble(left));
                    }
                    return false;
                default:
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Convierte un objeto a Double de forma segura.
     */
    private Double toDouble(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        if (obj instanceof String) {
            try {
                return Double.parseDouble((String) obj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
