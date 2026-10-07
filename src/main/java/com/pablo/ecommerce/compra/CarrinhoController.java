package com.pablo.ecommerce.compra;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/carrinho")
public class CarrinhoController {
    private final CarrinhoService service;

    public CarrinhoController(CarrinhoService service) {
        this.service = service;
    }

    @GetMapping
    public CarrinhoResponse buscar(@AuthenticationPrincipal Jwt jwt) {
        return service.buscar(service.usuarioId(jwt));
    }

    @PostMapping("/itens")
    public CarrinhoResponse adicionar(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ItemCarrinhoRequest request) {
        return service.adicionar(service.usuarioId(jwt), request);
    }

    @PutMapping("/itens/{produtoId}")
    public CarrinhoResponse alterar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long produtoId,
            @Valid @RequestBody QuantidadeRequest request) {
        return service.alterar(service.usuarioId(jwt), produtoId, request);
    }

    @DeleteMapping("/itens/{produtoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@AuthenticationPrincipal Jwt jwt, @PathVariable Long produtoId) {
        service.remover(service.usuarioId(jwt), produtoId);
    }
}
