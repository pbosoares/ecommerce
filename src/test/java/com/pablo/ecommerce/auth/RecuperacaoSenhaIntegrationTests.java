package com.pablo.ecommerce.auth;

import java.time.Instant;
import java.util.UUID;
import com.jayway.jsonpath.JsonPath;
import com.pablo.ecommerce.compra.EmailNotificacaoRepository;
import com.pablo.ecommerce.usuario.Usuario;
import com.pablo.ecommerce.usuario.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RecuperacaoSenhaIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired RecuperacaoSenhaRepository recuperacoes;
    @Autowired EmailNotificacaoRepository emails;
    @Autowired PasswordEncoder encoder;
    @Autowired TokenService tokens;
    @Autowired RecuperacaoSenhaService service;

    private Usuario cliente() {
        Usuario usuario = new Usuario();
        usuario.setNome("Cliente"); usuario.setEmail(UUID.randomUUID() + "@example.com");
        usuario.setSenha(encoder.encode("senha-antiga"));
        return usuarios.save(usuario);
    }

    private String solicitar(String email) throws Exception {
        return mvc.perform(post("/auth/recuperar-senha").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isAccepted()).andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse().getContentAsString();
    }

    private String token(Usuario usuario) {
        var email = emails.findAll().stream().filter(item -> usuario.getEmail().equals(item.getDestinatario())
                && item.getRecuperacaoUrl() != null).reduce((a, b) -> b).orElseThrow();
        return email.getRecuperacaoUrl().split("#redefinir-senha=")[1];
    }

    private org.springframework.test.web.servlet.ResultActions redefinir(String token, String senha) throws Exception {
        return mvc.perform(post("/auth/redefinir-senha").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"novaSenha\":\"" + senha + "\"}"));
    }

    @Test
    void respostaNaoRevelaContaEReenvioImediatoNaoDuplicaFila() throws Exception {
        var usuario = cliente();
        String resposta = solicitar(usuario.getEmail());
        assertThat(solicitar("desconhecido@example.com")).isEqualTo(resposta);
        long quantidade = emails.count();
        solicitar(usuario.getEmail());
        assertThat(emails.count()).isEqualTo(quantidade);
        String token = token(usuario);
        assertThat(token).hasSize(43);
        assertThat(resposta).doesNotContain(token);
        var registro = recuperacoes.findByTokenHash(RecuperacaoSenhaService.hash(token)).orElseThrow();
        assertThat(registro.getTokenHash()).doesNotContain(token);
        assertThat(registro.getExpiraEm()).isEqualTo(registro.getCriadoEm().plusSeconds(1800));
    }

    @Test
    void redefineUmaVezEInvalidaSenhaETokenDeAcessoAnteriores() throws Exception {
        var usuario = cliente();
        String sessaoAntiga = tokens.emitir(usuario).accessToken();
        solicitar(usuario.getEmail());
        String token = token(usuario);
        redefinir(token, "senha-nova-segura").andExpect(status().isNoContent());
        assertThat(encoder.matches("senha-nova-segura", usuarios.findById(usuario.getId()).orElseThrow().getSenha())).isTrue();
        redefinir(token, "outra-senha-segura").andExpect(status().isBadRequest());
        mvc.perform(get("/minha-conta").header("Authorization", "Bearer " + sessaoAntiga)).andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + usuario.getEmail() + "\",\"senha\":\"senha-antiga\"}"))
                .andExpect(status().isUnauthorized());
        var login = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + usuario.getEmail() + "\",\"senha\":\"senha-nova-segura\"}"))
                .andExpect(status().isOk()).andReturn();
        String novaSessao = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
        mvc.perform(get("/minha-conta").header("Authorization", "Bearer " + novaSessao)).andExpect(status().isOk());
    }

    @Test
    void rejeitaExpiracaoTokenDesconhecidoESenhaCurtaSemConsumirLink() throws Exception {
        var usuario = cliente(); solicitar(usuario.getEmail());
        String token = token(usuario);
        redefinir("A".repeat(43), "senha-segura").andExpect(status().isBadRequest());
        redefinir(token, "curta").andExpect(status().isBadRequest());
        redefinir(token, " ".repeat(10)).andExpect(status().isBadRequest());
        var registro = recuperacoes.findByTokenHash(RecuperacaoSenhaService.hash(token)).orElseThrow();
        assertThat(registro.getUsadoEm()).isNull();
        registro.setExpiraEm(Instant.now().minusSeconds(1));
        recuperacoes.flush();
        redefinir(token, "senha-segura").andExpect(status().isBadRequest());
    }

    @Test
    void novoPedidoDeRecuperacaoInvalidaLinkAnterior() throws Exception {
        var usuario = cliente(); solicitar(usuario.getEmail());
        String anterior = token(usuario);
        var registro = recuperacoes.findByTokenHash(RecuperacaoSenhaService.hash(anterior)).orElseThrow();
        registro.setCriadoEm(Instant.now().minusSeconds(61)); recuperacoes.flush();
        solicitar(usuario.getEmail());
        String novo = token(usuario);
        assertThat(novo).isNotEqualTo(anterior);
        redefinir(anterior, "senha-segura").andExpect(status().isBadRequest());
        redefinir(novo, "senha-segura").andExpect(status().isNoContent());
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void duasTentativasSimultaneasConsomemLinkUmaUnicaVez() throws Exception {
        var usuario = cliente();
        try {
            solicitar(usuario.getEmail());
            String token = token(usuario);
            var start = new java.util.concurrent.CountDownLatch(1);
            try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
                java.util.concurrent.Callable<Integer> tentativa = () -> {
                    start.await();
                    try { service.redefinir(token, "nova-senha-segura"); return 204; }
                    catch (org.springframework.web.server.ResponseStatusException exception) { return exception.getStatusCode().value(); }
                };
                var primeira = executor.submit(tentativa);
                var segunda = executor.submit(tentativa);
                start.countDown();
                assertThat(java.util.List.of(primeira.get(15, java.util.concurrent.TimeUnit.SECONDS),
                        segunda.get(15, java.util.concurrent.TimeUnit.SECONDS))).containsExactlyInAnyOrder(204, 400);
            }
            assertThat(usuarios.findById(usuario.getId()).orElseThrow().getSenhaVersao()).isEqualTo(1);
            assertThat(emails.findByDestinatarioAndRecuperacaoUrlIsNotNull(usuario.getEmail())).isEmpty();
        } finally {
            recuperacoes.deleteAll(recuperacoes.findAll().stream()
                    .filter(item -> item.getUsuario().getId().equals(usuario.getId())).toList());
            emails.deleteAll(emails.findAll().stream().filter(item -> usuario.getEmail().equals(item.getDestinatario())).toList());
            usuarios.deleteById(usuario.getId());
        }
    }
}
