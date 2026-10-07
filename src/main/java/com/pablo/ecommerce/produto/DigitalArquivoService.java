package com.pablo.ecommerce.produto;

import com.pablo.ecommerce.compra.PedidoRepository;
import com.pablo.ecommerce.compra.StatusPedido;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DigitalArquivoService {
    private final DigitalArquivoRepository arquivos;
    private final ProdutoRepository produtos;
    private final PedidoRepository pedidos;
    private final String storagePath;

    public DigitalArquivoService(DigitalArquivoRepository arquivos, ProdutoRepository produtos,
            PedidoRepository pedidos, @Value("${app.digital.storage-path:}") String storagePath) {
        this.arquivos = arquivos;
        this.produtos = produtos;
        this.pedidos = pedidos;
        this.storagePath = storagePath;
    }

    @Transactional
    public void cadastrar(Long produtoId, MultipartFile upload) {
        Produto produto = produtos.findById(produtoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto nao encontrado"));
        if (produto.getTipo() != TipoProduto.DIGITAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo apenas para produto digital");
        }
        if (upload.isEmpty() || upload.getSize() > 20L * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo deve ter entre 1 byte e 20 MB");
        }
        String nome = upload.getOriginalFilename() == null ? "produto-digital" : upload.getOriginalFilename();
        nome = nome.replace('\\', '/');
        nome = nome.substring(nome.lastIndexOf('/') + 1).replaceAll("[\\r\\n\\x00-\\x1f]", "");
        if (nome.isBlank() || nome.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome de arquivo invalido");
        }
        Path destino = base().resolve(UUID.randomUUID().toString()).normalize();
        try {
            Files.createDirectories(base());
            try (var entrada = upload.getInputStream()) {
                Files.copy(entrada, destino);
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Nao foi possivel armazenar arquivo", exception);
        }
        DigitalArquivo arquivo = arquivos.findByProdutoId(produtoId).orElseGet(DigitalArquivo::new);
        arquivo.setProdutoId(produtoId);
        arquivo.setChave(destino.getFileName().toString());
        arquivo.setNome(nome);
        arquivo.setEnviadoEm(Instant.now());
        arquivos.save(arquivo);
    }

    @Transactional(readOnly = true)
    public Download autorizado(Long usuarioId, Long pedidoId, Long itemId) {
        var pedido = pedidos.findByIdAndUsuarioId(pedidoId, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido nao encontrado"));
        if (pedido.getStatus() != StatusPedido.PAGO && pedido.getStatus() != StatusPedido.EM_PREPARACAO
                && pedido.getStatus() != StatusPedido.ENVIADO && pedido.getStatus() != StatusPedido.ENTREGUE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Download disponivel apos pagamento");
        }
        var item = pedido.getItens().stream().filter(i -> i.getId().equals(itemId)
                && i.getTipo() == TipoProduto.DIGITAL).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item digital nao encontrado"));
        if (item.getArquivoKey() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo indisponivel");
        }
        Path caminho = base().resolve(item.getArquivoKey()).normalize();
        if (!caminho.startsWith(base()) || !Files.isRegularFile(caminho, LinkOption.NOFOLLOW_LINKS)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo indisponivel");
        }
        return new Download(caminho, item.getArquivoNome());
    }

    private Path base() {
        if (storagePath.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Armazenamento digital nao configurado");
        }
        return Path.of(storagePath).toAbsolutePath().normalize();
    }

    public record Download(Path caminho, String nome) {}
}
