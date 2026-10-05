package com.pablo.ecommerce.auth;

import com.pablo.ecommerce.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class LoginIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;

    @BeforeEach
    void cadastrar() throws Exception {
        usuarios.deleteAll();
        mvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome":"Ana","email":"ana@example.com","senha":"senha-segura"}
                        """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.senha").doesNotExist());
    }

    @Test
    void loginValidaHashArgon2SemExporSenha() throws Exception {
        assertThat(usuarios.findByEmail("ana@example.com").orElseThrow().getSenha()).startsWith("$argon2");
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"ana@example.com","senha":"senha-segura"}
                        """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("ana@example.com"))
                .andExpect(jsonPath("$.senha").doesNotExist());
    }

    @Test
    void rejeitaCredenciaisIncorretas() throws Exception {
        for (String email : new String[]{"ana@example.com", "ausente@example.com"}) {
            mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + email + "\",\"senha\":\"errada\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void rejeitaCamposInvalidos() throws Exception {
        for (String body : new String[]{"{}", "{\"email\":\"invalido\",\"senha\":\"abc\"}",
                "{\"email\":\"ana@example.com\",\"senha\":\" \"}"}) {
            mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }
}
