package com.pablo.ecommerce.usuario;

import java.net.URI;
import java.util.List;
import com.pablo.ecommerce.compra.CarrinhoService;
import com.pablo.ecommerce.compra.EnderecoEntrega;
import com.pablo.ecommerce.compra.EnderecoRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/minha-conta")
public class MinhaContaController {
    private final UsuarioRepository usuarios;
    private final EnderecoSalvoRepository enderecos;
    private final CarrinhoService carrinho;

    public MinhaContaController(UsuarioRepository usuarios, EnderecoSalvoRepository enderecos, CarrinhoService carrinho) {
        this.usuarios = usuarios;
        this.enderecos = enderecos;
        this.carrinho = carrinho;
    }

    public record ContaResponse(Long id, String nome, String email) {}
    public record NomeRequest(@NotBlank @Size(max = 80) String nome) {}
    public record SalvarEnderecoRequest(@NotBlank @Size(max = 40) String apelido,
            @NotNull @Valid EnderecoRequest endereco) {}
    public record EnderecoResponse(Long id, String apelido, EnderecoEntrega endereco) {
        static EnderecoResponse from(EnderecoSalvo salvo) {
            return new EnderecoResponse(salvo.getId(), salvo.getApelido(), salvo.getEndereco());
        }
    }

    private Usuario usuario(Jwt jwt) {
        return usuarios.findById(carrinho.usuarioId(jwt))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ContaResponse buscar(@AuthenticationPrincipal Jwt jwt) {
        Usuario usuario = usuario(jwt);
        return new ContaResponse(usuario.getId(), usuario.getNome(), usuario.getEmail());
    }

    @PutMapping
    @Transactional
    public ContaResponse atualizar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody NomeRequest request) {
        Usuario usuario = carrinho.bloquearUsuario(carrinho.usuarioId(jwt));
        usuario.setNome(request.nome().trim());
        return new ContaResponse(usuario.getId(), usuario.getNome(), usuario.getEmail());
    }

    @GetMapping("/enderecos")
    @Transactional(readOnly = true)
    public List<EnderecoResponse> listar(@AuthenticationPrincipal Jwt jwt) {
        return enderecos.findByUsuarioIdOrderById(usuario(jwt).getId()).stream().map(EnderecoResponse::from).toList();
    }

    @PostMapping("/enderecos")
    @Transactional
    public ResponseEntity<EnderecoResponse> criar(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SalvarEnderecoRequest request) {
        Usuario usuario = carrinho.bloquearUsuario(carrinho.usuarioId(jwt));
        EnderecoSalvo salvo = new EnderecoSalvo();
        salvo.setUsuario(usuario);
        preencher(salvo, request);
        enderecos.save(salvo);
        return ResponseEntity.created(URI.create("/minha-conta/enderecos/" + salvo.getId())).body(EnderecoResponse.from(salvo));
    }

    @PutMapping("/enderecos/{id}")
    @Transactional
    public EnderecoResponse editar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody SalvarEnderecoRequest request) {
        Long usuarioId = carrinho.bloquearUsuario(carrinho.usuarioId(jwt)).getId();
        EnderecoSalvo salvo = proprio(id, usuarioId);
        preencher(salvo, request);
        return EnderecoResponse.from(salvo);
    }

    @DeleteMapping("/enderecos/{id}")
    @Transactional
    public ResponseEntity<Void> excluir(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        Long usuarioId = carrinho.bloquearUsuario(carrinho.usuarioId(jwt)).getId();
        enderecos.delete(proprio(id, usuarioId));
        return ResponseEntity.noContent().build();
    }

    private EnderecoSalvo proprio(Long id, Long usuarioId) {
        return enderecos.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void preencher(EnderecoSalvo salvo, SalvarEnderecoRequest request) {
        salvo.setApelido(request.apelido().trim());
        salvo.setEndereco(request.endereco().toEntity());
    }
}
