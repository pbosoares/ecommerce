package com.pablo.ecommerce.auth;

import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;

import com.jayway.jsonpath.JsonPath;
import com.pablo.ecommerce.produto.ProdutoRepository;
import com.pablo.ecommerce.usuario.Usuario;
import com.pablo.ecommerce.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class JwtIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired ProdutoRepository produtos;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtEncoder encoder;
    @Autowired JwtDecoder decoder;
    private Long usuarioId;

    @BeforeEach
    void preparar() {
        produtos.deleteAll();
        usuarios.deleteAll();
        Usuario usuario = new Usuario();
        usuario.setNome("Ana");
        usuario.setEmail("ana@example.com");
        usuario.setSenha(passwords.encode("senha-segura"));
        usuarioId = usuarios.save(usuario).getId();
    }

    private String login() throws Exception {
        var result = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@example.com\",\"senha\":\"senha-segura\"}"))
                .andExpect(status().isOk()).andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private String assinar(JwtEncoder signer, String issuer, Instant expiration, String subject) {
        var claims = JwtClaimsSet.builder().issuer(issuer).subject(subject).issuedAt(Instant.now().minusSeconds(120));
        if (expiration != null) claims.expiresAt(expiration);
        return signer.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build()))
                .getTokenValue();
    }

    @Test
    void tokenIdentificaUsuarioETemValidadeLimitada() throws Exception {
        Jwt token = decoder.decode(login());
        assertThat(token.getSubject()).isEqualTo(usuarioId.toString());
        assertThat(token.getClaimAsString("iss")).isEqualTo("ecommerce");
        assertThat(token.getExpiresAt()).isEqualTo(token.getIssuedAt().plusSeconds(900));
        assertThat(token.getClaims()).doesNotContainKeys("senha", "email");
    }

    @Test
    void crudExigeTokenEmTodosOsEndpoints() throws Exception {
        for (HttpMethod method : new HttpMethod[]{HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE}) {
            String path = method == HttpMethod.PUT || method == HttpMethod.DELETE ? "/produtos/1" : "/produtos";
            mvc.perform(request(method, path).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/produtos/1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/usuarios")).andExpect(status().isUnauthorized());
        mvc.perform(get("/auth/login")).andExpect(status().isUnauthorized());
        mvc.perform(get("/outro-endpoint")).andExpect(status().isUnauthorized());
        assertThat(produtos.count()).isZero();
    }

    @Test
    void tokenValidoPermiteCrudCompleto() throws Exception {
        String bearer = "Bearer " + login();
        String body = "{\"nome\":\"Teclado\",\"descricao\":\"USB\",\"preco\":100,\"estoque\":2}";
        var created = mvc.perform(post("/produtos").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        Number id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        mvc.perform(get("/produtos").header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/produtos/{id}", id).header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Teclado"));
        mvc.perform(put("/produtos/{id}", id).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(body.replace("Teclado", "Mouse")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Mouse"));
        mvc.perform(delete("/produtos/{id}", id).header("Authorization", bearer)).andExpect(status().isOk());
        mvc.perform(get("/produtos/{id}", id).header("Authorization", bearer)).andExpect(status().isNotFound());
    }

    @Test
    void rejeitaTokenExpiradoOuComEmissorIncorreto() throws Exception {
        for (String token : new String[]{
                assinar(encoder, "ecommerce", Instant.now().minusSeconds(10), "1"),
                assinar(encoder, "outro", Instant.now().plusSeconds(300), "1"),
                assinar(encoder, "ecommerce", null, "1"),
                assinar(encoder, "ecommerce", Instant.now().plusSeconds(300), "")}) {
            mvc.perform(get("/produtos").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void rejeitaTokenAdulteradoMalformadoOuAssinadoComOutraChave() throws Exception {
        var otherEncoder = NimbusJwtEncoder.withSecretKey(new SecretKeySpec(new byte[32], "HmacSHA256"))
                .algorithm(MacAlgorithm.HS256).build();
        String token = login();
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"999\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8)) + "." + parts[2];
        for (String invalid : new String[]{"invalido", tampered,
                assinar(otherEncoder, "ecommerce", Instant.now().plusSeconds(300), "1")}) {
            mvc.perform(get("/produtos").header("Authorization", "Bearer " + invalid))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void loginNaoAutenticaRequisicoesSeguintesSemBearer() throws Exception {
        String token = login();
        var result = mvc.perform(get("/produtos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
        mvc.perform(get("/produtos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/produtos").header("Authorization", "Basic YW5hOnNlbmhh"))
                .andExpect(status().isUnauthorized());
    }
}
