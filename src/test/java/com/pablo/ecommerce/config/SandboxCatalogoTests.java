package com.pablo.ecommerce.config;

import com.pablo.ecommerce.categoria.CategoriaRepository;
import com.pablo.ecommerce.produto.Produto;
import com.pablo.ecommerce.produto.ProdutoRepository;
import com.pablo.ecommerce.produto.TipoProduto;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SandboxCatalogoTests {
    @Test void sandboxSeedsOnlyPurchasablePhysicalProducts() throws Exception {
        var categorias = mock(CategoriaRepository.class);
        var produtos = mock(ProdutoRepository.class);
        var environment = new MockEnvironment();
        environment.setActiveProfiles("sandbox");
        when(categorias.save(any())).thenAnswer(call -> call.getArgument(0));
        new DemoCatalogoConfig().catalogoDemonstracao(categorias, produtos, environment).run(null);
        var saved = ArgumentCaptor.forClass(Produto.class);
        verify(produtos, times(5)).save(saved.capture());
        assertThat(saved.getAllValues()).allSatisfy(produto -> {
            assertThat(produto.getTipo()).isEqualTo(TipoProduto.FISICO);
            assertThat(produto.getEstoque()).isPositive();
            assertThat(produto.getImagens()).hasSize(1);
        });
        assertThat(new LojaConfigController(environment).config())
                .containsEntry("demo", false).containsEntry("pagamentoTeste", true);
    }

    @Test void existingCatalogIsNeverSeededAgain() throws Exception {
        var categorias = mock(CategoriaRepository.class);
        var produtos = mock(ProdutoRepository.class);
        when(produtos.count()).thenReturn(5L);
        new DemoCatalogoConfig().catalogoDemonstracao(categorias, produtos, new MockEnvironment()).run(null);
        verify(produtos, never()).save(any());
        verify(categorias, never()).save(any());
    }
}
