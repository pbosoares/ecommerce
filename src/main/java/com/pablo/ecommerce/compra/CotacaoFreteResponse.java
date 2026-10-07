package com.pablo.ecommerce.compra;

import java.math.BigDecimal;

public record CotacaoFreteResponse(String cep, long pesoGramas, BigDecimal subtotal,
        BigDecimal frete, BigDecimal total) {
}
