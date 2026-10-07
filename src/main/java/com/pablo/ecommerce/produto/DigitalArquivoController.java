package com.pablo.ecommerce.produto;

import com.pablo.ecommerce.compra.CarrinhoService;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class DigitalArquivoController {
    private final DigitalArquivoService arquivos;
    private final CarrinhoService carrinho;

    public DigitalArquivoController(DigitalArquivoService arquivos, CarrinhoService carrinho) {
        this.arquivos = arquivos;
        this.carrinho = carrinho;
    }

    @PostMapping("/produtos/{id}/arquivo")
    public ResponseEntity<Void> cadastrar(@PathVariable Long id, @RequestParam("arquivo") MultipartFile upload) {
        arquivos.cadastrar(id, upload);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pedidos/{pedidoId}/itens/{itemId}/download")
    public ResponseEntity<FileSystemResource> download(@AuthenticationPrincipal Jwt jwt,
            @PathVariable Long pedidoId, @PathVariable Long itemId) {
        var arquivo = arquivos.autorizado(carrinho.usuarioId(jwt), pedidoId, itemId);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(arquivo.nome(), StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM).body(new FileSystemResource(arquivo.caminho()));
    }
}
