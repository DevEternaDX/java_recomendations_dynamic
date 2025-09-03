package com.eterna.dx.rulesengine.features;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utilidades para cálculos estadísticos.
 * Equivalente a las funciones rolling_mean, rolling_median, zscore en Python.
 */
@Slf4j
public class StatisticsCalculator {

    /**
     * Calcula la media móvil de los últimos N valores.
     */
    public static Double rollingMean(List<Double> values, int windowDays) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        try {
            List<Double> nonNullValues = values.stream()
                    .filter(v -> v != null && !Double.isNaN(v) && Double.isFinite(v))
                    .collect(Collectors.toList());

            if (nonNullValues.isEmpty()) {
                return null;
            }

            // Tomar los últimos windowDays valores
            int startIndex = Math.max(0, nonNullValues.size() - windowDays);
            List<Double> windowValues = nonNullValues.subList(startIndex, nonNullValues.size());

            if (windowValues.isEmpty()) {
                return null;
            }

            double sum = windowValues.stream().mapToDouble(Double::doubleValue).sum();
            return sum / windowValues.size();
        } catch (Exception e) {
            log.debug("Error calculando media móvil: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Calcula la mediana móvil de los últimos N valores.
     */
    public static Double rollingMedian(List<Double> values, int windowDays) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        try {
            List<Double> nonNullValues = values.stream()
                    .filter(v -> v != null && !Double.isNaN(v) && Double.isFinite(v))
                    .sorted()
                    .collect(Collectors.toList());

            if (nonNullValues.isEmpty()) {
                return null;
            }

            // Tomar los últimos windowDays valores
            int startIndex = Math.max(0, nonNullValues.size() - windowDays);
            List<Double> windowValues = nonNullValues.subList(startIndex, nonNullValues.size());

            if (windowValues.isEmpty()) {
                return null;
            }

            windowValues.sort(Double::compareTo);
            int size = windowValues.size();
            
            if (size % 2 == 0) {
                return (windowValues.get(size / 2 - 1) + windowValues.get(size / 2)) / 2.0;
            } else {
                return windowValues.get(size / 2);
            }
        } catch (Exception e) {
            log.debug("Error calculando mediana móvil: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Calcula el z-score del último valor respecto a los últimos N valores.
     */
    public static Double zscore(List<Double> values, int windowDays) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        try {
            List<Double> nonNullValues = values.stream()
                    .filter(v -> v != null && !Double.isNaN(v) && Double.isFinite(v))
                    .collect(Collectors.toList());

            if (nonNullValues.isEmpty()) {
                return null;
            }

            // Tomar los últimos windowDays valores
            int startIndex = Math.max(0, nonNullValues.size() - windowDays);
            List<Double> windowValues = nonNullValues.subList(startIndex, nonNullValues.size());

            if (windowValues.size() < 2) {
                return null; // Necesitamos al menos 2 valores para calcular desviación estándar
            }

            // Calcular media
            double mean = windowValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            
            // Calcular desviación estándar
            double variance = windowValues.stream()
                    .mapToDouble(v -> Math.pow(v - mean, 2))
                    .average()
                    .orElse(0.0);
            
            double stdDev = Math.sqrt(variance);
            
            if (stdDev == 0.0) {
                return 0.0; // Si no hay variación, z-score es 0
            }

            // Z-score del último valor
            double lastValue = nonNullValues.get(nonNullValues.size() - 1);
            double zScore = (lastValue - mean) / stdDev;
            
            return Double.isFinite(zScore) ? zScore : null;
        } catch (Exception e) {
            log.debug("Error calculando z-score: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Convierte un valor a Double de forma segura.
     */
    public static Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        
        try {
            if (value instanceof Number) {
                double d = ((Number) value).doubleValue();
                return Double.isFinite(d) ? d : null;
            }
            
            if (value instanceof String) {
                String str = ((String) value).trim();
                if (str.isEmpty()) {
                    return null;
                }
                double d = Double.parseDouble(str);
                return Double.isFinite(d) ? d : null;
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extrae valores numéricos de una lista de registros para un campo específico.
     */
    public static List<Double> extractNumericValues(List<CombinedRecord> records, String fieldName) {
        return records.stream()
                .map(record -> {
                    Object value = record.getFieldValue(fieldName);
                    return toDouble(value);
                })
                .collect(Collectors.toList());
    }
}
