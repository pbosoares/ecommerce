package com.pablo.ecommerce.usuario;

import com.jayway.jsonpath.JsonPath;
import com.pablo.ecommerce.auth.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MinhaContaIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired TokenService tokens;

    private String entrar(String email) {
        Usuario usuario = new Usuario();
        usuario.setNome("Cliente");
        usuario.setEmail(email);
        usuario.setSenha("hash-nao-usado-pelo-teste");
        usuarios.save(usuario);
        return "Bearer " + tokens.emitir(usuario).accessToken();
    }

    private String endereco() {
        return """
                {"apelido":"Casa","endereco":{"cep":"01001000","logradouro":"Praça da Sé",
                "numero":"10","complemento":"Apto 2","bairro":"Sé","cidade":"São Paulo","uf":"SP"}}
                """;
    }

    @Test
    void exigeAutenticacao() throws Exception {
        mvc.perform(get("/minha-conta")).andExpect(status().isUnauthorized());
        mvc.perform(get("/minha-conta/enderecos")).andExpect(status().isUnauthorized());
        mvc.perform(post("/minha-conta/enderecos").contentType(MediaType.APPLICATION_JSON).content(endereco()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void perfilRetornaApenasDadosPublicosEAtualizaNomeSemMudarPapel() throws Exception {
        String auth = entrar("conta@teste.com");
        mvc.perform(get("/minha-conta").header("Authorization", auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("conta@teste.com"))
                .andExpect(jsonPath("$.senha").doesNotExist()).andExpect(jsonPath("$.papel").doesNotExist());
        mvc.perform(put("/minha-conta").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\" Ana \",\"papel\":\"ADMIN\",\"email\":\"outro@teste.com\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Ana"))
                .andExpect(jsonPath("$.email").value("conta@teste.com"));
        org.assertj.core.api.Assertions.assertThat(usuarios.findByEmail("conta@teste.com").orElseThrow().getPapel())
                .isEqualTo(Papel.CLIENTE);
    }

    @Test
    void enderecosSaoPersistidosEIsoladosPorCliente() throws Exception {
        String ana = entrar("ana-conta@teste.com");
        String bia = entrar("bia-conta@teste.com");
        var result = mvc.perform(post("/minha-conta/enderecos").header("Authorization", ana)
                .contentType(MediaType.APPLICATION_JSON).content(endereco())).andExpect(status().isCreated()).andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        mvc.perform(get("/minha-conta/enderecos").header("Authorization", ana)).andExpect(jsonPath("$[0].endereco.cep").value("01001000"));
        mvc.perform(get("/minha-conta/enderecos").header("Authorization", bia)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(put("/minha-conta/enderecos/" + id).header("Authorization", bia).contentType(MediaType.APPLICATION_JSON).content(endereco()))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/minha-conta/enderecos/" + id).header("Authorization", bia)).andExpect(status().isNotFound());
        mvc.perform(put("/minha-conta/enderecos/" + id).header("Authorization", ana).contentType(MediaType.APPLICATION_JSON)
                .content(endereco().replace("Casa", "Trabalho"))).andExpect(jsonPath("$.apelido").value("Trabalho"));
        mvc.perform(delete("/minha-conta/enderecos/" + id).header("Authorization", ana)).andExpect(status().isNoContent());
        mvc.perform(get("/minha-conta/enderecos").header("Authorization", ana)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejeitaDadosInvalidos() throws Exception {
        String auth = entrar("validacao-conta@teste.com");
        mvc.perform(post("/minha-conta/enderecos").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content(endereco().replace("01001000", "123"))).andExpect(status().isBadRequest());
        mvc.perform(post("/minha-conta/enderecos").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"apelido\":\"Casa\"}")).andExpect(status().isBadRequest());
        mvc.perform(put("/minha-conta").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"   \"}")).andExpect(status().isBadRequest());
    }
}
