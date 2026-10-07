package com.pablo.ecommerce.produto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.pablo.ecommerce.categoria.Categoria;

import jakarta.persistence.Entity;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OrderColumn;
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
    @Column(length = 4000)
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

    @Setter
    @Enumerated(EnumType.STRING)
    TipoProduto tipo = TipoProduto.FISICO;

    public TipoProduto getTipo() {
        // Rows created before product types existed are physical products.
        return tipo == null ? TipoProduto.FISICO : tipo;
    }

    @Setter
    @Column(unique = true, length = 64)
    String sku;

    @Setter
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "produto_imagens")
    @OrderColumn(name = "ordem")
    @Column(name = "url", length = 2048)
    List<String> imagens = new ArrayList<>();

    @Setter
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "produto_atributos")
    @MapKeyColumn(name = "chave", length = 60)
    @Column(name = "valor", length = 255)
    Map<String, String> atributos = new HashMap<>();

    @Setter
    Integer pesoGramas;

    @Setter
    Integer alturaCm;

    @Setter
    Integer larguraCm;

    @Setter
    Integer comprimentoCm;

}
