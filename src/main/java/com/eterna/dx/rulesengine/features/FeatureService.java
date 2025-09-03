package com.eterna.dx.rulesengine.features;

import com.eterna.dx.rulesengine.config.DataProperties;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVParserBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para carga y procesamiento de datos de features.
 * Equivalente a las funciones en features.py de Python.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureService {

    private final DataProperties dataProperties;

    // Campos que deben excluirse del procesamiento numérico
    private static final Set<String> EXCLUDE_COLS = Set.of(
            "date", "user_id", "bedtime", "waketime", "start_date_time", "end_date_time",
            "calculation_date", "webhook_date_time", "last_webhook_update_date_time",
            "device_source", "sex", "gender"
    );

    // Mapeo de nombres de columnas (equivalente a COLMAP en Python)
    private static final Map<String, String> COLUMN_MAPPING = Map.of(
            "patient_id", "user_id",
            "low_intensity_minutes", "minutes_light",
            "moderate_intensity_minutes", "minutes_moderate",
            "vigorous_intensity_minutes", "minutes_vigorous",
            "resting_heart_rate_bpm", "resting_heart_rate",
            "calculation_date", "date"
    );

    /**
     * Carga el DataFrame base combinado.
     * Equivalente a load_base_dataframe() en Python.
     */
    public List<CombinedRecord> loadBaseDataframe() {
        // Intentar cargar el CSV procesado primero
        if (Files.exists(Paths.get(dataProperties.getProcessedCsvPath()))) {
            try {
                log.info("Cargando CSV procesado: {}", dataProperties.getProcessedCsvPath());
                return loadProcessedCsv();
            } catch (Exception e) {
                log.warn("Error cargando CSV procesado: {}, fallback a CSV originales", e.getMessage());
            }
        }

        // Fallback: cargar y combinar CSV originales
        log.info("Cargando y combinando CSV originales");
        return loadAndCombineOriginalCsvs();
    }

    /**
     * Carga el CSV ya procesado con variables derivadas.
     */
    private List<CombinedRecord> loadProcessedCsv() throws Exception {
        List<CombinedRecord> records = new ArrayList<>();

        try (CSVReader reader = new CSVReaderBuilder(new FileReader(dataProperties.getProcessedCsvPath()))
                .withSkipLines(1) // Saltar header
                .build()) {

            String[] header = null;
            
            // Leer header primero
            try (CSVReader headerReader = new CSVReader(new FileReader(dataProperties.getProcessedCsvPath()))) {
                header = headerReader.readNext();
            }

            if (header == null) {
                throw new Exception("No se pudo leer el header del CSV");
            }

            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                CombinedRecord record = new CombinedRecord();
                
                for (int i = 0; i < Math.min(header.length, nextLine.length); i++) {
                    String columnName = header[i].trim();
                    String value = nextLine[i].trim();
                    
                    if (value.isEmpty()) {
                        continue;
                    }

                    // Procesar campos especiales
                    if ("date".equals(columnName)) {
                        record.setDate(parseDate(value));
                    } else if ("user_id".equals(columnName)) {
                        record.setUserId(value);
                    } else {
                        // Usar el método genérico para otros campos
                        record.setFieldValue(columnName, value);
                    }
                }

                if (record.getUserId() != null && record.getDate() != null) {
                    records.add(record);
                }
            }
        }

        log.info("Cargados {} registros del CSV procesado", records.size());
        return records.stream()
                .sorted(Comparator.comparing(CombinedRecord::getUserId)
                        .thenComparing(CombinedRecord::getDate))
                .collect(Collectors.toList());
    }

    /**
     * Carga y combina los CSV originales de actividad y sueño.
     */
    private List<CombinedRecord> loadAndCombineOriginalCsvs() {
        Map<String, CombinedRecord> recordMap = new HashMap<>();

        // Cargar datos de actividad diaria
        loadDailyCsv(recordMap);
        
        // Cargar datos de sueño
        loadSleepCsv(recordMap);

        List<CombinedRecord> records = new ArrayList<>(recordMap.values());
        
        // Ordenar por user_id y fecha
        records.sort(Comparator.comparing(CombinedRecord::getUserId)
                .thenComparing(CombinedRecord::getDate));

        log.info("Combinados {} registros de CSV originales", records.size());
        return records;
    }

    /**
     * Carga datos del CSV de actividad diaria.
     */
    private void loadDailyCsv(Map<String, CombinedRecord> recordMap) {
        String dailyPath = dataProperties.getDailyCsvPath();
        if (!Files.exists(Paths.get(dailyPath))) {
            log.warn("Archivo de datos diarios no encontrado: {}", dailyPath);
            return;
        }

        try (CSVReader reader = new CSVReaderBuilder(new FileReader(dailyPath))
                .withCSVParser(new CSVParserBuilder().withSeparator(';').build()) // CSV usa separador ;
                .withSkipLines(1)
                .build()) {

            String[] header = null;
            
            // Leer header
            try (CSVReader headerReader = new CSVReaderBuilder(new FileReader(dailyPath))
                    .withCSVParser(new CSVParserBuilder().withSeparator(';').build())
                    .build()) {
                header = headerReader.readNext();
            }

            if (header == null) return;

            // Aplicar mapeo de columnas
            for (int i = 0; i < header.length; i++) {
                header[i] = COLUMN_MAPPING.getOrDefault(header[i].trim(), header[i].trim());
            }

            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                CombinedRecord record = new CombinedRecord();
                
                for (int i = 0; i < Math.min(header.length, nextLine.length); i++) {
                    String columnName = header[i];
                    String value = nextLine[i].trim();
                    
                    if (value.isEmpty()) continue;

                    if ("date".equals(columnName)) {
                        record.setDate(parseDate(value));
                    } else if ("user_id".equals(columnName)) {
                        record.setUserId(value);
                    } else {
                        record.setFieldValue(columnName, value);
                    }
                }

                if (record.getUserId() != null && record.getDate() != null) {
                    String key = record.getUserId() + "|" + record.getDate().toString();
                    recordMap.put(key, record);
                }
            }

            log.info("Cargados {} registros de datos diarios", recordMap.size());
        } catch (Exception e) {
            log.error("Error cargando CSV de datos diarios: {}", e.getMessage());
        }
    }

    /**
     * Carga datos del CSV de sueño.
     */
    private void loadSleepCsv(Map<String, CombinedRecord> recordMap) {
        String sleepPath = dataProperties.getSleepCsvPath();
        if (!Files.exists(Paths.get(sleepPath))) {
            log.warn("Archivo de datos de sueño no encontrado: {}", sleepPath);
            return;
        }

        try (CSVReader reader = new CSVReaderBuilder(new FileReader(sleepPath))
                .withCSVParser(new CSVParserBuilder().withSeparator(';').build())
                .withSkipLines(1)
                .build()) {

            String[] header = null;
            
            // Leer header
            try (CSVReader headerReader = new CSVReaderBuilder(new FileReader(sleepPath))
                    .withCSVParser(new CSVParserBuilder().withSeparator(';').build())
                    .build()) {
                header = headerReader.readNext();
            }

            if (header == null) return;

            // Aplicar mapeo de columnas
            for (int i = 0; i < header.length; i++) {
                header[i] = COLUMN_MAPPING.getOrDefault(header[i].trim(), header[i].trim());
            }

            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                String userId = null;
                LocalDate date = null;
                Map<String, String> sleepData = new HashMap<>();
                
                for (int i = 0; i < Math.min(header.length, nextLine.length); i++) {
                    String columnName = header[i];
                    String value = nextLine[i].trim();
                    
                    if (value.isEmpty()) continue;

                    if ("date".equals(columnName)) {
                        date = parseDate(value);
                    } else if ("user_id".equals(columnName)) {
                        userId = value;
                    } else {
                        sleepData.put(columnName, value);
                    }
                }

                if (userId != null && date != null) {
                    String key = userId + "|" + date.toString();
                    final String finalUserId = userId;
                    final LocalDate finalDate = date;
                    CombinedRecord record = recordMap.computeIfAbsent(key, k -> {
                        CombinedRecord newRecord = new CombinedRecord();
                        newRecord.setUserId(finalUserId);
                        newRecord.setDate(finalDate);
                        return newRecord;
                    });

                    // Agregar datos de sueño al registro
                    for (Map.Entry<String, String> entry : sleepData.entrySet()) {
                        record.setFieldValue(entry.getKey(), entry.getValue());
                    }
                }
            }

            log.info("Combinados datos de sueño en {} registros totales", recordMap.size());
        } catch (Exception e) {
            log.error("Error cargando CSV de sueño: {}", e.getMessage());
        }
    }

    /**
     * Construye features para un usuario en una fecha específica.
     * Equivalente a build_features() en Python.
     */
    public Map<String, Map<String, Object>> buildFeatures(List<CombinedRecord> allRecords, 
                                                          LocalDate targetDate, 
                                                          String userId) {
        // Filtrar registros del usuario hasta la fecha objetivo
        List<CombinedRecord> userRecords = allRecords.stream()
                .filter(r -> userId.equals(r.getUserId()) && 
                           r.getDate() != null && 
                           !r.getDate().isAfter(targetDate))
                .sorted(Comparator.comparing(CombinedRecord::getDate))
                .collect(Collectors.toList());

        Map<String, Map<String, Object>> features = new HashMap<>();

        if (userRecords.isEmpty()) {
            log.debug("No hay datos para usuario {} hasta fecha {}", userId, targetDate);
            return features;
        }

        // Obtener el último registro (valores actuales)
        CombinedRecord lastRecord = userRecords.get(userRecords.size() - 1);

        // Lista de campos a procesar
        List<String> fieldsToProcess = Arrays.asList(
                "steps", "minutes_light", "minutes_moderate", "minutes_vigorous",
                "heart_rate_average_bpm", "max_heart_rate_bpm", "min_heart_rate_bpm",
                "resting_heart_rate", "user_max_heart_rate_bpm", "heart_rate_variability_sdnn",
                "rem_sleep_minutes", "asleep_state_minutes", "deep_sleep_state_minutes",
                "light_sleep_state_minutes", "awake_state_minutes", "avg_breaths_per_min",
                "acwr", "trimp", "readiness_score", "hrv_rhr_ratio"
        );

        // Procesar cada campo
        for (String field : fieldsToProcess) {
            if (EXCLUDE_COLS.contains(field)) {
                continue;
            }

            Map<String, Object> fieldFeatures = new HashMap<>();

            // Valor actual
            Object currentValue = lastRecord.getFieldValue(field);
            fieldFeatures.put("current", currentValue);

            // Extraer serie de valores numéricos
            List<Double> values = StatisticsCalculator.extractNumericValues(userRecords, field);

            // Calcular estadísticas móviles
            fieldFeatures.put("mean_3d", StatisticsCalculator.rollingMean(values, 3));
            fieldFeatures.put("mean_7d", StatisticsCalculator.rollingMean(values, 7));
            fieldFeatures.put("mean_14d", StatisticsCalculator.rollingMean(values, 14));
            fieldFeatures.put("median_14d", StatisticsCalculator.rollingMedian(values, 14));

            // Calcular delta_pct_3v14 = (mean_3d/mean_14d) - 1
            Double mean3d = (Double) fieldFeatures.get("mean_3d");
            Double mean14d = (Double) fieldFeatures.get("mean_14d");
            if (mean3d != null && mean14d != null && mean14d != 0.0) {
                fieldFeatures.put("delta_pct_3v14", (mean3d / mean14d) - 1);
            } else {
                fieldFeatures.put("delta_pct_3v14", null);
            }

            // Z-score
            fieldFeatures.put("zscore_28d", StatisticsCalculator.zscore(values, 28));

            features.put(field, fieldFeatures);
        }

        // Calcular feature derivada: max_hr_pct_user_max
        calculateDerivedFeatures(features);

        log.debug("Construidas features para usuario {} en fecha {}: {} variables", 
                 userId, targetDate, features.size());

        return features;
    }

    /**
     * Calcula features derivadas adicionales.
     */
    private void calculateDerivedFeatures(Map<String, Map<String, Object>> features) {
        // max_hr_pct_user_max = max_heart_rate_bpm.current / user_max_heart_rate_bpm.current
        Object maxHr = features.getOrDefault("max_heart_rate_bpm", Map.of()).get("current");
        Object userMaxHr = features.getOrDefault("user_max_heart_rate_bpm", Map.of()).get("current");

        if (maxHr != null && userMaxHr != null) {
            Double maxHrDouble = StatisticsCalculator.toDouble(maxHr);
            Double userMaxHrDouble = StatisticsCalculator.toDouble(userMaxHr);

            if (maxHrDouble != null && userMaxHrDouble != null && userMaxHrDouble != 0.0) {
                Map<String, Object> derivedFeature = new HashMap<>();
                derivedFeature.put("current", maxHrDouble / userMaxHrDouble);
                features.put("max_hr_pct_user_max", derivedFeature);
            }
        }
    }

    /**
     * Parsea una fecha de string a LocalDate de forma robusta.
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        // Intentar varios formatos de fecha
        String[] patterns = {
                "yyyy-MM-dd",
                "dd/MM/yyyy",
                "MM/dd/yyyy",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss"
        };

        for (String pattern : patterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                if (pattern.contains("HH:mm:ss")) {
                    // Para fechas con tiempo, extraer solo la fecha
                    return LocalDate.parse(dateStr.substring(0, 10), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                } else {
                    return LocalDate.parse(dateStr.trim(), formatter);
                }
            } catch (DateTimeParseException e) {
                // Intentar siguiente formato
            }
        }

        log.warn("No se pudo parsear fecha: {}", dateStr);
        return null;
    }
}
