package com.pablo.ecommerce.auth;

import com.pablo.ecommerce.usuario.UsuarioRepository;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.Jwt;

public class VersaoSenhaValidator implements OAuth2TokenValidator<Jwt> {
    private final UsuarioRepository usuarios;
    public VersaoSenhaValidator(UsuarioRepository usuarios) { this.usuarios = usuarios; }
    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        try {
            Object versao = jwt.getClaim("senhaVersao");
            long valor = versao == null ? 0 : Long.parseLong(versao.toString());
            var usuario = usuarios.findById(Long.valueOf(jwt.getSubject())).orElse(null);
            if (usuario != null && usuario.getSenhaVersao() == valor) return OAuth2TokenValidatorResult.success();
        } catch (IllegalArgumentException exception) { /* Invalid subject or version. */ }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Sessao invalida", null));
    }
}
