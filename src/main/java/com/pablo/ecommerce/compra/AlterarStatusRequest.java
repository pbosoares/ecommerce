package com.pablo.ecommerce.compra;

import jakarta.validation.constraints.NotNull;

public record AlterarStatusRequest(@NotNull StatusPedido status) {}
