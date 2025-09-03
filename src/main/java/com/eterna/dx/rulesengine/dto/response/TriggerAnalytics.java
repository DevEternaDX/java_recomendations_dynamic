package com.eterna.dx.rulesengine.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TriggerAnalytics {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate start;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate end;

    private List<RuleSeries> series;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RuleSeries {
        private String ruleId;
        private List<DataPoint> points;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DataPoint {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        private Long count;
    }
}
