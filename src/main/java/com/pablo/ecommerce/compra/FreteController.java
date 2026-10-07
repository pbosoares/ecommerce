package com.pablo.ecommerce.compra;

import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/frete")
public class FreteController {
    private final FreteService frete;
    private final CarrinhoService carrinho;

    public FreteController(FreteService frete, CarrinhoService carrinho) {
        this.frete = frete;
        this.carrinho = carrinho;
    }

    @PostMapping("/cotacoes")
    public CotacaoFreteResponse cotar(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CotacaoFreteRequest request) {
        return frete.cotarCarrinho(carrinho.usuarioId(jwt), request.cep());
    }

    @PostMapping("/faixas")
    public ResponseEntity<FaixaFrete> criar(@Valid @RequestBody FaixaFreteRequest request) {
        FaixaFrete faixa = frete.criar(request);
        return ResponseEntity.created(URI.create("/frete/faixas/" + faixa.getId())).body(faixa);
    }

    @GetMapping("/faixas")
    public List<FaixaFrete> listar() {
        return frete.listar();
    }

    @DeleteMapping("/faixas/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        frete.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
