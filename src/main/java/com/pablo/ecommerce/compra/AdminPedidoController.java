package com.pablo.ecommerce.compra;

import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminPedidoController {
    private final PedidoService pedidos;

    public AdminPedidoController(PedidoService pedidos) {
        this.pedidos = pedidos;
    }

    @GetMapping("/admin/pedidos")
    public Page<PedidoResponse> listar(@RequestParam(defaultValue = "0") @Min(0) int pagina) {
        return pedidos.listarAdmin(pagina);
    }
}
