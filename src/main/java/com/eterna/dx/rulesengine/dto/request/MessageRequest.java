package com.eterna.dx.rulesengine.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageRequest {

    @NotBlank(message = "text es requerido")
    private String text;

    @Min(value = 1, message = "weight debe ser >= 1")
    @Builder.Default
    private Integer weight = 1;

    @Builder.Default
    private Boolean active = true;

    private String locale;
}
