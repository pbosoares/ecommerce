package com.pablo.ecommerce.produto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record ProdutoRequest(
        @NotBlank String nome,
        String descricao,
        @NotNull @Positive BigDecimal preco,
        @NotNull @PositiveOrZero Integer estoque,
        @NotNull Long categoriaId) {
}
