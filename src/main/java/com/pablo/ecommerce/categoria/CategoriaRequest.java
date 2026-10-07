package com.pablo.ecommerce.categoria;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CategoriaRequest(
        @NotBlank String nome,
        @NotBlank @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") String slug) {
}
