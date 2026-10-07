package com.pablo.ecommerce.compra;

import java.math.BigDecimal;
import java.util.List;
import com.pablo.ecommerce.produto.Produto;
import com.pablo.ecommerce.produto.TipoProduto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FreteService {
    private final FaixaFreteRepository faixas;
    private final CarrinhoItemRepository itens;
    private final CarrinhoService carrinho;
    private final BigDecimal valorPadrao;

    public FreteService(FaixaFreteRepository faixas, CarrinhoItemRepository itens, CarrinhoService carrinho,
            @Value("${app.frete.valor-padrao}") BigDecimal valorPadrao) {
        this.faixas = faixas;
        this.itens = itens;
        this.carrinho = carrinho;
        if (valorPadrao.signum() < 0) throw new IllegalArgumentException("Frete padrao nao pode ser negativo");
        this.valorPadrao = valorPadrao;
    }

    @Transactional
    public FaixaFrete criar(FaixaFreteRequest request) {
        if (request.cepInicio().compareTo(request.cepFim()) > 0
                || request.pesoMinimoGramas() >= request.pesoMaximoGramas()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Faixa de CEP ou peso invalida");
        }
        boolean sobreposta = faixas.findAll().stream().anyMatch(faixa ->
                faixa.getCepInicio().compareTo(request.cepFim()) <= 0
                        && faixa.getCepFim().compareTo(request.cepInicio()) >= 0
                        && faixa.getPesoMinimoGramas() < request.pesoMaximoGramas()
                        && faixa.getPesoMaximoGramas() > request.pesoMinimoGramas());
        if (sobreposta) throw new ResponseStatusException(HttpStatus.CONFLICT, "Faixa de frete sobreposta");
        FaixaFrete faixa = new FaixaFrete();
        faixa.setCepInicio(request.cepInicio());
        faixa.setCepFim(request.cepFim());
        faixa.setPesoMinimoGramas(request.pesoMinimoGramas());
        faixa.setPesoMaximoGramas(request.pesoMaximoGramas());
        faixa.setValor(request.valor());
        return faixas.save(faixa);
    }

    @Transactional(readOnly = true)
    public List<FaixaFrete> listar() {
        return faixas.findAll();
    }

    @Transactional
    public void excluir(Long id) {
        if (!faixas.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Faixa nao encontrada");
        faixas.deleteById(id);
    }

    @Transactional(readOnly = true)
    public CotacaoFreteResponse cotarCarrinho(Long usuarioId, String cep) {
        carrinho.buscar(usuarioId);
        List<CarrinhoItem> linhas = itens.findByUsuarioIdOrderById(usuarioId);
        if (linhas.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Carrinho vazio");
        return cotar(linhas, cep);
    }

    public CotacaoFreteResponse cotar(List<CarrinhoItem> linhas, String cep) {
        BigDecimal subtotal = BigDecimal.ZERO;
        long peso = 0;
        for (CarrinhoItem linha : linhas) {
            Produto produto = linha.getProduto();
            carrinho.validarQuantidade(produto, linha.getQuantidade());
            subtotal = subtotal.add(produto.getPreco().multiply(BigDecimal.valueOf(linha.getQuantidade())));
            if (produto.getTipo() == TipoProduto.FISICO) {
                if (produto.getPesoGramas() == null || produto.getPesoGramas() <= 0) {
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                            "Produto fisico sem peso cadastrado: " + produto.getId());
                }
                peso += (long) produto.getPesoGramas() * linha.getQuantidade();
            }
        }
        BigDecimal frete = BigDecimal.ZERO;
        if (peso > 0) {
            long pesoFinal = peso;
            frete = faixas.findAll().stream()
                    .filter(faixa -> faixa.getCepInicio().compareTo(cep) <= 0
                            && faixa.getCepFim().compareTo(cep) >= 0
                            && pesoFinal > faixa.getPesoMinimoGramas()
                            && pesoFinal <= faixa.getPesoMaximoGramas())
                    .map(FaixaFrete::getValor).findFirst().orElse(valorPadrao);
        }
        return new CotacaoFreteResponse(cep, peso, subtotal, frete, subtotal.add(frete));
    }
}
