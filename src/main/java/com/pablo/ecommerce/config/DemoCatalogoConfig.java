package com.pablo.ecommerce.config;

import java.math.BigDecimal;
import java.util.List;
import com.pablo.ecommerce.categoria.Categoria;
import com.pablo.ecommerce.categoria.CategoriaRepository;
import com.pablo.ecommerce.produto.Produto;
import com.pablo.ecommerce.produto.ProdutoRepository;
import com.pablo.ecommerce.produto.TipoProduto;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("demo")
public class DemoCatalogoConfig {
    @Bean
    ApplicationRunner catalogoDemonstracao(CategoriaRepository categorias, ProdutoRepository produtos) {
        return args -> {
            if (categorias.count() > 0 || produtos.count() > 0) return;
            Categoria tecnologia = categoria(categorias, "Tecnologia", "tecnologia");
            Categoria casa = categoria(categorias, "Casa", "casa");
            Categoria estilo = categoria(categorias, "Estilo", "estilo");
            Categoria digital = categoria(categorias, "Digital", "digital");

            produto(produtos, tecnologia, "Fone Aurora · demonstração", "Produto fictício para demonstrar a loja. Fone sem fio com acabamento suave.",
                    "189.90", 12, 350, "/demo/fone.svg", TipoProduto.FISICO);
            produto(produtos, tecnologia, "Caixa de som Brisa · demonstração", "Produto fictício para demonstrar a loja. Som compacto para qualquer ambiente.",
                    "249.00", 8, 800, "/demo/caixa.svg", TipoProduto.FISICO);
            produto(produtos, casa, "Luminária Arco · demonstração", "Produto fictício para demonstrar a loja. Luz acolhedora para seu canto favorito.",
                    "129.90", 16, 1200, "/demo/luminaria.svg", TipoProduto.FISICO);
            produto(produtos, casa, "Caneca Terra · demonstração", "Produto fictício para demonstrar a loja. Cerâmica para a rotina de todos os dias.",
                    "59.90", 25, 450, "/demo/caneca.svg", TipoProduto.FISICO);
            produto(produtos, estilo, "Mochila Nômade · demonstração", "Produto fictício para demonstrar a loja. Espaço e leveza para acompanhar você.",
                    "219.00", 10, 650, "/demo/mochila.svg", TipoProduto.FISICO);
            produto(produtos, digital, "Guia Criativo · demonstração", "Produto digital fictício para testar o catálogo. Nenhum arquivo é entregue nesta versão.",
                    "39.90", 0, null, "/demo/guia.svg", TipoProduto.DIGITAL);
        };
    }

    private Categoria categoria(CategoriaRepository repositorio, String nome, String slug) {
        Categoria categoria = new Categoria();
        categoria.setNome(nome);
        categoria.setSlug(slug);
        return repositorio.save(categoria);
    }

    private void produto(ProdutoRepository repositorio, Categoria categoria, String nome, String descricao,
            String preco, int estoque, Integer pesoGramas, String imagem, TipoProduto tipo) {
        Produto produto = new Produto();
        produto.setCategoria(categoria);
        produto.setNome(nome);
        produto.setDescricao(descricao);
        produto.setPreco(new BigDecimal(preco));
        produto.setEstoque(estoque);
        produto.setPesoGramas(pesoGramas);
        produto.setTipo(tipo);
        produto.setImagens(List.of(imagem));
        repositorio.save(produto);
    }
}
