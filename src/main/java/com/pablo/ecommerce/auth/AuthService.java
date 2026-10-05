package com.pablo.ecommerce.auth;

import com.pablo.ecommerce.usuario.Usuario;
import com.pablo.ecommerce.usuario.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final String dummyHash;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.dummyHash = encoder.encode(java.util.UUID.randomUUID().toString());
    }

    public Usuario autenticar(LoginRequest request) {
        Usuario usuario = usuarios.findByEmail(request.email()).orElse(null);
        // Execute Argon2 even for unknown users to reduce timing differences.
        boolean senhaValida = encoder.matches(request.senha(), usuario == null ? dummyHash : usuario.getSenha());
        if (usuario == null || !senhaValida) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha invalidos");
        }
        return usuario;
    }
}
