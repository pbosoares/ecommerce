package com.pablo.ecommerce.compra;

import java.math.BigDecimal;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class FaixaFrete {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String cepInicio;
    private String cepFim;
    private int pesoMinimoGramas;
    private int pesoMaximoGramas;
    private BigDecimal valor;
}
