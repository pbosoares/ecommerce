package com.pablo.ecommerce.auth;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import com.pablo.ecommerce.compra.EmailNotificacaoService;
import com.pablo.ecommerce.usuario.Usuario;
import com.pablo.ecommerce.usuario.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RecuperacaoSenhaService {
    private final RecuperacaoSenhaRepository recuperacoes;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final EmailNotificacaoService emails;
    private final String lojaUrl;
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;
    private final SecureRandom random = new SecureRandom();

    public RecuperacaoSenhaService(RecuperacaoSenhaRepository recuperacoes, UsuarioRepository usuarios,
            PasswordEncoder encoder, EmailNotificacaoService emails,
            @Value("${app.stripe.success-url:http://localhost:5173/}") String retornoUrl,
            @Value("${app.password-reset.url:}") String configurada, Environment environment) {
        this.recuperacoes = recuperacoes;
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.emails = emails;
        URI url = URI.create(configurada.isBlank() ? retornoUrl : configurada);
        boolean local = "http".equals(url.getScheme()) && "localhost".equals(url.getHost())
                && !environment.matchesProfiles("prod", "sandbox");
        if (url.getHost() == null || url.getUserInfo() != null || !("https".equals(url.getScheme()) || local)) {
            throw new IllegalArgumentException("URL de recuperacao deve ser HTTPS; localhost HTTP permitido apenas localmente");
        }
        lojaUrl = url.getScheme() + "://" + url.getAuthority() + "/";
    }

    @Transactional
    public void solicitar(String email) {
        Usuario encontrado = usuarios.findByEmail(email.trim()).orElse(null);
        if (encontrado == null) return;
        // Serialize both requests and password changes on the account, preventing two valid uses.
        Usuario usuario = usuarios.findByIdForUpdate(encontrado.getId()).orElseThrow();
        Instant agora = Instant.now();
        var anteriores = recuperacoes.findByUsuarioIdAndUsadoEmIsNull(usuario.getId());
        if (anteriores.stream().anyMatch(item -> item.getCriadoEm().isAfter(agora.minusSeconds(60)))) return;
        anteriores.forEach(item -> item.setUsadoEm(agora));
        emails.descartarRecuperacoes(usuario);
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RecuperacaoSenha recuperacao = new RecuperacaoSenha();
        recuperacao.setUsuario(usuario);
        recuperacao.setTokenHash(hash(token));
        recuperacao.setCriadoEm(agora);
        recuperacao.setExpiraEm(agora.plusSeconds(1800));
        recuperacoes.save(recuperacao);
        emails.agendarRecuperacao(usuario, lojaUrl + "#redefinir-senha=" + token, recuperacao.getExpiraEm());
    }

    @Transactional
    public void redefinir(String token, String novaSenha) {
        // Validate without logging rejected password or token values through Bean Validation.
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}") || novaSenha == null
                || novaSenha.isBlank() || novaSenha.length() < 8 || novaSenha.length() > 128) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        RecuperacaoSenha recuperacao = recuperacoes.findByTokenHash(hash(token))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
        Usuario usuario = usuarios.findByIdForUpdate(recuperacao.getUsuario().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
        // Refresh after taking the user lock: another request may already have consumed the token.
        entityManager.refresh(recuperacao);
        if (recuperacao.getUsadoEm() != null || !recuperacao.getExpiraEm().isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        usuario.setSenha(encoder.encode(novaSenha));
        usuario.setSenhaVersao(usuario.getSenhaVersao() + 1);
        recuperacoes.findByUsuarioIdAndUsadoEmIsNull(usuario.getId()).forEach(item -> item.setUsadoEm(Instant.now()));
        emails.descartarRecuperacoes(usuario);
    }

    static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
}
