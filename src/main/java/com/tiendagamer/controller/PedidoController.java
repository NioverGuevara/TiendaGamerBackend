package com.tiendagamer.controller;

import com.tiendagamer.dto.EstadoPedidoRequest;
import com.tiendagamer.dto.PedidoCreadoResponse;
import com.tiendagamer.dto.PedidoRequest;
import com.tiendagamer.dto.PedidoResponse;
import com.tiendagamer.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/pedidos")
@Tag(name = "Pedidos", description = "Compras: crear (autenticado), consultar (ADMIN y VENDEDOR), modificar (ADMIN)")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @Operation(summary = "Listar pedidos (ADMIN, VENDEDOR)")
    @GetMapping
    public List<PedidoResponse> listar() {
        return pedidoService.listar();
    }

    @Operation(summary = "Detalle de un pedido (ADMIN, VENDEDOR)")
    @GetMapping("/{id}")
    public PedidoResponse obtener(@PathVariable Long id) {
        return pedidoService.obtener(id);
    }

    @Operation(summary = "Registrar una compra (usuario autenticado)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoCreadoResponse crear(@Valid @RequestBody PedidoRequest req, Authentication autenticacion) {
        PedidoResponse pedido = pedidoService.crear(req, autenticacion.getName());
        return new PedidoCreadoResponse("Pedido registrado correctamente", pedido);
    }

    @Operation(summary = "Cambiar estado de un pedido (ADMIN)")
    @PatchMapping("/{id}/estado")
    public PedidoResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoPedidoRequest req) {
        return pedidoService.cambiarEstado(id, req.estado());
    }

    @Operation(summary = "Eliminar un pedido (ADMIN)")
    @DeleteMapping("/{id}")
    public Map<String, String> eliminar(@PathVariable Long id) {
        pedidoService.eliminar(id);
        return Map.of("mensaje", "Pedido eliminado correctamente");
    }
}
