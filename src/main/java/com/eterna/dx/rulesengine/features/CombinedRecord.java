package com.eterna.dx.rulesengine.features;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;

/**
 * Registro que combina datos de actividad diaria y sueño.
 * Equivalente a una fila del DataFrame combinado en Python.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CombinedRecord {

    // Campos comunes
    private String userId;
    private LocalDate date;

    // Campos de actividad diaria
    private Integer steps;
    private Integer minutesLight;
    private Integer minutesModerate;
    private Integer minutesVigorous;
    private Double heartRateAverageBpm;
    private Double maxHeartRateBpm;
    private Double minHeartRateBpm;
    private Double restingHeartRate;
    private Double userMaxHeartRateBpm;
    private Double heartRateVariabilitySdnn;

    // Campos de sueño
    private Integer remSleepMinutes;
    private Integer asleepStateMinutes;
    private Integer deepSleepStateMinutes;
    private Integer lightSleepStateMinutes;
    private Integer awakeStateMinutes;
    private Double avgBreathsPerMin;

    // Campos derivados (si están en el CSV procesado)
    private Double acwr;
    private Double trimp;
    private Double readinessScore;
    private Double hrv_rhr_ratio;

    /**
     * Obtiene el valor de un campo por nombre.
     * Utilizado para el procesamiento genérico de columnas.
     */
    public Object getFieldValue(String fieldName) {
        switch (fieldName.toLowerCase()) {
            case "steps": return steps;
            case "minutes_light": return minutesLight;
            case "minutes_moderate": return minutesModerate;
            case "minutes_vigorous": return minutesVigorous;
            case "heart_rate_average_bpm": return heartRateAverageBpm;
            case "max_heart_rate_bpm": return maxHeartRateBpm;
            case "min_heart_rate_bpm": return minHeartRateBpm;
            case "resting_heart_rate": return restingHeartRate;
            case "user_max_heart_rate_bpm": return userMaxHeartRateBpm;
            case "heart_rate_variability_sdnn": return heartRateVariabilitySdnn;
            case "rem_sleep_minutes": return remSleepMinutes;
            case "asleep_state_minutes": return asleepStateMinutes;
            case "deep_sleep_state_minutes": return deepSleepStateMinutes;
            case "light_sleep_state_minutes": return lightSleepStateMinutes;
            case "awake_state_minutes": return awakeStateMinutes;
            case "avg_breaths_per_min": return avgBreathsPerMin;
            case "acwr": return acwr;
            case "trimp": return trimp;
            case "readiness_score": return readinessScore;
            case "hrv_rhr_ratio": return hrv_rhr_ratio;
            default: return null;
        }
    }

    /**
     * Establece el valor de un campo por nombre.
     */
    public void setFieldValue(String fieldName, Object value) {
        try {
            switch (fieldName.toLowerCase()) {
                case "steps": 
                    steps = value != null ? Integer.valueOf(value.toString()) : null; 
                    break;
                case "minutes_light": 
                    minutesLight = value != null ? Integer.valueOf(value.toString()) : null; 
                    break;
                case "minutes_moderate": 
                    minutesModerate = value != null ? Integer.valueOf(value.toString()) : null; 
                    break;
                case "minutes_vigorous": 
                    minutesVigorous = value != null ? Integer.valueOf(value.toString()) : null; 
                    break;
                case "heart_rate_average_bpm": 
                    heartRateAverageBpm = value != null ? Double.valueOf(value.toString()) : null; 
                    break;
                case "max_heart_rate_bpm": 
                    maxHeartRateBpm = value != null ? Double.valueOf(value.toString()) : null; 
                    break;
                case "min_heart_rate_bpm": 
                    minHeartRateBpm = value != null ? Double.valueOf(value.toString()) : null; 
                    break;
                case "resting_heart_rate": 
                    restingHeartRate = value != null ? Double.valueOf(value.toString()) : null; 
                    break;
                case "user_max_heart_rate_bpm": 
                    userMaxHeartRateBpm = value != null ? Double.valueOf(value.toString()) : null; 
                    break;
                case "heart_rate_variability_sdnn": 
                    heartRateVariabilitySdnn = value != null ? Double.valueOf(value.toString()) : null; 
                    break;
                case "rem_sleep_minutes": 
                    remSleepMinutes = value != null ? Integer.valueOf(value.toString()) : null; 
                    break;
                case "asleep_state_minutes": 
                    asleepStateMinutes = value != null ? Integer.valueOf(value.toString()) : null; 
                    break;
                case "deep_sleep_state_minutes": 
                    deepSleepStateMinutes = value != null ? Integer.valueOf(value.toString()) : null; 
                    break;
                case "light_sleep_state_minutes": 
                    lightSleepStateMinutes = value != null ? Integer.valueOf(value.toString()) : null; 
                    break;
                case "awake_state_minutes": 
                    awakeStateMinutes = value != null ? Integer.valueOf(value.toString()) : null; 
                    break;
                case "avg_breaths_per_min": 
                    avgBreathsPerMin = value != null ? Double.valueOf(value.toString()) : null; 
                    break;
                case "acwr": 
                    acwr = value != null ? Double.valueOf(value.toString()) : null; 
                    break;
                case "trimp": 
                    trimp = value != null ? Double.valueOf(value.toString()) : null; 
                    break;
                case "readiness_score": 
                    readinessScore = value != null ? Double.valueOf(value.toString()) : null; 
                    break;
                case "hrv_rhr_ratio": 
                    hrv_rhr_ratio = value != null ? Double.valueOf(value.toString()) : null; 
                    break;
            }
        } catch (Exception e) {
            // Ignorar errores de conversión
        }
    }
}
