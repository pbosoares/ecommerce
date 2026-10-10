package com.pablo.ecommerce.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class RecuperacaoSenhaController {
    private final RecuperacaoSenhaService recuperacao;
    public RecuperacaoSenhaController(RecuperacaoSenhaService recuperacao) { this.recuperacao = recuperacao; }
    public record SolicitarRequest(@NotBlank @Email @Size(max = 254) String email) {}
    public record RedefinirRequest(String token, String novaSenha) {}
    public record Mensagem(String mensagem) {}

    @PostMapping("/recuperar-senha")
    public ResponseEntity<Mensagem> solicitar(@Valid @RequestBody SolicitarRequest request) {
        recuperacao.solicitar(request.email());
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(new Mensagem(
                "Se houver uma conta com esse e-mail, enviaremos um link para redefinir sua senha."));
    }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<Void> redefinir(@RequestBody RedefinirRequest request) {
        recuperacao.redefinir(request.token(), request.novaSenha());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
