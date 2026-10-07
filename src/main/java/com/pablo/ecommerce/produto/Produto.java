package com.pablo.ecommerce.produto;

import java.math.BigDecimal;
import com.pablo.ecommerce.categoria.Categoria;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Setter 
    @NotBlank
    String nome;

    @Setter 
    String descricao;

    @Setter 
    @Positive
    BigDecimal preco;

    @Setter 
    @PositiveOrZero
    int estoque;

    @Setter
    @ManyToOne
    @JoinColumn(name = "categoria_id")
    Categoria categoria;

}
