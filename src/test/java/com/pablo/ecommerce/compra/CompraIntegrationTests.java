package com.pablo.ecommerce.compra;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import com.pablo.ecommerce.categoria.CategoriaRepository;
import com.pablo.ecommerce.produto.ProdutoRepository;
import com.pablo.ecommerce.produto.DigitalArquivoRepository;
import com.pablo.ecommerce.usuario.Papel;
import com.pablo.ecommerce.usuario.Usuario;
import com.pablo.ecommerce.usuario.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.digital.storage-path=target/test-digital-files")
@AutoConfigureMockMvc
class CompraIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired CategoriaRepository categorias;
    @Autowired ProdutoRepository produtos;
    @Autowired CarrinhoItemRepository carrinhoItens;
    @Autowired PedidoRepository pedidos;
    @Autowired FaixaFreteRepository faixas;
    @Autowired DigitalArquivoRepository arquivos;
    @Autowired PedidoService pedidoService;
    @Autowired PasswordEncoder encoder;
    private String admin;
    private String ana;
    private String bia;
    private Number categoriaId;

    @BeforeEach
    void preparar() throws Exception {
        limpar();
        admin = entrar("admin@exemplo.com", Papel.ADMIN);
        ana = entrar("ana@exemplo.com", Papel.CLIENTE);
        bia = entrar("bia@exemplo.com", Papel.CLIENTE);
        var categoria = mvc.perform(post("/categorias").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Diversos\",\"slug\":\"diversos\"}"))
                .andExpect(status().isOk()).andReturn();
        categoriaId = JsonPath.read(categoria.getResponse().getContentAsString(), "$.id");
        mvc.perform(post("/frete/faixas").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cepInicio\":\"00000000\",\"cepFim\":\"99999999\",\"pesoMinimoGramas\":0,\"pesoMaximoGramas\":100000,\"valor\":15}"))
                .andExpect(status().isCreated());
    }

    @AfterEach
    void limpar() {
        pedidos.deleteAll();
        faixas.deleteAll();
        carrinhoItens.deleteAll();
        arquivos.deleteAll();
        produtos.deleteAll();
        categorias.deleteAll();
        usuarios.deleteAll();
    }

    private String entrar(String email, Papel papel) throws Exception {
        Usuario usuario = new Usuario();
        usuario.setNome(email);
        usuario.setEmail(email);
        usuario.setSenha(encoder.encode("senha-segura"));
        usuario.setPapel(papel);
        usuarios.save(usuario);
        var result = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"senha-segura\"}"))
                .andExpect(status().isOk()).andReturn();
        return "Bearer " + JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private Number produto(String nome, int preco, int estoque, String tipo) throws Exception {
        var result = mvc.perform(post("/produtos").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"" + nome + "\",\"preco\":" + preco
                        + ",\"estoque\":" + estoque + ",\"tipo\":\"" + tipo
                        + "\",\"categoriaId\":" + categoriaId
                        + ("FISICO".equals(tipo) ? ",\"pesoGramas\":500" : "") + "}"))
                .andExpect(status().isOk()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private String adicionar(Number produtoId, int quantidade) {
        return "{\"produtoId\":" + produtoId + ",\"quantidade\":" + quantidade + "}";
    }

    private String endereco() {
        return """
                {"entrega":{"cep":"01001000","logradouro":"Praca da Se","numero":"1",
                 "bairro":"Se","cidade":"Sao Paulo","uf":"SP"}}
                """;
    }

    @Test
    void carrinhoPertenceAoUsuarioECalculaPrecoNoServidor() throws Exception {
        Number livro = produto("Livro", 40, 3, "FISICO");
        mvc.perform(get("/carrinho")).andExpect(status().isUnauthorized());
        mvc.perform(get("/pedidos")).andExpect(status().isUnauthorized());
        mvc.perform(post("/carrinho/itens").header("Authorization", ana)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"produtoId\":" + livro + ",\"quantidade\":2,\"precoUnitario\":0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.subtotal").value(80));
        mvc.perform(get("/carrinho").header("Authorization", bia))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens.length()").value(0));
        mvc.perform(put("/carrinho/itens/{id}", livro).header("Authorization", bia)
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantidade\":1}"))
                .andExpect(status().isNotFound());
        mvc.perform(put("/carrinho/itens/{id}", livro).header("Authorization", ana)
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantidade\":3}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.subtotal").value(120));
        mvc.perform(post("/carrinho/itens").header("Authorization", ana)
                .contentType(MediaType.APPLICATION_JSON).content(adicionar(livro, 1)))
                .andExpect(status().isConflict());
        mvc.perform(delete("/carrinho/itens/{id}", livro).header("Authorization", ana))
                .andExpect(status().isNoContent());
        mvc.perform(get("/carrinho").header("Authorization", ana))
                .andExpect(jsonPath("$.itens.length()").value(0));
    }

    @Test
    void pedidoFisicoExigeEnderecoPreservaValoresEIsolaHistorico() throws Exception {
        Number livro = produto("Livro", 40, 5, "FISICO");
        Number curso = produto("Curso", 50, 0, "DIGITAL");
        for (Number id : new Number[]{livro, curso}) {
            mvc.perform(post("/carrinho/itens").header("Authorization", ana)
                    .contentType(MediaType.APPLICATION_JSON).content(adicionar(id, 2)))
                    .andExpect(status().isOk());
        }
        mvc.perform(post("/pedidos").header("Authorization", ana))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/pedidos").header("Authorization", ana)
                .contentType(MediaType.APPLICATION_JSON).content("{\"entrega\":{\"cep\":\"123\"}}"))
                .andExpect(status().isBadRequest());
        var result = mvc.perform(post("/pedidos").header("Authorization", ana)
                .contentType(MediaType.APPLICATION_JSON).content(endereco()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_PAGAMENTO"))
                .andExpect(jsonPath("$.subtotal").value(180))
                .andExpect(jsonPath("$.total").value(195))
                .andExpect(jsonPath("$.frete").value(15))
                .andExpect(jsonPath("$.entrega.cep").value("01001000"))
                .andReturn();
        Number pedidoId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        mvc.perform(get("/carrinho").header("Authorization", ana))
                .andExpect(jsonPath("$.itens.length()").value(0));
        mvc.perform(get("/pedidos/{id}", pedidoId).header("Authorization", bia))
                .andExpect(status().isNotFound());
        mvc.perform(get("/pedidos").header("Authorization", bia))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(put("/produtos/{id}", livro).header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Livro novo\",\"preco\":99,\"estoque\":5,\"categoriaId\":" + categoriaId + "}"))
                .andExpect(status().isOk());
        mvc.perform(get("/pedidos/{id}", pedidoId).header("Authorization", ana))
                .andExpect(status().isOk()).andExpect(jsonPath("$.subtotal").value(180))
                .andExpect(jsonPath("$.itens[0].nome").value("Livro"))
                .andExpect(jsonPath("$.itens[0].precoUnitario").value(40));
        assertThat(produtos.findById(livro.longValue()).orElseThrow().getEstoque()).isEqualTo(5);
    }

    @Test
    void pedidoDigitalDispensaEnderecoEProdutoExcluidoSaiDoCarrinho() throws Exception {
        Number curso = produto("Curso", 50, 0, "DIGITAL");
        mvc.perform(post("/carrinho/itens").header("Authorization", ana)
                .contentType(MediaType.APPLICATION_JSON).content(adicionar(curso, 1)))
                .andExpect(status().isOk());
        mvc.perform(post("/frete/cotacoes").header("Authorization", ana)
                .contentType(MediaType.APPLICATION_JSON).content("{\"cep\":\"01001000\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pesoGramas").value(0))
                .andExpect(jsonPath("$.frete").value(0))
                .andExpect(jsonPath("$.total").value(50));
        var result = mvc.perform(post("/pedidos").header("Authorization", ana))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("AGUARDANDO_PAGAMENTO"))
                .andExpect(jsonPath("$.total").value(50)).andReturn();
        Number pedidoId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        mvc.perform(post("/pedidos").header("Authorization", ana)).andExpect(status().isBadRequest());
        mvc.perform(post("/carrinho/itens").header("Authorization", ana)
                .contentType(MediaType.APPLICATION_JSON).content(adicionar(curso, 1)))
                .andExpect(status().isOk());
        mvc.perform(delete("/produtos/{id}", curso).header("Authorization", admin))
                .andExpect(status().isOk());
        mvc.perform(get("/carrinho").header("Authorization", ana))
                .andExpect(jsonPath("$.itens.length()").value(0));
        mvc.perform(get("/pedidos/{id}", pedidoId).header("Authorization", ana))
                .andExpect(jsonPath("$.itens[0].nome").value("Curso"));
    }

    @Test
    void reservaEstoqueEApenasCompradorPodeCancelarAntesDoCheckout() throws Exception {
        Number id = produto("Ultimo", 30, 1, "FISICO");
        mvc.perform(post("/carrinho/itens").header("Authorization", ana)
                .contentType(MediaType.APPLICATION_JSON).content(adicionar(id, 1)))
                .andExpect(status().isOk());
        var criado = mvc.perform(post("/pedidos").header("Authorization", ana)
                .header("Idempotency-Key", "pedido-reserva-0001")
                .contentType(MediaType.APPLICATION_JSON).content(endereco()))
                .andExpect(status().isCreated()).andReturn();
        Number pedidoId = JsonPath.read(criado.getResponse().getContentAsString(), "$.id");
        mvc.perform(post("/pedidos").header("Authorization", ana)
                .header("Idempotency-Key", "pedido-reserva-0001")
                .contentType(MediaType.APPLICATION_JSON).content(endereco()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(pedidoId));
        assertThat(pedidos.count()).isEqualTo(1);
        assertThat(produtos.findById(id.longValue()).orElseThrow().getEstoque()).isZero();
        mvc.perform(post("/pedidos/{id}/cancelar", pedidoId).header("Authorization", bia))
                .andExpect(status().isNotFound());
        mvc.perform(post("/pedidos/{id}/checkout", pedidoId).header("Authorization", bia))
                .andExpect(status().isNotFound());
        mvc.perform(post("/pedidos/{id}/checkout", pedidoId).header("Authorization", ana))
                .andExpect(status().isServiceUnavailable());
        mvc.perform(post("/pedidos/{id}/cancelar", pedidoId).header("Authorization", ana))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADO"));
        assertThat(produtos.findById(id.longValue()).orElseThrow().getEstoque()).isEqualTo(1);
        mvc.perform(post("/pedidos/{id}/cancelar", pedidoId).header("Authorization", ana))
                .andExpect(status().isConflict());
    }

    @Test
    void downloadDigitalExigeArquivoPagamentoEDonoDoPedido() throws Exception {
        Number curso = produto("Guia", 25, 0, "DIGITAL");
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "guia.txt", "text/plain",
                "conteudo particular".getBytes());
        mvc.perform(multipart("/produtos/{id}/arquivo", curso).file(arquivo).header("Authorization", ana))
                .andExpect(status().isForbidden());
        mvc.perform(multipart("/produtos/{id}/arquivo", curso).file(arquivo).header("Authorization", admin))
                .andExpect(status().isNoContent());
        mvc.perform(post("/carrinho/itens").header("Authorization", ana)
                .contentType(MediaType.APPLICATION_JSON).content(adicionar(curso, 1)))
                .andExpect(status().isOk());
        var criado = mvc.perform(post("/pedidos").header("Authorization", ana))
                .andExpect(status().isCreated()).andReturn();
        Number pedidoId = JsonPath.read(criado.getResponse().getContentAsString(), "$.id");
        Number itemId = JsonPath.read(criado.getResponse().getContentAsString(), "$.itens[0].id");
        mvc.perform(get("/pedidos/{id}/itens/{item}/download", pedidoId, itemId)
                .header("Authorization", ana)).andExpect(status().isForbidden());
        mvc.perform(get("/pedidos/{id}/itens/{item}/download", pedidoId, itemId)
                .header("Authorization", bia)).andExpect(status().isNotFound());
        var pedido = pedidos.findById(pedidoId.longValue()).orElseThrow();
        pedido.setCheckoutSessionId("cs_test_download");
        pedidos.saveAndFlush(pedido);
        pedidoService.confirmarPagamento(pedidoId.longValue(), "cs_test_download", 2500, "brl");
        mvc.perform(get("/pedidos/{id}/itens/{item}/download", pedidoId, itemId)
                .header("Authorization", ana))
                .andExpect(status().isOk())
                .andExpect(content().bytes("conteudo particular".getBytes()));
    }

    @Test
    void pedidoSemCheckoutExpiraELiberaReservaUmaVez() throws Exception {
        Number id = produto("Reservado", 30, 1, "FISICO");
        mvc.perform(post("/carrinho/itens").header("Authorization", ana)
                .contentType(MediaType.APPLICATION_JSON).content(adicionar(id, 1)))
                .andExpect(status().isOk());
        var criado = mvc.perform(post("/pedidos").header("Authorization", ana)
                .contentType(MediaType.APPLICATION_JSON).content(endereco()))
                .andExpect(status().isCreated()).andReturn();
        Number pedidoId = JsonPath.read(criado.getResponse().getContentAsString(), "$.id");
        var pedido = pedidos.findById(pedidoId.longValue()).orElseThrow();
        pedido.setCriadoEm(Instant.now().minusSeconds(1900));
        pedidos.saveAndFlush(pedido);
        mvc.perform(post("/pedidos/{id}/checkout", pedidoId).header("Authorization", ana))
                .andExpect(status().isConflict());
        pedidoService.expirarSemCheckout(pedidoId.longValue());
        pedidoService.expirarSemCheckout(pedidoId.longValue());
        assertThat(pedidos.findById(pedidoId.longValue()).orElseThrow().getStatus()).isEqualTo(StatusPedido.EXPIRADO);
        assertThat(produtos.findById(id.longValue()).orElseThrow().getEstoque()).isEqualTo(1);
    }

    @Test
    void pedidoConfereEstoqueAntesDeReservar() throws Exception {
        Number livro = produto("Livro", 40, 2, "FISICO");
        mvc.perform(post("/carrinho/itens").header("Authorization", ana)
                .contentType(MediaType.APPLICATION_JSON).content(adicionar(livro, 0)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/carrinho/itens").header("Authorization", ana)
                .contentType(MediaType.APPLICATION_JSON).content(adicionar(livro, 3)))
                .andExpect(status().isConflict());
        mvc.perform(post("/carrinho/itens").header("Authorization", ana)
                .contentType(MediaType.APPLICATION_JSON).content(adicionar(livro, 2)))
                .andExpect(status().isOk());
        mvc.perform(put("/produtos/{id}", livro).header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Livro\",\"preco\":40,\"estoque\":1,\"categoriaId\":" + categoriaId + "}"))
                .andExpect(status().isOk());
        mvc.perform(post("/pedidos").header("Authorization", ana)
                .contentType(MediaType.APPLICATION_JSON).content(endereco()))
                .andExpect(status().isConflict());
        assertThat(pedidos.count()).isZero();
        mvc.perform(get("/carrinho").header("Authorization", ana))
                .andExpect(jsonPath("$.itens.length()").value(1));
    }

    @Test
    void cotacaoExigeAutenticacaoUsaPesoDosFisicosEProtegeTabela() throws Exception {
        Number livro = produto("Livro", 40, 5, "FISICO");
        Number curso = produto("Curso", 50, 0, "DIGITAL");
        for (Number id : new Number[]{livro, curso}) {
            mvc.perform(post("/carrinho/itens").header("Authorization", ana)
                    .contentType(MediaType.APPLICATION_JSON).content(adicionar(id, 2)))
                    .andExpect(status().isOk());
        }
        mvc.perform(post("/frete/cotacoes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"cep\":\"01001000\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/frete/cotacoes").header("Authorization", ana)
                .contentType(MediaType.APPLICATION_JSON).content("{\"cep\":\"123\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/frete/cotacoes").header("Authorization", ana)
                .contentType(MediaType.APPLICATION_JSON).content("{\"cep\":\"01001000\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pesoGramas").value(1000))
                .andExpect(jsonPath("$.subtotal").value(180))
                .andExpect(jsonPath("$.frete").value(15))
                .andExpect(jsonPath("$.total").value(195));
        mvc.perform(post("/frete/cotacoes").header("Authorization", bia)
                .contentType(MediaType.APPLICATION_JSON).content("{\"cep\":\"01001000\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/frete/faixas").header("Authorization", ana)).andExpect(status().isForbidden());
        mvc.perform(post("/frete/faixas").header("Authorization", ana)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cepInicio\":\"00000000\",\"cepFim\":\"99999999\",\"pesoMinimoGramas\":0,\"pesoMaximoGramas\":1000,\"valor\":1}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/frete/faixas").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cepInicio\":\"00000000\",\"cepFim\":\"99999999\",\"pesoMinimoGramas\":0,\"pesoMaximoGramas\":1000,\"valor\":1}"))
                .andExpect(status().isConflict());
    }

    @Test
    void tarifaPadraoFuncionaSemFaixasEMasProdutoSemPesoNaoFechaPedido() throws Exception {
        Number livro = produto("Livro", 40, 5, "FISICO");
        mvc.perform(post("/carrinho/itens").header("Authorization", ana)
                .contentType(MediaType.APPLICATION_JSON).content(adicionar(livro, 1)))
                .andExpect(status().isOk());
        faixas.deleteAll();
        mvc.perform(post("/frete/cotacoes").header("Authorization", ana)
                .contentType(MediaType.APPLICATION_JSON).content("{\"cep\":\"01001000\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.frete").value(15));
        assertThat(pedidos.count()).isZero();
        mvc.perform(get("/carrinho").header("Authorization", ana))
                .andExpect(jsonPath("$.itens.length()").value(1));
        mvc.perform(put("/produtos/{id}", livro).header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Livro\",\"preco\":40,\"estoque\":5,\"categoriaId\":" + categoriaId + "}"))
                .andExpect(status().isOk());
        mvc.perform(post("/frete/cotacoes").header("Authorization", ana)
                .contentType(MediaType.APPLICATION_JSON).content("{\"cep\":\"01001000\"}"))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(post("/pedidos").header("Authorization", ana)
                .contentType(MediaType.APPLICATION_JSON).content(endereco()))
                .andExpect(status().isUnprocessableContent());
        assertThat(pedidos.count()).isZero();
        mvc.perform(get("/carrinho").header("Authorization", ana))
                .andExpect(jsonPath("$.itens.length()").value(1));
        mvc.perform(put("/produtos/{id}", livro).header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Livro\",\"preco\":40,\"estoque\":5,\"categoriaId\":" + categoriaId
                        + ",\"pesoGramas\":500}"))
                .andExpect(status().isOk());
        mvc.perform(post("/pedidos").header("Authorization", ana)
                .contentType(MediaType.APPLICATION_JSON).content(endereco()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.frete").value(15))
                .andExpect(jsonPath("$.total").value(55));
    }

    @Test
    void faixasEscolhemTarifaPorCepEPeso() throws Exception {
        faixas.deleteAll();
        for (String faixa : new String[]{
                "{\"cepInicio\":\"01000000\",\"cepFim\":\"01999999\",\"pesoMinimoGramas\":0,\"pesoMaximoGramas\":500,\"valor\":10}",
                "{\"cepInicio\":\"01000000\",\"cepFim\":\"01999999\",\"pesoMinimoGramas\":500,\"pesoMaximoGramas\":1000,\"valor\":20}"}) {
            mvc.perform(post("/frete/faixas").header("Authorization", admin)
                    .contentType(MediaType.APPLICATION_JSON).content(faixa))
                    .andExpect(status().isCreated());
        }
        Number livro = produto("Livro", 40, 5, "FISICO");
        mvc.perform(post("/carrinho/itens").header("Authorization", ana)
                .contentType(MediaType.APPLICATION_JSON).content(adicionar(livro, 1)))
                .andExpect(status().isOk());
        mvc.perform(post("/frete/cotacoes").header("Authorization", ana)
                .contentType(MediaType.APPLICATION_JSON).content("{\"cep\":\"01001000\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.frete").value(10));
        mvc.perform(put("/carrinho/itens/{id}", livro).header("Authorization", ana)
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantidade\":2}"))
                .andExpect(status().isOk());
        mvc.perform(post("/frete/cotacoes").header("Authorization", ana)
                .contentType(MediaType.APPLICATION_JSON).content("{\"cep\":\"01001000\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.frete").value(20));
        mvc.perform(post("/frete/cotacoes").header("Authorization", ana)
                .contentType(MediaType.APPLICATION_JSON).content("{\"cep\":\"20000000\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.frete").value(15));
    }
}
