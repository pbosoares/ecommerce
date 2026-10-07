package com.pablo.ecommerce.produto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record ProdutoRequest(
        @NotBlank String nome,
        String descricao,
        @NotNull @Positive BigDecimal preco,
        @PositiveOrZero Integer estoque,
        @NotNull Long categoriaId,
        TipoProduto tipo,
        @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9._-]+") String sku,
        @Size(max = 10) List<@NotBlank String> imagens,
        @Size(max = 20) Map<@NotBlank String, @NotBlank String> atributos,
        @Positive Integer pesoGramas,
        @Positive Integer alturaCm,
        @Positive Integer larguraCm,
        @Positive Integer comprimentoCm) {
}
