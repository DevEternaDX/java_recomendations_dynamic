package com.eterna.dx.rulesengine.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulateRequest {

    @NotBlank(message = "user_id es requerido")
    private String userId;

    @NotNull(message = "date es requerida")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @Builder.Default
    private String tenantId = "default";

    @Builder.Default
    private boolean debug = false;
}
