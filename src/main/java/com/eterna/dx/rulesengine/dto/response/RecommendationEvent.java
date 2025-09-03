package com.eterna.dx.rulesengine.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationEvent {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    private String tenantId;

    private String userId;

    private String ruleId;

    private String category;

    private Integer severity;

    private Integer priority;

    private Integer messageId;

    private String messageText;

    private String locale;

    private List<Map<String, Object>> why;
}
