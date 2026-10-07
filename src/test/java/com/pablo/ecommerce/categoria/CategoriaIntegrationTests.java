package com.pablo.ecommerce.categoria;

import com.jayway.jsonpath.JsonPath;
import com.pablo.ecommerce.produto.ProdutoRepository;
import com.pablo.ecommerce.usuario.Papel;
import com.pablo.ecommerce.usuario.Usuario;
import com.pablo.ecommerce.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CategoriaIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ProdutoRepository produtos;
    @Autowired CategoriaRepository categorias;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder passwords;
    private String admin;
    private String cliente;

    @BeforeEach
    void preparar() throws Exception {
        produtos.deleteAll();
        categorias.deleteAll();
        usuarios.deleteAll();
        admin = criarEEntrar("admin@example.com", Papel.ADMIN);
        cliente = criarEEntrar("cliente@example.com", Papel.CLIENTE);
    }

    private String criarEEntrar(String email, Papel papel) throws Exception {
        Usuario usuario = new Usuario();
        usuario.setNome(email);
        usuario.setEmail(email);
        usuario.setSenha(passwords.encode("senha-segura"));
        usuario.setPapel(papel);
        usuarios.save(usuario);
        var result = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"senha-segura\"}"))
                .andExpect(status().isOk()).andReturn();
        return "Bearer " + JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private Number criarCategoria(String nome, String slug) throws Exception {
        var result = mvc.perform(post("/categorias").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"" + nome + "\",\"slug\":\"" + slug + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private String produto(String nome, Number categoriaId) {
        return "{\"nome\":\"" + nome + "\",\"preco\":42,\"estoque\":3,\"categoriaId\":" + categoriaId + "}";
    }

    @Test
    void clienteVeCategoriasEProdutosFiltradosMasNaoPodeAlterarCatalogo() throws Exception {
        Number livros = criarCategoria("Livros", "livros");
        Number jogos = criarCategoria("Jogos", "jogos");
        var created = mvc.perform(post("/produtos").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content(produto("Romance", livros)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.categoria.slug").value("livros"))
                .andReturn();
        Number id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        mvc.perform(post("/produtos").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content(produto("Xadrez", jogos)))
                .andExpect(status().isOk());

        mvc.perform(get("/categorias")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/produtos?categoria=livros")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].nome").value("Romance"));
        mvc.perform(get("/produtos?categoria=jogos")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Xadrez"));
        mvc.perform(get("/produtos?categoria=desconhecida")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/produtos")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));

        mvc.perform(post("/categorias").header("Authorization", cliente)
                .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Fraude\",\"slug\":\"fraude\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/produtos/{id}", id).header("Authorization", cliente)
                .contentType(MediaType.APPLICATION_JSON).content(produto("Alterado", livros)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/produtos/{id}", id).header("Authorization", cliente))
                .andExpect(status().isForbidden());
        assertThat(produtos.findById(id.longValue()).orElseThrow().getNome()).isEqualTo("Romance");
        assertThat(categorias.existsBySlug("fraude")).isFalse();
    }

    @Test
    void administradorPodeMoverProdutoEntreCategorias() throws Exception {
        Number livros = criarCategoria("Livros", "livros");
        Number jogos = criarCategoria("Jogos", "jogos");
        var created = mvc.perform(post("/produtos").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content(produto("Romance", livros)))
                .andExpect(status().isOk()).andReturn();
        Number id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        mvc.perform(put("/produtos/{id}", id).header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content(produto("Romance", jogos)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.categoria.slug").value("jogos"));
        mvc.perform(get("/produtos?categoria=livros")).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/produtos?categoria=jogos")).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void rejeitaCategoriaDuplicadaInvalidaOuAusente() throws Exception {
        criarCategoria("Livros", "livros");
        mvc.perform(post("/categorias").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Outros livros\",\"slug\":\"livros\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/categorias").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"\",\"slug\":\"Invalido!\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/produtos").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content(produto("Sem categoria", 999999)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/produtos").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Sem categoria\",\"preco\":42,\"estoque\":3}"))
                .andExpect(status().isBadRequest());
        assertThat(produtos.count()).isZero();
    }

    @Test
    void cadastraProdutoFisicoComDadosGenericos() throws Exception {
        Number categoriaId = criarCategoria("Roupas", "roupas");
        String body = """
                {"nome":"Camiseta","descricao":"Algodao","preco":79.90,"estoque":12,
                 "categoriaId":%d,"tipo":"FISICO","sku":"CAM-PRETA-M",
                 "imagens":["https://exemplo.com/camiseta.jpg"],
                 "atributos":{"cor":"preta","tamanho":"M"},
                 "pesoGramas":250,"alturaCm":3,"larguraCm":20,"comprimentoCm":25}
                """.formatted(categoriaId);
        var result = mvc.perform(post("/produtos").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sku").value("CAM-PRETA-M"))
                .andExpect(jsonPath("$.tipo").value("FISICO"))
                .andExpect(jsonPath("$.imagens[0]").value("https://exemplo.com/camiseta.jpg"))
                .andExpect(jsonPath("$.atributos.tamanho").value("M"))
                .andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        mvc.perform(get("/produtos/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.pesoGramas").value(250));
        mvc.perform(put("/produtos/{id}", id).header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content(body.replace("Camiseta", "Camiseta nova")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Camiseta nova"));
        mvc.perform(post("/produtos").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        assertThat(produtos.count()).isEqualTo(1);
    }

    @Test
    void cadastraProdutoDigitalSemEstoqueOuFrete() throws Exception {
        Number categoriaId = criarCategoria("Cursos", "cursos");
        String body = """
                {"nome":"Curso de desenho","preco":49.90,"categoriaId":%d,
                 "tipo":"DIGITAL","sku":"CURSO-DESENHO",
                 "imagens":["https://exemplo.com/capa.png"],
                 "atributos":{"idioma":"portugues","formato":"video"}}
                """.formatted(categoriaId);
        mvc.perform(post("/produtos").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tipo").value("DIGITAL"))
                .andExpect(jsonPath("$.estoque").value(0))
                .andExpect(jsonPath("$.atributos.formato").value("video"));
        mvc.perform(get("/produtos?categoria=cursos")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("DIGITAL"));
        mvc.perform(post("/produtos").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content(body.replace("\"sku\":\"CURSO-DESENHO\",", "\"pesoGramas\":250,")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejeitaImagemInseguraEDadosInvalidosDoProdutoFisico() throws Exception {
        Number categoriaId = criarCategoria("Roupas", "roupas");
        mvc.perform(post("/produtos").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Camiseta\",\"preco\":20,\"categoriaId\":" + categoriaId + "}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/produtos").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Camiseta\",\"preco\":20,\"estoque\":1,\"categoriaId\":"
                        + categoriaId + ",\"imagens\":[\"javascript:alert(1)\"]}"))
                .andExpect(status().isBadRequest());
        assertThat(produtos.count()).isZero();
    }
}
