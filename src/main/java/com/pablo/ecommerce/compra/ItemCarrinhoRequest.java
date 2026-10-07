package com.pablo.ecommerce.compra;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ItemCarrinhoRequest(@NotNull Long produtoId,
        @NotNull @Positive @Max(999) Integer quantidade) {
}
