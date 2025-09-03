package com.eterna.dx.rulesengine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "data")
public class DataProperties {
    
    private String dir = "data";
    private String dailyCsvPath = "data/patient_daily_data.csv";
    private String sleepCsvPath = "data/patient_sleep_data.csv";
    private String processedCsvPath = "data/daily_processed.csv";
    private String patientCsvPath = "data/patient_fixed.csv";
}
