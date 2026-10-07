package com.pablo.ecommerce.compra;

import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {
    private final PedidoService pedidos;
    private final CarrinhoService carrinho;

    public PedidoController(PedidoService pedidos, CarrinhoService carrinho) {
        this.pedidos = pedidos;
        this.carrinho = carrinho;
    }

    @PostMapping
    public ResponseEntity<PedidoResponse> criar(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody(required = false) CriarPedidoRequest request) {
        PedidoResponse pedido = pedidos.criar(carrinho.usuarioId(jwt), request);
        return ResponseEntity.created(URI.create("/pedidos/" + pedido.id())).body(pedido);
    }

    @GetMapping
    public List<PedidoResponse> listar(@AuthenticationPrincipal Jwt jwt) {
        return pedidos.listar(carrinho.usuarioId(jwt));
    }

    @GetMapping("/{id}")
    public PedidoResponse buscar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return pedidos.buscar(carrinho.usuarioId(jwt), id);
    }
}
