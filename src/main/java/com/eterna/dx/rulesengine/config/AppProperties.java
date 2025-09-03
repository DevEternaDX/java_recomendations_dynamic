package com.eterna.dx.rulesengine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    
    private String env = "development";
    private String defaultLocale = "es-ES";
    private int maxRecsPerDay = 3;
    private int maxRecsPerCategoryPerDay = 1;
    private int antiRepeatDays = 7;
    private boolean authEnabled = false;
}
