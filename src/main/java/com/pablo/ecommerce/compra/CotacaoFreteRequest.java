package com.pablo.ecommerce.compra;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CotacaoFreteRequest(@NotNull @Pattern(regexp = "\\d{8}") String cep) {
}
