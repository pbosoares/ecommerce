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
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
        mvc.perform(get("/index.html")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"tabs\"")));
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
}
