package com.pablo.ecommerce.compra;

import jakarta.validation.Valid;

public record CriarPedidoRequest(@Valid EnderecoRequest entrega) {
}
