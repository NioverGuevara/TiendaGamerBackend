package com.tiendagamer.controller;

import com.tiendagamer.dto.ProductoRequest;
import com.tiendagamer.dto.ProductoResponse;
import com.tiendagamer.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/productos")
@Tag(name = "Productos", description = "Catálogo: lectura pública, escritura solo ADMIN")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @Operation(summary = "Listar productos (público)")
    @SecurityRequirements
    @GetMapping
    public List<ProductoResponse> listar() {
        return productoService.listar();
    }

    @Operation(summary = "Detalle de un producto (público)")
    @SecurityRequirements
    @GetMapping("/{id}")
    public ProductoResponse obtener(@PathVariable Long id) {
        return productoService.obtener(id);
    }

    @Operation(summary = "Crear producto (ADMIN)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoResponse crear(@Valid @RequestBody ProductoRequest req) {
        return productoService.crear(req);
    }

    @Operation(summary = "Actualizar producto (ADMIN)")
    @PutMapping("/{id}")
    public ProductoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest req) {
        return productoService.actualizar(id, req);
    }

    @Operation(summary = "Eliminar producto (ADMIN)")
    @DeleteMapping("/{id}")
    public Map<String, String> eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
        return Map.of("mensaje", "Producto eliminado correctamente");
    }
}
