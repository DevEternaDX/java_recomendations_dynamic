package com.eterna.dx.rulesengine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "seeds")
public class SeedsProperties {
    
    private String variablesPath = "src/main/resources/seeds/variables_seed.json";
    private String rulesPath = "src/main/resources/seeds/rules_seed.json";
}
