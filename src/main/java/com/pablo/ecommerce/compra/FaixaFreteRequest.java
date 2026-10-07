package com.pablo.ecommerce.compra;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record FaixaFreteRequest(
        @NotNull @Pattern(regexp = "\\d{8}") String cepInicio,
        @NotNull @Pattern(regexp = "\\d{8}") String cepFim,
        @NotNull @PositiveOrZero Integer pesoMinimoGramas,
        @NotNull @Positive Integer pesoMaximoGramas,
        @NotNull @PositiveOrZero BigDecimal valor) {
}
